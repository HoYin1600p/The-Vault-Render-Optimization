/*
 * Buffer handling adapted in part from Accelerated Rendering (MIT, Copyright (c) 2023 Argon4W),
 * core/meshes/MeshBuffer.java and core/buffers/accelerated/AcceleratedBufferSource.java at
 * 11f149ac716ec757907209dc77b04baaa3f915fc. Modified by HoYin1600p for The Vault Render
 * Optimization (VRO), 2026: GL 4.3 without DSA, persistent mapping or indirect draws; the output is
 * written into the vertex buffer vanilla has just uploaded instead of a separate draw.
 */
package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL31C;
import org.lwjgl.opengl.GL42C;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.system.MemoryUtil;

/**
 * The GL side: the compute program, the append-only mesh arena and the per-draw instance upload.
 * Render thread only. Every binding it changes is one vanilla and Embeddium do not cache
 * ({@code GL_SHADER_STORAGE_BUFFER}, {@code GL_COPY_READ/WRITE_BUFFER}); the program is reset so
 * the next {@code ShaderInstance.apply()} binds its own.
 */
public final class GpuEntityBackend {
    static final String SHADER = "model_instance_expand.comp";
    static final String PARTICLE_SHADER = "particle_expand.comp";
    static final int WORK_GROUP_SIZE = 64;
    static final int MAX_GROUPS_PER_DISPATCH = 65535;
    private static final int MESH_BYTES_PER_VERTEX = ModelMesh.FLOATS_PER_VERTEX * 4;

    private final long arenaMaxBytes;
    private int program;
    private int instanceBaseLocation;
    private int instanceCountLocation;
    /** The same program built for Oculus' extended entity vertices; 0 when it failed to build. */
    private int irisProgram;
    private int irisBaseLocation;
    private int irisCountLocation;
    private String irisFailure;
    /** The particle program; 0 when it failed to build (particles then stay on the CPU). */
    private int particleProgram;
    private int particleBaseLocation;
    private int particleCountLocation;
    private int cameraLeftLocation;
    private int cameraUpLocation;
    private String particleFailure;
    private int arena;
    private long arenaCapacity;
    private int arenaVertices;
    /** Item quads (ItemMesh words) have their own arena and programs; 0 when the programs failed to build. */
    private int itemProgram;
    private int itemBaseLocation;
    private int itemCountLocation;
    private String itemFailure;
    private int itemIrisProgram;
    private int itemIrisBaseLocation;
    private int itemIrisCountLocation;
    private String itemIrisFailure;
    private int itemArena;
    private long itemArenaCapacity;
    private int itemArenaVertices;
    private int instanceBuffer;
    private long instanceCapacity;
    private long ringHead;
    private long ringAlignment = 256;
    private IntBuffer staging = MemoryUtil.memAllocInt(InstanceRecord.WORDS * 256);
    private boolean closed;

    private GpuEntityBackend(long arenaMaxBytes) {
        this.arenaMaxBytes = arenaMaxBytes;
    }

    /** Compiles the program and allocates the buffers; throws with the driver's log on failure. */
    static GpuEntityBackend create(GpuEntityCapabilities.Route route, long arenaMaxBytes) {
        GpuEntityBackend backend = new GpuEntityBackend(arenaMaxBytes);
        try {
            backend.program = compile(route.header() + loadSource());
            bindBlock(backend.program, "MeshArena", 0);
            bindBlock(backend.program, "Instances", 1);
            bindBlock(backend.program, "Vertices", 2);
            backend.instanceBaseLocation = uniform(backend.program, "instanceBase");
            backend.instanceCountLocation = uniform(backend.program, "instanceCount");
            backend.arenaCapacity = Math.min(arenaMaxBytes, 1L << 20);
            backend.arena = newBuffer(backend.arenaCapacity, GL15C.GL_DYNAMIC_DRAW);
            backend.instanceCapacity = 4L << 20;
            backend.ringAlignment = Math.max(4, GL11C.glGetInteger(GL43C.GL_SHADER_STORAGE_BUFFER_OFFSET_ALIGNMENT));
            backend.instanceBuffer = newBuffer(backend.instanceCapacity, GL15C.GL_STREAM_DRAW);
            try {
                backend.irisProgram = compile(route.header() + "#define IRIS_ENTITY 1\n" + loadSource());
                bindBlock(backend.irisProgram, "MeshArena", 0);
                bindBlock(backend.irisProgram, "Instances", 1);
                bindBlock(backend.irisProgram, "Vertices", 2);
                backend.irisBaseLocation = uniform(backend.irisProgram, "instanceBase");
                backend.irisCountLocation = uniform(backend.irisProgram, "instanceCount");
            } catch (RuntimeException failure) {
                if (backend.irisProgram != 0) GL20C.glDeleteProgram(backend.irisProgram);
                backend.irisProgram = 0;
                backend.irisFailure = failure.getMessage();
            }
            try {
                backend.itemProgram = compile(route.header() + "#define ITEM_QUADS 1\n" + loadSource());
                bindBlock(backend.itemProgram, "MeshArena", 0);
                bindBlock(backend.itemProgram, "Instances", 1);
                bindBlock(backend.itemProgram, "Vertices", 2);
                backend.itemBaseLocation = uniform(backend.itemProgram, "instanceBase");
                backend.itemCountLocation = uniform(backend.itemProgram, "instanceCount");
                backend.itemArenaCapacity = Math.min(arenaMaxBytes, 1L << 20);
                backend.itemArena = newBuffer(backend.itemArenaCapacity, GL15C.GL_DYNAMIC_DRAW);
            } catch (RuntimeException failure) {
                if (backend.itemProgram != 0) GL20C.glDeleteProgram(backend.itemProgram);
                backend.itemProgram = 0;
                backend.itemFailure = failure.getMessage();
            }
            try {
                backend.itemIrisProgram = compile(route.header() + "#define ITEM_QUADS 1\n#define IRIS_ENTITY 1\n"
                        + loadSource());
                bindBlock(backend.itemIrisProgram, "MeshArena", 0);
                bindBlock(backend.itemIrisProgram, "Instances", 1);
                bindBlock(backend.itemIrisProgram, "Vertices", 2);
                backend.itemIrisBaseLocation = uniform(backend.itemIrisProgram, "instanceBase");
                backend.itemIrisCountLocation = uniform(backend.itemIrisProgram, "instanceCount");
            } catch (RuntimeException failure) {
                if (backend.itemIrisProgram != 0) GL20C.glDeleteProgram(backend.itemIrisProgram);
                backend.itemIrisProgram = 0;
                backend.itemIrisFailure = failure.getMessage();
            }
            try {
                backend.particleProgram = compile(route.header() + loadSource(PARTICLE_SHADER));
                bindBlock(backend.particleProgram, "Particles", 1);
                bindBlock(backend.particleProgram, "Vertices", 2);
                backend.particleBaseLocation = uniform(backend.particleProgram, "particleBase");
                backend.particleCountLocation = uniform(backend.particleProgram, "particleCount");
                backend.cameraLeftLocation = uniform(backend.particleProgram, "cameraLeft");
                backend.cameraUpLocation = uniform(backend.particleProgram, "cameraUp");
            } catch (RuntimeException failure) {
                if (backend.particleProgram != 0) GL20C.glDeleteProgram(backend.particleProgram);
                backend.particleProgram = 0;
                backend.particleFailure = failure.getMessage();
            }
            return backend;
        } catch (RuntimeException | Error failure) {
            backend.close();
            throw failure;
        }
    }

    static String loadSource() {
        return loadSource(SHADER);
    }

    static String loadSource(String name) {
        try (InputStream in = GpuEntityBackend.class.getResourceAsStream(name)) {
            if (in == null) throw new IllegalStateException(name + " is missing from the VRO jar");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("Could not read " + name, failure);
        }
    }

    boolean particlesAvailable() {
        return particleProgram != 0;
    }

    String particleFailure() {
        return particleFailure;
    }

    /** The item program built and the item arena exists. */
    boolean itemsAvailable() {
        return itemProgram != 0 && itemArena != 0;
    }

    String itemFailure() {
        return itemFailure;
    }

    boolean itemsIrisAvailable() {
        return itemIrisProgram != 0 && itemArena != 0;
    }

    String itemIrisFailure() {
        return itemIrisFailure;
    }

    boolean irisAvailable() {
        return irisProgram != 0;
    }

    String irisFailure() {
        return irisFailure;
    }

    static int compile(String source) {
        int shader = GL20C.glCreateShader(GL43C.GL_COMPUTE_SHADER);
        int program = 0;
        try {
            GL20C.glShaderSource(shader, source);
            GL20C.glCompileShader(shader);
            if (GL20C.glGetShaderi(shader, GL20C.GL_COMPILE_STATUS) != GL11C.GL_TRUE) {
                throw new IllegalStateException("compute shader did not compile: " + GL20C.glGetShaderInfoLog(shader));
            }
            program = GL20C.glCreateProgram();
            GL20C.glAttachShader(program, shader);
            GL20C.glLinkProgram(program);
            if (GL20C.glGetProgrami(program, GL20C.GL_LINK_STATUS) != GL11C.GL_TRUE) {
                String log = GL20C.glGetProgramInfoLog(program);
                GL20C.glDeleteProgram(program);
                throw new IllegalStateException("compute program did not link: " + log);
            }
            GL20C.glDetachShader(program, shader);
            return program;
        } finally {
            GL20C.glDeleteShader(shader);
        }
    }

    private static void bindBlock(int program, String name, int binding) {
        int index = GL43C.glGetProgramResourceIndex(program, GL43C.GL_SHADER_STORAGE_BLOCK, name);
        if (index == GL31C.GL_INVALID_INDEX) throw new IllegalStateException("storage block " + name + " is missing");
        GL43C.glShaderStorageBlockBinding(program, index, binding);
    }

    private static int uniform(int program, String name) {
        int location = GL20C.glGetUniformLocation(program, name);
        if (location < 0) throw new IllegalStateException("uniform " + name + " is missing");
        return location;
    }

    private static int newBuffer(long bytes, int usage) {
        int buffer = GL15C.glGenBuffers();
        GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, buffer);
        GL15C.glBufferData(GL43C.GL_SHADER_STORAGE_BUFFER, bytes, usage);
        GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, 0);
        return buffer;
    }

    /** Drains the GL error queue (only after an arena growth, which is rare): true if any was out of memory. */
    private static boolean outOfMemory() {
        boolean oom = false;
        for (int i = 0, error; i < 16 && (error = GL11C.glGetError()) != GL11C.GL_NO_ERROR; i++) {
            if (error == GL11C.GL_OUT_OF_MEMORY) oom = true;
        }
        return oom;
    }

    /**
     * Appends a mesh to the arena.
     *
     * @return its first vertex index, or -1 when the arena is at its cap (the part then stays on the CPU)
     */
    int upload(ModelMesh mesh) {
        long offset = (long) arenaVertices * MESH_BYTES_PER_VERTEX;
        long bytes = (long) mesh.vertexCount() * MESH_BYTES_PER_VERTEX;
        if (offset + bytes > arenaCapacity && !growArena(offset + bytes)) return -1;
        ByteBuffer data = MemoryUtil.memAlloc((int) bytes);
        try {
            data.asFloatBuffer().put(mesh.data(), 0, mesh.vertexCount() * ModelMesh.FLOATS_PER_VERTEX);
            GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, arena);
            GL15C.glBufferSubData(GL43C.GL_SHADER_STORAGE_BUFFER, offset, data);
            GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, 0);
        } finally {
            MemoryUtil.memFree(data);
        }
        int first = arenaVertices;
        arenaVertices += mesh.vertexCount();
        return first;
    }

    /**
     * Appends an item mesh to the item arena.
     *
     * @return its first vertex index, or -1 when the arena is missing or at its cap
     */
    int uploadItem(ItemMesh mesh) {
        if (itemArena == 0) return -1;
        long perVertex = (long) ItemMesh.WORDS_PER_VERTEX * 4;
        long offset = (long) itemArenaVertices * perVertex;
        long bytes = (long) mesh.vertexCount() * perVertex;
        if (offset + bytes > itemArenaCapacity && !growItemArena(offset + bytes)) return -1;
        ByteBuffer data = MemoryUtil.memAlloc((int) bytes);
        try {
            data.asIntBuffer().put(mesh.words(), 0, mesh.vertexCount() * ItemMesh.WORDS_PER_VERTEX);
            GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, itemArena);
            GL15C.glBufferSubData(GL43C.GL_SHADER_STORAGE_BUFFER, offset, data);
            GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, 0);
        } finally {
            MemoryUtil.memFree(data);
        }
        int first = itemArenaVertices;
        itemArenaVertices += mesh.vertexCount();
        return first;
    }

    private boolean growItemArena(long needed) {
        if (needed > arenaMaxBytes) return false;
        long capacity = itemArenaCapacity;
        while (capacity < needed) capacity = Math.min(arenaMaxBytes, capacity * 2);
        int grown = newBuffer(capacity, GL15C.GL_DYNAMIC_DRAW);
        long used = (long) itemArenaVertices * ItemMesh.WORDS_PER_VERTEX * 4;
        if (used > 0) {
            GL15C.glBindBuffer(GL31C.GL_COPY_READ_BUFFER, itemArena);
            GL15C.glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, grown);
            GL31C.glCopyBufferSubData(GL31C.GL_COPY_READ_BUFFER, GL31C.GL_COPY_WRITE_BUFFER, 0, 0, used);
            GL15C.glBindBuffer(GL31C.GL_COPY_READ_BUFFER, 0);
            GL15C.glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, 0);
        }
        if (outOfMemory()) {
            // The driver could not allocate or copy: keep the old arena; new meshes stay on the CPU.
            GL15C.glDeleteBuffers(grown);
            return false;
        }
        GL15C.glDeleteBuffers(itemArena);
        itemArena = grown;
        itemArenaCapacity = capacity;
        return true;
    }

    int itemArenaVertices() {
        return itemArenaVertices;
    }

    long itemArenaCapacity() {
        return itemArenaCapacity;
    }

    private boolean growArena(long needed) {
        if (needed > arenaMaxBytes) return false;
        long capacity = arenaCapacity;
        while (capacity < needed) capacity = Math.min(arenaMaxBytes, capacity * 2);
        int grown = newBuffer(capacity, GL15C.GL_DYNAMIC_DRAW);
        long used = (long) arenaVertices * MESH_BYTES_PER_VERTEX;
        if (used > 0) {
            GL15C.glBindBuffer(GL31C.GL_COPY_READ_BUFFER, arena);
            GL15C.glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, grown);
            GL31C.glCopyBufferSubData(GL31C.GL_COPY_READ_BUFFER, GL31C.GL_COPY_WRITE_BUFFER, 0, 0, used);
            GL15C.glBindBuffer(GL31C.GL_COPY_READ_BUFFER, 0);
            GL15C.glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, 0);
        }
        if (outOfMemory()) {
            // The driver could not allocate or copy: keep the old arena; new meshes stay on the CPU.
            GL15C.glDeleteBuffers(grown);
            return false;
        }
        GL15C.glDeleteBuffers(arena);
        arena = grown;
        arenaCapacity = capacity;
        return true;
    }

    /** Forgets every mesh (resource reload); callers bump the mesh generation at the same time. */
    void resetArena() {
        arenaVertices = 0;
        itemArenaVertices = 0;
    }

    int arenaVertices() {
        return arenaVertices;
    }

    long arenaCapacity() {
        return arenaCapacity;
    }

    /** Expands every record of {@code batch} into {@code vertexBuffer} (already uploaded by vanilla). */
    void dispatch(HoleBatch batch, int vertexBuffer, boolean readback) {
        int count = batch.count();
        IntBuffer records = stage(batch.words(), count);
        for (int i = 0; i < count; i++) {
            records.put(i * InstanceRecord.WORDS + InstanceRecord.OUTPUT, batch.outputWord(i));
        }
        run(batch.items() ? itemArena : arena, records, count, vertexBuffer, MAX_GROUPS_PER_DISPATCH, readback,
                batch.iris(), batch.items());
    }

    /**
     * Uploads {@code records} into the instance ring and runs the program. The ring is written at an
     * advancing, aligned offset and bound with {@code glBindBufferRange}; it is orphaned only when it
     * wraps, so a draw costs one small {@code glBufferSubData} rather than a reallocation. Bindings are
     * left in place (nothing else uses these SSBO slots) and the program is left bound: the caller
     * resets {@code ShaderInstance}'s program cache so vanilla's {@code apply()} binds its own.
     *
     * @param maxGroups lowered only by the self-test, to exercise the multi-dispatch path
     * @param readback  also make the writes visible to {@code glGetBufferSubData} (self-test/verify)
     */
    void run(int meshBuffer, IntBuffer records, int count, int outputBuffer, int maxGroups, boolean readback) {
        run(meshBuffer, records, count, outputBuffer, maxGroups, readback, false);
    }

    /** @param iris write Oculus' extended entity vertices (the IRIS_ENTITY program) */
    void run(int meshBuffer, IntBuffer records, int count, int outputBuffer, int maxGroups, boolean readback,
             boolean iris) {
        run(meshBuffer, records, count, outputBuffer, maxGroups, readback, iris, false);
    }

    /** @param items expand ItemMesh words (the ITEM_QUADS programs) instead of model meshes */
    void run(int meshBuffer, IntBuffer records, int count, int outputBuffer, int maxGroups, boolean readback,
             boolean iris, boolean items) {
        int selected = items ? (iris ? itemIrisProgram : itemProgram) : (iris ? irisProgram : program);
        int baseLocation = items ? (iris ? itemIrisBaseLocation : itemBaseLocation)
                : (iris ? irisBaseLocation : instanceBaseLocation);
        int countLocation = items ? (iris ? itemIrisCountLocation : itemCountLocation)
                : (iris ? irisCountLocation : instanceCountLocation);
        long bytes = (long) count * InstanceRecord.WORDS * 4;
        GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, instanceBuffer);
        if (bytes > instanceCapacity) {
            while (instanceCapacity < bytes) instanceCapacity *= 2;
            GL15C.glBufferData(GL43C.GL_SHADER_STORAGE_BUFFER, instanceCapacity, GL15C.GL_STREAM_DRAW);
            ringHead = 0;
        } else if (ringHead + bytes > instanceCapacity) {
            // Orphan on wrap: earlier regions may still be read by in-flight dispatches.
            GL15C.glBufferData(GL43C.GL_SHADER_STORAGE_BUFFER, instanceCapacity, GL15C.GL_STREAM_DRAW);
            ringHead = 0;
        }
        records.position(0).limit(count * InstanceRecord.WORDS);
        GL15C.glBufferSubData(GL43C.GL_SHADER_STORAGE_BUFFER, ringHead, records);

        GL20C.glUseProgram(selected);
        GL30C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, 0, meshBuffer);
        GL30C.glBindBufferRange(GL43C.GL_SHADER_STORAGE_BUFFER, 1, instanceBuffer, ringHead, bytes);
        GL30C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, 2, outputBuffer);
        GL30C.glUniform1ui(countLocation, count);
        for (int base = 0; base < count; base += maxGroups) {
            GL30C.glUniform1ui(baseLocation, base);
            GL43C.glDispatchCompute(Math.min(maxGroups, count - base), 1, 1);
        }
        ringHead = (ringHead + bytes + ringAlignment - 1) / ringAlignment * ringAlignment;
        GL42C.glMemoryBarrier(readback
                ? GL42C.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT | GL42C.GL_BUFFER_UPDATE_BARRIER_BIT
                : GL42C.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT);
    }

    /** Expands every particle record of {@code batch} into {@code vertexBuffer} (already uploaded by vanilla). */
    void dispatchParticles(HoleBatch batch, int vertexBuffer, boolean readback) {
        int count = batch.particleCount();
        IntBuffer records = stageWords(batch.particleWords(), count * ParticleRecord.WORDS);
        for (int i = 0; i < count; i++) {
            records.put(i * ParticleRecord.WORDS + ParticleRecord.OUTPUT, batch.holeStart(i) >> 2);
        }
        runParticles(records, count, batch.camera(), vertexBuffer, MAX_GROUPS_PER_DISPATCH, readback);
    }

    /** Particle records into the same instance ring, one thread per particle. */
    void runParticles(IntBuffer records, int count, float[] camera, int outputBuffer, int maxGroups, boolean readback) {
        long bytes = (long) count * ParticleRecord.WORDS * 4;
        GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, instanceBuffer);
        if (bytes > instanceCapacity) {
            while (instanceCapacity < bytes) instanceCapacity *= 2;
            GL15C.glBufferData(GL43C.GL_SHADER_STORAGE_BUFFER, instanceCapacity, GL15C.GL_STREAM_DRAW);
            ringHead = 0;
        } else if (ringHead + bytes > instanceCapacity) {
            GL15C.glBufferData(GL43C.GL_SHADER_STORAGE_BUFFER, instanceCapacity, GL15C.GL_STREAM_DRAW);
            ringHead = 0;
        }
        records.position(0).limit(count * ParticleRecord.WORDS);
        GL15C.glBufferSubData(GL43C.GL_SHADER_STORAGE_BUFFER, ringHead, records);
        GL20C.glUseProgram(particleProgram);
        GL30C.glBindBufferRange(GL43C.GL_SHADER_STORAGE_BUFFER, 1, instanceBuffer, ringHead, bytes);
        GL30C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, 2, outputBuffer);
        GL30C.glUniform1ui(particleCountLocation, count);
        GL20C.glUniform3f(cameraLeftLocation, camera[0], camera[1], camera[2]);
        GL20C.glUniform3f(cameraUpLocation, camera[3], camera[4], camera[5]);
        int perDispatch = maxGroups * WORK_GROUP_SIZE;
        for (int base = 0; base < count; base += perDispatch) {
            GL30C.glUniform1ui(particleBaseLocation, base);
            int groups = (Math.min(perDispatch, count - base) + WORK_GROUP_SIZE - 1) / WORK_GROUP_SIZE;
            GL43C.glDispatchCompute(groups, 1, 1);
        }
        ringHead = (ringHead + bytes + ringAlignment - 1) / ringAlignment * ringAlignment;
        GL42C.glMemoryBarrier(readback
                ? GL42C.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT | GL42C.GL_BUFFER_UPDATE_BARRIER_BIT
                : GL42C.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT);
    }

    IntBuffer stage(int[] words, int count) {
        return stageWords(words, count * InstanceRecord.WORDS);
    }

    IntBuffer stageWords(int[] words, int needed) {
        if (staging.capacity() < needed) {
            // Allocate before freeing: a failed allocation must leave staging valid for close().
            IntBuffer grown = MemoryUtil.memAllocInt(Math.max(needed, staging.capacity() * 2));
            MemoryUtil.memFree(staging);
            staging = grown;
        }
        staging.clear();
        staging.put(words, 0, needed);
        staging.flip();
        return staging;
    }

    int program() {
        return program;
    }

    void close() {
        if (closed) return;
        closed = true;
        if (program != 0) GL20C.glDeleteProgram(program);
        if (particleProgram != 0) GL20C.glDeleteProgram(particleProgram);
        if (irisProgram != 0) GL20C.glDeleteProgram(irisProgram);
        if (itemProgram != 0) GL20C.glDeleteProgram(itemProgram);
        if (itemIrisProgram != 0) GL20C.glDeleteProgram(itemIrisProgram);
        if (itemArena != 0) GL15C.glDeleteBuffers(itemArena);
        if (arena != 0) GL15C.glDeleteBuffers(arena);
        if (instanceBuffer != 0) GL15C.glDeleteBuffers(instanceBuffer);
        MemoryUtil.memFree(staging);
        program = particleProgram = irisProgram = itemProgram = itemIrisProgram = itemArena = arena = instanceBuffer = 0;
    }
}
