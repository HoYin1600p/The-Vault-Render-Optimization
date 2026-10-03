package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.system.MemoryUtil;

/** Item-mode helpers shared by the core and GL tests: item cases as {@link HoleBatch}es. */
final class GpuItemTestSupport {
    private GpuItemTestSupport() {
    }

    /**
     * The records of {@code test} added to a batch the way the item mixin adds them. With a backend the meshes
     * go through {@link GpuEntityBackend#uploadItem}; without one, arena positions are their concatenation
     * offsets (CPU-only tests).
     *
     * @return null when the arena refused a mesh
     */
    static HoleBatch batch(GpuEntitySelfTest.ItemCase test, GpuEntityBackend backend, boolean iris) {
        int meshes = test.meshes().size();
        int[] concatFirst = new int[meshes + 1];
        int[] arenaFirst = new int[meshes];
        for (int m = 0; m < meshes; m++) {
            concatFirst[m + 1] = concatFirst[m] + test.meshes().get(m).vertexCount();
            arenaFirst[m] = concatFirst[m];
            if (backend != null) {
                arenaFirst[m] = backend.uploadItem(test.meshes().get(m));
                if (arenaFirst[m] < 0) return null;
            }
        }
        HoleBatch batch = new HoleBatch();
        float[] pose = new float[16];
        float[] normal = new float[9];
        int m = 0;
        for (int i = 0; i < test.count(); i++) {
            int r = i * InstanceRecord.WORDS;
            int[] w = test.records();
            for (int k = 0; k < 16; k++) pose[k] = Float.intBitsToFloat(w[r + InstanceRecord.POSE + k]);
            for (int k = 0; k < 9; k++) normal[k] = Float.intBitsToFloat(w[r + InstanceRecord.NORMAL + k]);
            int global = w[r + InstanceRecord.MESH_FIRST];
            while (global >= concatFirst[m + 1]) m++;
            boolean embeddium = (w[r + InstanceRecord.SPRITE] & InstanceRecord.FLAG_ITEM_EMBEDDIUM) != 0;
            if (!batch.addItem(test.meshes().get(m), arenaFirst[m], global - concatFirst[m],
                    w[r + InstanceRecord.VERTEX_COUNT], pose, normal, w[r + InstanceRecord.OUTPUT] * 4,
                    w[r + InstanceRecord.COLOR], w[r + InstanceRecord.OVERLAY], w[r + InstanceRecord.LIGHT],
                    embeddium, iris, w[r + InstanceRecord.IRIS_IDS], w[r + InstanceRecord.IRIS_ITEM])) {
                throw new IllegalStateException("addItem refused record " + i);
            }
        }
        return batch;
    }

    /** A sentinel-filled little-endian buffer of the case's output size, in bytes. */
    static ByteBuffer sentinelBuffer(GpuEntitySelfTest.ItemCase test) {
        ByteBuffer buffer = MemoryUtil.memAlloc(test.outputWords() * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < test.outputWords(); i++) buffer.putInt(i * 4, GpuEntitySelfTest.SENTINEL);
        return buffer;
    }

    static String compare(GpuEntitySelfTest.ItemCase test, ByteBuffer actual, String who) {
        for (int i = 0; i < test.expected().length; i++) {
            int got = actual.getInt(i * 4);
            if (got != test.expected()[i]) {
                return who + (test.written()[i] ? " vertex word " : " gap word ") + i + " expected 0x"
                        + Integer.toHexString(test.expected()[i]) + " but got 0x" + Integer.toHexString(got);
            }
        }
        return null;
    }

    /**
     * Items through the arena path: meshes uploaded with {@link GpuEntityBackend#uploadItem}, records added
     * with {@link HoleBatch#addItem}, expanded by {@link GpuEntityBackend#dispatch}; the vertex buffer must equal
     * {@link ItemReference} word for word and {@link HoleBatch#fillOnCpu} must produce the same bytes.
     */
    static String arenaRoundTrip(GpuEntityBackend backend, long seed, int instances, boolean iris) {
        GpuEntitySelfTest.ItemCase test = GpuEntitySelfTest.buildItems(seed, instances, iris);
        backend.resetArena();
        ByteBuffer cpu = sentinelBuffer(test);
        ByteBuffer gpu = MemoryUtil.memAlloc(test.outputWords() * 4).order(ByteOrder.LITTLE_ENDIAN);
        int vbo = GL15C.glGenBuffers();
        try {
            HoleBatch batch = batch(test, backend, iris);
            if (batch == null) return "item arena refused a mesh";
            batch.fillOnCpu(cpu);
            String cpuResult = compare(test, cpu, "CPU fill");
            if (cpuResult != null) return cpuResult;

            ByteBuffer sentinel = sentinelBuffer(test);
            try {
                GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
                GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, sentinel, GL15C.GL_STREAM_DRAW);
                GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
            } finally {
                MemoryUtil.memFree(sentinel);
            }
            backend.dispatch(batch, vbo, true);
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
            GL15C.glGetBufferSubData(GL15C.GL_ARRAY_BUFFER, 0L, gpu);
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
            return compare(test, gpu, "GPU dispatch");
        } finally {
            GL15C.glDeleteBuffers(vbo);
            MemoryUtil.memFree(cpu);
            MemoryUtil.memFree(gpu);
            backend.resetArena();
        }
    }
}
