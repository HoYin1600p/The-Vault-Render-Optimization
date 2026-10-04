package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import dev.hoyin1600p.vault_render_optimization.util.CommandText;
import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.system.MemoryUtil;
import static dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuEntityModels.*;

/** Live verifier: GPU-written vertices compared with the exact CPU fill. */
final class GpuVerifier {
    private GpuVerifier() {
    }

    static volatile boolean verify;
    static String firstMismatch;

    /**
     * Diagnostic: read back every dispatch's vertex buffer and compare the GPU-written vertices word
     * for word with {@link HoleBatch#fillOnCpu}'s exact vanilla bytes for the same parts. Stalls the
     * pipeline once per dispatch, so it is for testing only.
     */
    public static void setVerify(boolean enabled) {
        verify = enabled;
    }

    public static String verifyStatus() {
        return "GPU entity verify " + CommandText.onOff(verify) + ": vertices checked " + VERIFIED_VERTICES.get()
                + ", mismatching vertices " + VERIFY_MISMATCHES.get()
                + (firstMismatch == null ? "" : ", first: " + firstMismatch);
    }

    static void verify(HoleBatch batch, ByteBuffer client, VertexFormat format) {
        int bytes = client.remaining();
        ByteBuffer gpu = null;
        ByteBuffer cpu = null;
        try {
            gpu = MemoryUtil.memAlloc(bytes);
            cpu = MemoryUtil.memAlloc(bytes);
            // BufferUploader still has the format's vertex buffer bound to GL_ARRAY_BUFFER.
            GL15C.glGetBufferSubData(GL15C.GL_ARRAY_BUFFER, 0L, gpu);
            // Expected: vanilla's client bytes with the reserved vertices filled exactly on the CPU.
            cpu.put(client.duplicate()).clear();
            batch.fillOnCpu(cpu);
            int stride = format.getVertexSize();
            // Every vertex of the upload, reserved or CPU-written, must match.
            for (int at = 0; at + stride <= bytes; at += stride) {
                boolean same = true;
                for (int w = 0; w < stride; w += 4) {
                    if (gpu.getInt(at + w) != cpu.getInt(at + w)) {
                        same = false;
                        if (firstMismatch == null) {
                            firstMismatch = "byte " + (at + w) + " GPU 0x" + Integer.toHexString(gpu.getInt(at + w))
                                    + " CPU 0x" + Integer.toHexString(cpu.getInt(at + w));
                            VaultRenderOptimization.LOGGER.warn("GPU entity verify mismatch: {}", firstMismatch);
                        }
                        break;
                    }
                }
                VERIFIED_VERTICES.incrementAndGet();
                if (!same) VERIFY_MISMATCHES.incrementAndGet();
            }
        } finally {
            MemoryUtil.memFree(gpu);
            MemoryUtil.memFree(cpu);
        }
    }
}
