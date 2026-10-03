package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import net.minecraft.util.Mth;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.system.MemoryUtil;

/**
 * Particle batches through the real upload path: holes reserved at byte offsets (adjacent or with CPU-written
 * particles between them), uploaded with {@link GpuEntityModels#uploadGaps}, expanded by
 * {@code dispatchParticles}; the whole buffer must equal the client bytes with the holes filled on the CPU.
 */
final class GpuModelsParticleGap {
    private GpuModelsParticleGap() {
    }

    static String roundTrip(GpuEntityBackend backend, long seed) {
        Random random = new Random(seed);
        HoleBatch batch = new HoleBatch();
        int stride = ParticleRecord.VERTEX_WORDS * 4;
        float[] camera = GpuParticleSelfTest.build(seed, 1).camera();
        int cursor = stride * 4;
        for (int i = 0; i < 2000; i++) {
            boolean rolled = random.nextBoolean();
            float angle = random.nextFloat() * 6;
            if (!batch.addParticle(random.nextFloat() * 60 - 30, random.nextFloat() * 60 - 30, random.nextFloat() * 60 - 30,
                    random.nextFloat() * 0.5F, rolled, rolled ? Mth.sin(angle) : 0.0F, rolled ? Mth.cos(angle) : 1.0F,
                    random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextInt(),
                    random.nextInt(0x00F000F1), cursor, camera[0], camera[1], camera[2], camera[3], camera[4], camera[5])) {
                return "batch refused a particle";
            }
            cursor += 4 * stride + (random.nextBoolean() ? 0 : 4 * stride * (1 + random.nextInt(3)));
        }
        int bytes = cursor + stride * 4;
        batch.setBase(0);
        ByteBuffer client = MemoryUtil.memAlloc(bytes).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer expected = MemoryUtil.memAlloc(bytes).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer gpu = MemoryUtil.memAlloc(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int vbo = GL15C.glGenBuffers();
        try {
            for (int i = 0; i < bytes; i += 4) client.putInt(i, random.nextInt());
            expected.put(client.duplicate()).clear();
            batch.fillOnCpu(expected);
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
            if (!GpuEntityModels.uploadGaps(GL15C.GL_ARRAY_BUFFER, client, GL15C.GL_STREAM_DRAW, batch)) {
                return "uploadGaps refused ascending particle holes";
            }
            backend.dispatchParticles(batch, vbo, true);
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
            GL15C.glGetBufferSubData(GL15C.GL_ARRAY_BUFFER, 0L, gpu);
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
            for (int i = 0; i < bytes; i += 4) {
                if (expected.getInt(i) != gpu.getInt(i)) {
                    return "byte " + i + ": expected 0x" + Integer.toHexString(expected.getInt(i)) + " GPU 0x"
                            + Integer.toHexString(gpu.getInt(i));
                }
            }
            return null;
        } finally {
            GL15C.glDeleteBuffers(vbo);
            MemoryUtil.memFree(client);
            MemoryUtil.memFree(expected);
            MemoryUtil.memFree(gpu);
        }
    }
}
