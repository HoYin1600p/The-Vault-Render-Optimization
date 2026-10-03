package dev.hoyin1600p.vault_render_optimization.client.entitygpu.citadel;

import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.TabulaModelRenderUtils;
import com.mojang.math.Vector3f;
import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.ModelMesh;
import java.lang.reflect.Field;

/**
 * Reads one Citadel {@code AdvancedModelBox}'s own cubes exactly as its private {@code doRender} consumes them:
 * per quad, its vertices in order with {@code position / 16}, {@code textureU}/{@code textureV} and the quad's
 * normal. {@code doRender} is vanilla {@code ModelPart.compile}'s arithmetic (same division, the same
 * {@code Vector4f}/{@code Vector3f} transforms, no normalization), so the existing shader applies unchanged.
 *
 * <p>Citadel's quad and vertex classes are package-private, so their fields are read reflectively; this runs
 * once per part (the mesh is cached), never per frame.
 */
public final class CitadelMeshCapture {
    private static final String QUAD = "com.github.alexthe666.citadel.client.model.TabulaModelRenderUtils$TexturedQuad";
    private static final String VERTEX = "com.github.alexthe666.citadel.client.model.TabulaModelRenderUtils$PositionTextureVertex";
    private static final Field QUAD_NORMAL;
    private static final Field QUAD_VERTICES;
    private static final Field VERTEX_POSITION;
    private static final Field VERTEX_U;
    private static final Field VERTEX_V;

    static {
        Field normal = null, vertices = null, position = null, u = null, v = null;
        try {
            ClassLoader loader = TabulaModelRenderUtils.class.getClassLoader();
            Class<?> quad = Class.forName(QUAD, false, loader);
            Class<?> vertex = Class.forName(VERTEX, false, loader);
            normal = open(quad, "normal");
            vertices = open(quad, "vertexPositions");
            position = open(vertex, "position");
            u = open(vertex, "textureU");
            v = open(vertex, "textureV");
        } catch (ReflectiveOperationException | RuntimeException failure) {
            VaultRenderOptimization.LOGGER.warn("GPU entity models: Citadel's quad layout is not the expected one ({}); "
                    + "Citadel models stay on the CPU", failure.toString());
            normal = vertices = position = u = v = null;
        }
        QUAD_NORMAL = normal;
        QUAD_VERTICES = vertices;
        VERTEX_POSITION = position;
        VERTEX_U = u;
        VERTEX_V = v;
    }

    private CitadelMeshCapture() {
    }

    private static Field open(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    /** @return the mesh, or null when the layout is unexpected or a quad is not four vertices (CPU path) */
    public static ModelMesh capture(AdvancedModelBox box) {
        if (QUAD_NORMAL == null || box.cubeList == null) return null;
        try {
            int vertices = 0;
            for (TabulaModelRenderUtils.ModelBox cube : box.cubeList) {
                if (cube == null || cube.quads == null) return null;
                for (Object quad : cube.quads) {
                    if (quad == null || QUAD_NORMAL.get(quad) == null) return null;
                    Object[] corners = (Object[]) QUAD_VERTICES.get(quad);
                    if (corners == null || corners.length != 4) return null;
                    for (Object corner : corners) {
                        if (corner == null || VERTEX_POSITION.get(corner) == null) return null;
                    }
                    vertices += 4;
                }
            }
            if (vertices == 0) return ModelMesh.EMPTY;
            float[] data = new float[vertices * ModelMesh.FLOATS_PER_VERTEX];
            int i = 0;
            for (TabulaModelRenderUtils.ModelBox cube : box.cubeList) {
                for (Object quad : cube.quads) {
                    Vector3f normal = (Vector3f) QUAD_NORMAL.get(quad);
                    float nx = normal.x(), ny = normal.y(), nz = normal.z();
                    for (Object corner : (Object[]) QUAD_VERTICES.get(quad)) {
                        Vector3f position = (Vector3f) VERTEX_POSITION.get(corner);
                        // doRender divides by 16 per frame; the quotient is the same float every time.
                        data[i++] = position.x() / 16.0F;
                        data[i++] = position.y() / 16.0F;
                        data[i++] = position.z() / 16.0F;
                        data[i++] = VERTEX_U.getFloat(corner);
                        data[i++] = VERTEX_V.getFloat(corner);
                        data[i++] = nx;
                        data[i++] = ny;
                        data[i++] = nz;
                    }
                }
            }
            return new ModelMesh(data, vertices);
        } catch (IllegalAccessException | ClassCastException failure) {
            return null;
        }
    }
}
