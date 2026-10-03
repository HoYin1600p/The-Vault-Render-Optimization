package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import java.nio.IntBuffer;
import java.util.Random;
import net.minecraft.util.Mth;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.system.MemoryUtil;

/**
 * Startup and on-demand check of {@code particle_expand.comp}: random particle records (rolled and not, varied
 * sizes, positions, UVs, colours and lights) expanded on the GPU must equal {@link ParticleReference} word for
 * word, and the words between their outputs must stay untouched.
 */
final class GpuParticleSelfTest {
    private static final int GAP_WORDS = 3;
    private static final int SENTINEL = 0x5EED5EED;

    private GpuParticleSelfTest() {
    }

    record Case(int[] records, int count, float[] camera, int outputWords, int[] expected) {
    }

    static Case build(long seed, int particles) {
        Random random = new Random(seed);
        float[] camera = new float[6];
        // A plausible camera basis from yaw/pitch, as Camera.setRotation computes it through a quaternion.
        float yaw = random.nextFloat() * 360.0F, pitch = random.nextFloat() * 180.0F - 90.0F;
        com.mojang.math.Quaternion rotation = com.mojang.math.Vector3f.YP.rotationDegrees(-yaw);
        rotation.mul(com.mojang.math.Vector3f.XP.rotationDegrees(pitch));
        com.mojang.math.Vector3f left = new com.mojang.math.Vector3f(1.0F, 0.0F, 0.0F);
        left.transform(rotation);
        com.mojang.math.Vector3f up = new com.mojang.math.Vector3f(0.0F, 1.0F, 0.0F);
        up.transform(rotation);
        camera[0] = left.x(); camera[1] = left.y(); camera[2] = left.z();
        camera[3] = up.x(); camera[4] = up.y(); camera[5] = up.z();
        int[] records = new int[particles * ParticleRecord.WORDS];
        int words = ParticleRecord.VERTICES * ParticleRecord.VERTEX_WORDS;
        int out = GAP_WORDS;
        int[] offsets = new int[particles];
        for (int i = 0; i < particles; i++) {
            boolean rolled = random.nextInt(3) == 0;
            float angle = random.nextFloat() * 12.0F - 6.0F;
            float size = random.nextInt(8) == 0 ? random.nextFloat() * 8.0F : random.nextFloat() * 0.5F;
            ParticleRecord.write(records, i * ParticleRecord.WORDS,
                    random.nextFloat() * 128.0F - 64.0F, random.nextFloat() * 128.0F - 64.0F, random.nextFloat() * 128.0F - 64.0F,
                    size, rolled, rolled ? Mth.sin(angle) : 0.0F, rolled ? Mth.cos(angle) : 1.0F,
                    random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat(),
                    random.nextInt(), random.nextInt(0x00F000F1), out);
            offsets[i] = out;
            out += words + GAP_WORDS;
        }
        int[] expected = new int[out];
        java.util.Arrays.fill(expected, SENTINEL);
        int[] vertices = new int[words];
        for (int i = 0; i < particles; i++) {
            ParticleReference.expand(records, i * ParticleRecord.WORDS, camera, vertices, 0);
            System.arraycopy(vertices, 0, expected, offsets[i], words);
        }
        return new Case(records, particles, camera, out, expected);
    }

    /** @return null when the GPU output is bit-identical, otherwise the first difference */
    static String run(GpuEntityBackend backend, long seed, int particles) {
        if (!backend.particlesAvailable()) return "particle program unavailable: " + backend.particleFailure();
        Case test = build(seed, particles);
        int outputBuffer = GL15C.glGenBuffers();
        IntBuffer sentinel = null;
        IntBuffer readback = null;
        try {
            sentinel = MemoryUtil.memAllocInt(test.outputWords());
            for (int i = 0; i < test.outputWords(); i++) sentinel.put(i, SENTINEL);
            GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, outputBuffer);
            GL15C.glBufferData(GL43C.GL_SHADER_STORAGE_BUFFER, sentinel, GL15C.GL_STATIC_DRAW);
            GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, 0);
            // A small group limit exercises the multi-dispatch path.
            backend.runParticles(backend.stageWords(test.records(), test.count() * ParticleRecord.WORDS), test.count(),
                    test.camera(), outputBuffer, 3, true);
            GL20C.glUseProgram(0);
            readback = MemoryUtil.memAllocInt(test.outputWords());
            GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, outputBuffer);
            GL15C.glGetBufferSubData(GL43C.GL_SHADER_STORAGE_BUFFER, 0L, readback);
            GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, 0);
            int error = GL11C.glGetError();
            if (error != GL11C.GL_NO_ERROR) return "GL error 0x" + Integer.toHexString(error) + " during the particle self-test";
            int[] expected = test.expected();
            for (int i = 0; i < expected.length; i++) {
                if (readback.get(i) != expected[i]) {
                    return "particle word " + i + " expected 0x" + Integer.toHexString(expected[i]) + " but the GPU wrote 0x"
                            + Integer.toHexString(readback.get(i));
                }
            }
            return null;
        } finally {
            GL15C.glDeleteBuffers(outputBuffer);
            if (sentinel != null) MemoryUtil.memFree(sentinel);
            if (readback != null) MemoryUtil.memFree(readback);
        }
    }
}
