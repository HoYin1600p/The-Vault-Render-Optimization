package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GLCapabilities;

/**
 * What the GPU entity path needs from the driver, read once on the render thread without creating
 * or replacing the context's capabilities object.
 *
 * <p>Minecraft asks for a 3.2 core context, and some drivers (NVIDIA included) then report exactly
 * 3.2 with GLSL 1.50. Compute shaders are still reachable there through the ARB extensions those
 * drivers expose, so there are two routes: core OpenGL 4.3 with GLSL 4.30, or GLSL 1.50 plus
 * ARB_compute_shader, ARB_shader_storage_buffer_object, ARB_program_interface_query,
 * ARB_shader_image_load_store (memory barriers) and ARB_gpu_shader5 ({@code precise}, float bit
 * casts). Anything else fails closed.
 */
public final class GpuEntityCapabilities {
    /** Largest mesh arena the path ever allocates. */
    public static final long ARENA_MAX_BYTES = 64L << 20;
    static final int MIN_CORE_GLSL = 430;
    static final int MIN_EXTENSION_GLSL = 150;
    static final List<String> RENDERER_DENYLIST = List.of(
            "llvmpipe", "softpipe", "swrast", "gdi generic", "microsoft basic render", "svga3d",
            "gl4es", "mobileglues", "ltw", "apple"
    );

    private GpuEntityCapabilities() {
    }

    public enum Route {
        CORE_43("#version 430 core\n"),
        ARB_EXTENSIONS("#version 150 core\n"
                + "#extension GL_ARB_compute_shader : require\n"
                + "#extension GL_ARB_shader_storage_buffer_object : require\n"
                + "#extension GL_ARB_gpu_shader5 : require\n");

        private final String header;

        Route(String header) {
            this.header = header;
        }

        public String header() {
            return header;
        }
    }

    public record Snapshot(boolean openGl43, boolean arbExtensions, boolean functions, String version,
                           String renderer, String vendor, String glslVersion, int maxComputeStorageBlocks,
                           int maxStorageBindings, long maxStorageBlockSize, int maxWorkGroupCountX,
                           int maxWorkGroupSizeX) {
    }

    public static Snapshot read() {
        GLCapabilities caps = GL.getCapabilities();
        boolean arb = caps.GL_ARB_compute_shader && caps.GL_ARB_shader_storage_buffer_object
                && caps.GL_ARB_program_interface_query && caps.GL_ARB_shader_image_load_store
                && caps.GL_ARB_gpu_shader5;
        boolean functions = caps.glDispatchCompute != 0L && caps.glMemoryBarrier != 0L
                && caps.glShaderStorageBlockBinding != 0L && caps.glGetProgramResourceIndex != 0L
                && caps.glBindBufferBase != 0L && caps.glGetBufferSubData != 0L && caps.glUniform1ui != 0L
                && caps.glCopyBufferSubData != 0L && caps.glGetIntegeri_v != 0L;
        boolean usable = functions && (caps.OpenGL43 || arb);
        return new Snapshot(
                caps.OpenGL43,
                arb,
                functions,
                String.valueOf(GL11C.glGetString(GL11C.GL_VERSION)),
                String.valueOf(GL11C.glGetString(GL11C.GL_RENDERER)),
                String.valueOf(GL11C.glGetString(GL11C.GL_VENDOR)),
                String.valueOf(GL11C.glGetString(GL20C.GL_SHADING_LANGUAGE_VERSION)),
                usable ? GL11C.glGetInteger(GL43C.GL_MAX_COMPUTE_SHADER_STORAGE_BLOCKS) : 0,
                usable ? GL11C.glGetInteger(GL43C.GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS) : 0,
                usable ? Integer.toUnsignedLong(GL11C.glGetInteger(GL43C.GL_MAX_SHADER_STORAGE_BLOCK_SIZE)) : 0L,
                usable ? GL30C.glGetIntegeri(GL43C.GL_MAX_COMPUTE_WORK_GROUP_COUNT, 0) : 0,
                usable ? GL30C.glGetIntegeri(GL43C.GL_MAX_COMPUTE_WORK_GROUP_SIZE, 0) : 0
        );
    }

    /** The shader route to use, or null when neither is available. */
    public static Route route(Snapshot snapshot) {
        if (!snapshot.functions()) return null;
        int glsl = glslVersion(snapshot.glslVersion());
        if (snapshot.openGl43() && glsl >= MIN_CORE_GLSL) return Route.CORE_43;
        if (snapshot.arbExtensions() && glsl >= MIN_EXTENSION_GLSL) return Route.ARB_EXTENSIONS;
        return null;
    }

    /** @return null when the snapshot supports the path, otherwise why it is blocked */
    public static String blocker(Snapshot snapshot) {
        if (route(snapshot) == null) {
            return "compute shaders are not available: needs OpenGL 4.3 or the ARB compute/storage-buffer/"
                    + "gpu_shader5 extensions (driver reports " + snapshot.version() + ", GLSL "
                    + snapshot.glslVersion() + ")";
        }
        String renderer = snapshot.renderer().toLowerCase(Locale.ROOT);
        for (String denied : RENDERER_DENYLIST) {
            if (renderer.contains(denied)) return "renderer '" + snapshot.renderer() + "' is not supported";
        }
        if (snapshot.maxComputeStorageBlocks() < 3 || snapshot.maxStorageBindings() < 3) {
            return "too few shader storage buffer bindings";
        }
        if (snapshot.maxStorageBlockSize() < (16L << 20)) return "shader storage blocks are smaller than 16 MiB";
        if (snapshot.maxWorkGroupCountX() < 65535 || snapshot.maxWorkGroupSizeX() < 64) {
            return "compute work group limits are below the OpenGL 4.3 minimum";
        }
        return null;
    }

    private static final Pattern VERSION = Pattern.compile("^\\s*(\\d+)\\.(\\d+)");

    /** "4.60 NVIDIA" -> 460, "1.50 NVIDIA via Cg compiler" -> 150; unparsable -> 0. */
    static int glslVersion(String text) {
        if (text == null) return 0;
        Matcher matcher = VERSION.matcher(text);
        if (!matcher.find()) return 0;
        int major = Integer.parseInt(matcher.group(1));
        String minorText = matcher.group(2);
        int minor = Integer.parseInt(minorText.length() == 1 ? minorText + "0" : minorText.substring(0, 2));
        return major * 100 + minor;
    }
}
