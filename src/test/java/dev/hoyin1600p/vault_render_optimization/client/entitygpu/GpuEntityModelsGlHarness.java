package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.system.MemoryUtil;

/** GL-side checks shared by the GPU tests; needs a current context. */
final class GpuEntityModelsGlHarness {
    private GpuEntityModelsGlHarness() {
    }

    /**
     * Uploads meshes through the arena, records holes in a {@link HoleBatch} at a non-zero base as a
     * builder would, dispatches into a vertex buffer and compares it with {@link HoleBatch#fillOnCpu}.
     */
    static String arenaRoundTrip(GpuEntityBackend backend, long seed) {
        GpuEntitySelfTest.Case source = GpuEntitySelfTest.build(seed, 400);
        HoleBatch batch = new HoleBatch();
        int base = 36 * 13;
        int cursor = base + 36 * 3;
        float[] pose = new float[16];
        float[] normal = new float[9];
        for (int i = 0; i < source.count(); i++) {
            ModelMesh mesh = source.meshes().get(i);
            int first = backend.upload(mesh);
            if (first < 0) return "arena refused a mesh";
            int r = i * InstanceRecord.WORDS;
            for (int k = 0; k < 16; k++) pose[k] = Float.intBitsToFloat(source.records()[r + k]);
            for (int k = 0; k < 9; k++) normal[k] = Float.intBitsToFloat(source.records()[r + 16 + k]);
            batch.add(mesh, first, pose, normal, cursor, source.records()[r + InstanceRecord.COLOR],
                    source.records()[r + InstanceRecord.OVERLAY], source.records()[r + InstanceRecord.LIGHT]);
            cursor += mesh.vertexCount() * 36 + 36 * (i % 3);
        }
        batch.base = base;
        int bytes = cursor - base;

        ByteBuffer cpu = MemoryUtil.memAlloc(bytes).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer gpu = MemoryUtil.memAlloc(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int vbo = GL15C.glGenBuffers();
        try {
            for (int i = 0; i < bytes; i += 4) cpu.putInt(i, 0x5A5A5A5A);
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
            GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, cpu, GL15C.GL_STREAM_DRAW);
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
            batch.fillOnCpu(cpu);

            backend.dispatch(batch, vbo, true);
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
            GL15C.glGetBufferSubData(GL15C.GL_ARRAY_BUFFER, 0L, gpu);
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
            for (int i = 0; i < bytes; i += 4) {
                if (cpu.getInt(i) != gpu.getInt(i)) {
                    return "byte " + i + ": CPU 0x" + Integer.toHexString(cpu.getInt(i)) + " GPU 0x"
                            + Integer.toHexString(gpu.getInt(i));
                }
            }
            return null;
        } finally {
            GL15C.glDeleteBuffers(vbo);
            MemoryUtil.memFree(cpu);
            MemoryUtil.memFree(gpu);
            backend.resetArena();
        }
    }

    /**
     * The gap-only upload: a client buffer whose reserved ranges hold garbage is uploaded with
     * {@link GpuEntityModels#uploadGaps}, the GPU fills the reserved ranges, and the whole vertex
     * buffer must equal the client bytes with the reserved ranges filled exactly on the CPU.
     */
    static String gapUploadRoundTrip(GpuEntityBackend backend, long seed) {
        GpuEntitySelfTest.Case source = GpuEntitySelfTest.build(seed, 300);
        HoleBatch batch = new HoleBatch();
        java.util.Random random = new java.util.Random(seed);
        int cursor = 36 * 2;
        float[] pose = new float[16];
        float[] normal = new float[9];
        for (int i = 0; i < source.count(); i++) {
            ModelMesh mesh = source.meshes().get(i);
            int first = backend.upload(mesh);
            if (first < 0) return "arena refused a mesh";
            int r = i * InstanceRecord.WORDS;
            for (int k = 0; k < 16; k++) pose[k] = Float.intBitsToFloat(source.records()[r + k]);
            for (int k = 0; k < 9; k++) normal[k] = Float.intBitsToFloat(source.records()[r + 16 + k]);
            batch.add(mesh, first, pose, normal, cursor, source.records()[r + InstanceRecord.COLOR],
                    source.records()[r + InstanceRecord.OVERLAY], source.records()[r + InstanceRecord.LIGHT]);
            // Adjacent reservations half the time, CPU-written vertices between them otherwise.
            cursor += mesh.vertexCount() * 36 + (random.nextBoolean() ? 0 : 36 * (1 + random.nextInt(5)));
        }
        int bytes = cursor + 36 * 3;
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
                return "uploadGaps refused ascending holes";
            }
            backend.dispatch(batch, vbo, true);
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
            backend.resetArena();
        }
    }

    static String glError() {
        int error = GL11C.glGetError();
        return error == GL11C.GL_NO_ERROR ? null : "0x" + Integer.toHexString(error);
    }
}
