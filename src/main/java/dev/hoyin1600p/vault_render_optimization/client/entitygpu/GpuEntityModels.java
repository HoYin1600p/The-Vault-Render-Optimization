package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.model.geom.ModelPart;
import org.lwjgl.opengl.GL15C;

/**
 * GPU entity models: a model part drawn into a vanilla entity buffer reserves its vertices instead
 * of computing them on the CPU; when that buffer is uploaded, a compute shader writes the reserved
 * vertices straight into the uploaded vertex buffer, before vanilla's own draw call.
 *
 * <p>Nothing about the draw changes: the same buffer, draw order, render type state, shader and
 * uniforms, and the same bytes (the shader reproduces vanilla's float arithmetic and passed a
 * bit-for-bit self-test at startup). Whenever a reserved buffer reaches anything other than that
 * upload (sorting, another uploader, a {@code VertexBuffer}, a failure), the holes are filled on
 * the CPU with the exact vanilla bytes first, so no path ever sees unwritten vertices.
 *
 * <p>Render thread only. Off unless enabled in the config, outside Compare Mode, with a supported
 * driver, a passing self-test and a clean mixin audit. With an Oculus shader pack active, entity buffers use
 * Oculus' extended vertex format; a second program writes it (face normal, tangent, mid UV and entity IDs,
 * as Oculus computes them) after its own self-test, and the path pauses while a shader pack is active
 * if that program is unavailable.
 */
public final class GpuEntityModels {
    public enum State { UNINITIALIZED, ACTIVE, BLOCKED, FAILED }

    private static State state = State.UNINITIALIZED;
    private static String reason = "not initialized";
    private static GpuEntityCapabilities.Snapshot snapshot;
    private static GpuEntityBackend backend;
    private static long maxDrawBytes;

    /** Read on every model part: true only while the whole path is usable for this frame. */
    private static boolean frameActive;
    private static boolean watchXaeroTracing;
    private static int generation = 1;
    private static boolean resetRequested;
    private static boolean arenaFullLogged;

    // Upload hand-off between BufferUploader.end, BufferBuilder.popNextBuffer and BufferUploader._end.
    private static boolean uploadArmed;
    private static HoleBatch handoff;
    private static ByteBuffer handoffBuffer;

    private static final ArrayDeque<HoleBatch> POOL = new ArrayDeque<>();
    private static final float[] POSE = new float[16];
    private static final float[] NORMAL = new float[9];
    private static final float[] SPRITE = new float[4];
    private static final FloatBuffer POSE_BUFFER = FloatBuffer.wrap(POSE);
    private static final FloatBuffer NORMAL_BUFFER = FloatBuffer.wrap(NORMAL);

    public static final AtomicLong PARTS_GPU = new AtomicLong();
    public static final AtomicLong VERTICES_GPU = new AtomicLong();
    public static final AtomicLong DISPATCHES = new AtomicLong();
    public static final AtomicLong BATCHES_CPU_FILLED = new AtomicLong();
    public static final AtomicLong PARTS_NOT_ELIGIBLE = new AtomicLong();
    public static final AtomicLong LATE_FILLS = new AtomicLong();
    /** CPU fills of batches popped outside an immediate BufferUploader.end (not sorting fills). */
    public static final AtomicLong UNARMED_FILLS = new AtomicLong();
    /** Batches too small to be worth a dispatch (HUD icons, a held item), filled exactly on the CPU instead. */
    public static final AtomicLong SMALL_FILLS = new AtomicLong();
    /** Below this many reserved vertices a batch is filled on the CPU: a dispatch and barrier cost more. */
    static final int MIN_GPU_VERTICES = 96;
    /** Consecutive small batches after which a builder stops reserving (see BufferBuilderGpuMixin). */
    public static final int SMALL_STREAK = 8;
    private static int frameIndex;
    public static final AtomicLong PAUSES = new AtomicLong();
    public static final AtomicLong BYTES_NOT_UPLOADED = new AtomicLong();
    public static final AtomicLong VERIFIED_VERTICES = new AtomicLong();
    public static final AtomicLong VERIFY_MISMATCHES = new AtomicLong();
    /** GeckoLib cubes reserved for the GPU, and cubes left on GeckoLib's CPU path while the frame was active. */
    public static final AtomicLong GECKO_CUBES_GPU = new AtomicLong();
    public static final AtomicLong GECKO_NOT_ELIGIBLE = new AtomicLong();
    /** Verify mode: cached GeckoLib meshes that no longer matched their live cube and were recaptured. */
    public static final AtomicLong GECKO_MESH_CHANGED = new AtomicLong();
    /** Citadel (Alex's Mobs) AdvancedModelBox parts reserved for the GPU, and parts left on Citadel's path. */
    public static final AtomicLong CITADEL_PARTS_GPU = new AtomicLong();
    /** Particle quads reserved for the GPU. */
    public static final AtomicLong PARTICLES_GPU = new AtomicLong();
    private static volatile boolean particlesActive;
    /** Oculus extended entity vertices: the format when that program passed its self-test, else null. */
    private static volatile VertexFormat irisFormat;
    private static volatile String irisReason = "not initialized";
    /** Items: the self-tests of the item program without and with Oculus' extended format. */
    private static volatile boolean itemsActive;
    private static volatile boolean itemsIrisActive;
    private static volatile String itemReason = "not initialized";

    /** Draws of Oculus extended entity vertices written by the GPU. */
    public static final AtomicLong IRIS_DISPATCHES = new AtomicLong();
    private static volatile String particleReason = "not initialized";
    public static final AtomicLong CITADEL_NOT_ELIGIBLE = new AtomicLong();
    private static volatile boolean verify;
    private static String firstMismatch;

    private GpuEntityModels() {
    }

    /** Cached per {@code ModelPart}: its mesh and arena location for one generation. */
    public record CachedMesh(int generation, List<ModelPart.Cube> cubes, int cubeCount, ModelMesh mesh, int first) {
        public boolean gpu() {
            return first >= 0;
        }
    }

    /** True while model parts may reserve GPU vertices (checked on every part). */
    /** Block entities: parts drawn through a SpriteCoordinateExpander, unless another mod changed it. */
    public static boolean spriteWrappersAllowed() {
        return GpuEntityAudit.spriteBlocker() == null;
    }

    /** Particles have their own program and self-test; a failure there only keeps particles on the CPU. */
    private static void initializeParticles() {
        try {
            String result = GpuParticleSelfTest.run(backend, 0x5641_5254L, 1024);
            restoreProgram();
            particlesActive = result == null;
            particleReason = result == null ? "self-test passed" : "CPU only - self-test: " + result;
        } catch (Throwable failure) {
            restoreProgram();
            particlesActive = false;
            particleReason = "CPU only - " + failure;
        }
        VaultRenderOptimization.LOGGER.info("GPU particles: {}", particleReason);
    }

    /** Oculus' extended entity path: its own self-test; a failure only pauses the path under shader packs. */
    private static void initializeIris() {
        if (!OculusExtendedEntity.installed()) {
            irisReason = "Oculus not installed";
            return;
        }
        try {
            VertexFormat format = OculusExtendedEntity.format();
            String result = format == null ? "IrisVertexFormats.ENTITY unavailable"
                    : !backend.irisAvailable() ? "program: " + backend.irisFailure()
                    : GpuEntitySelfTest.run(backend, 0x4952_4953L, 512, true);
            restoreProgram();
            irisFormat = result == null ? format : null;
            irisReason = result == null ? "self-test passed" : "paused under shader packs - " + result;
        } catch (Throwable failure) {
            restoreProgram();
            irisFormat = null;
            irisReason = "paused under shader packs - " + failure;
        }
        VaultRenderOptimization.LOGGER.info("GPU entity models with Oculus shader packs: {}", irisReason);
    }

    /** Oculus' extended entity format while its GPU program is usable, else null. */
    public static VertexFormat irisFormat() {
        return irisFormat;
    }

    /** Oculus' builder fields could not be read: extended vertices stay on the CPU for the session. */
    public static void disableIris(String why) {
        irisFormat = null;
        irisReason = "paused under shader packs - " + why;
        VaultRenderOptimization.LOGGER.warn("GPU entity models with Oculus shader packs: {}", irisReason);
    }

    /** Items have their own program and self-tests; a failure there only keeps items on the CPU. */
    private static void initializeItems() {
        try {
            String result = !backend.itemsAvailable() ? "program: " + backend.itemFailure()
                    : GpuEntitySelfTest.run(backend, 0x4954_454DL, 512, true, false);
            restoreProgram();
            itemsActive = result == null;
            String iris = !itemsActive || irisFormat == null ? "not needed"
                    : !backend.itemsIrisAvailable() ? "program: " + backend.itemFailure()
                    : GpuEntitySelfTest.run(backend, 0x4954_4952L, 512, true, true);
            restoreProgram();
            itemsIrisActive = iris == null;
            itemReason = result != null ? "CPU only - self-test: " + result
                    : "self-test passed" + (irisFormat == null ? "" : iris == null ? " (Oculus extended too)"
                    : "; with shader packs CPU only - " + iris);
        } catch (Throwable failure) {
            restoreProgram();
            itemsActive = itemsIrisActive = false;
            itemReason = "CPU only - " + failure;
        }
        VaultRenderOptimization.LOGGER.info("GPU items: {}", itemReason);
    }

    /** True while item quad lists may reserve GPU vertices this frame (the builder format is checked per list). */
    public static boolean itemFrameActive() {
        return itemsActive && frameActive();
    }

    /** Items into Oculus' extended format need that program's self-test too. */
    public static boolean itemsIrisActive() {
        return itemsIrisActive;
    }

    /** True while billboard particles may reserve GPU vertices this frame. */
    public static boolean particleFrameActive() {
        return particlesActive && ClientOptimizationConfig.gpuParticles
                && (frameActive() || particlesOnlyFrame && !(watchXaeroTracing && XaeroIconTraceProbe.tracing()));
    }

    /** Shader pack on, models paused: particles may still use the GPU (gpu_particles_with_shaders). */
    private static boolean particlesOnlyFrame;

    /** True while an Oculus shader pack keeps models on the CPU but particles on the GPU. */
    static boolean shaderMode(boolean all) {
        return all ? irisFormat != null && ClientOptimizationConfig.gpuEntityModelsWithShaders
                : ClientOptimizationConfig.gpuParticlesWithShaders;
    }

    public static boolean frameActive() {
        return frameActive && !(watchXaeroTracing && XaeroIconTraceProbe.tracing());
    }

    /** {@code LevelRenderer.renderLevel} HEAD: initialize lazily and decide whether this frame may reserve. */
    public static void beginFrame() {
        frameActive = false;
        particlesOnlyFrame = false;
        frameIndex++;
        dropAllParked();
        if (!ClientOptimizationConfig.gpuEntityModels || ClientOptimizationConfig.compareModeEnabled()) return;
        if (state == State.UNINITIALIZED) initialize();
        if (state != State.ACTIVE) return;
        if (resetRequested) {
            resetRequested = false;
            backend.resetArena();
            GpuItems.reset();
            GpuBlockModels.reset();
            UPLOADED.clear();
            UPLOADED_ITEMS.clear();
            generation++;
            arenaFullLogged = false;
        }
        if (OculusShaderPackProbe.shaderPackActive() && !shaderMode(true)) {
            // Particles keep the PARTICLE format under shader packs, so they need no extended program.
            particlesOnlyFrame = shaderMode(false) && effectiveness.allowFrame();
            return;
        }
        frameActive = effectiveness.allowFrame();
    }

    /**
     * Watches whether reserved parts actually reach the GPU. Some buffer sources (for example
     * Oculus' batched entity rendering, even with shaders off) pop finished buffers outside
     * {@code BufferUploader.end}; every reservation is then filled on the CPU, which is exact but
     * pure overhead. When a probe window ends with such fills and no dispatch, reservations pause
     * for a while and are retried later, so the path costs nothing where it cannot help.
     */
    static final class Effectiveness {
        Effectiveness() {
            restart();
        }

        static final int PROBE_FRAMES = 120;
        static final int PAUSE_FRAMES = 3600;

        private int frames;
        private int pausedFrames;
        private long dispatchesAtStart;
        private long unarmedAtStart;
        private boolean paused;
        private String reason = "";

        boolean allowFrame() {
            if (paused) {
                if (++pausedFrames < PAUSE_FRAMES) return false;
                paused = false;
                restart();
                return true;
            }
            if (++frames >= PROBE_FRAMES) {
                long dispatches = DISPATCHES.get() - dispatchesAtStart;
                long unarmed = UNARMED_FILLS.get() - unarmedAtStart;
                if (dispatches == 0 && unarmed > 0) {
                    paused = true;
                    pausedFrames = 0;
                    PAUSES.incrementAndGet();
                    reason = unarmed + " reserved batches in " + PROBE_FRAMES + " frames were drawn outside "
                            + "BufferUploader.end (buffer source " + bufferSourceName() + "); retrying later";
                    return false;
                }
                restart();
            }
            return true;
        }

        private void restart() {
            frames = 0;
            dispatchesAtStart = DISPATCHES.get();
            unarmedAtStart = UNARMED_FILLS.get();
        }

        boolean paused() {
            return paused;
        }

        String reason() {
            return reason;
        }
    }

    private static final Effectiveness effectiveness = new Effectiveness();

    private static String bufferSourceName() {
        try {
            return net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource().getClass().getName();
        } catch (Throwable failure) {
            return "unknown";
        }
    }

    private static void initialize() {
        String audit = GpuEntityAudit.blocker();
        if (audit != null) {
            block("mixin audit: " + audit);
            return;
        }
        if (GpuEntityAudit.xaeroTraceHookPresent()) {
            String xaero = XaeroIconTraceProbe.resolve();
            if (xaero != null) {
                block(xaero);
                return;
            }
            watchXaeroTracing = true;
        }
        try {
            snapshot = GpuEntityCapabilities.read();
            String blocker = GpuEntityCapabilities.blocker(snapshot);
            if (blocker != null) {
                block(blocker);
                return;
            }
            backend = GpuEntityBackend.create(GpuEntityCapabilities.route(snapshot),
                    Math.min(GpuEntityCapabilities.ARENA_MAX_BYTES, snapshot.maxStorageBlockSize()));
            maxDrawBytes = snapshot.maxStorageBlockSize();
            String selfTest = GpuEntitySelfTest.run(backend, 0x5652_4F47L, 512);
            restoreProgram();
            if (selfTest != null) {
                backend.close();
                backend = null;
                block("self-test mismatch: " + selfTest);
                return;
            }
            state = State.ACTIVE;
            reason = "self-test passed on " + snapshot.renderer() + " (" + GpuEntityCapabilities.route(snapshot) + ")";
            VaultRenderOptimization.LOGGER.info("GPU entity models: ACTIVE - {}", reason);
            initializeParticles();
            initializeIris();
            initializeItems();
        } catch (Throwable failure) {
            restoreProgram();
            if (backend != null) {
                try {
                    backend.close();
                } catch (Throwable ignored) {
                }
                backend = null;
            }
            state = State.BLOCKED;
            reason = "initialization failed: " + failure;
            VaultRenderOptimization.LOGGER.warn("GPU entity models: BLOCKED - {}", reason, failure);
        }
    }

    private static void block(String why) {
        state = State.BLOCKED;
        reason = why;
        VaultRenderOptimization.LOGGER.info("GPU entity models: BLOCKED - {}", why);
    }

    private static void fail(String why, Throwable failure) {
        frameActive = false;
        if (state == State.FAILED) return;
        state = State.FAILED;
        reason = why + ": " + failure;
        VaultRenderOptimization.LOGGER.error("GPU entity models: FAILED, using the CPU path for the rest of the session - {}",
                reason, failure);
    }

    /**
     * Hot path: the compute program stays bound until vanilla's {@code apply()}, which rebinds its
     * own program because its cache is reset here.
     */
    private static void forgetProgram() {
        dev.hoyin1600p.vault_render_optimization.mixin.entitygpu.ShaderInstanceGpuAccessor.vro$setLastProgramId(-1);
    }

    /** The next {@code ShaderInstance.apply()} must bind its own program again. */
    private static void restoreProgram() {
        org.lwjgl.opengl.GL20C.glUseProgram(0);
        dev.hoyin1600p.vault_render_optimization.mixin.entitygpu.ShaderInstanceGpuAccessor.vro$setLastProgramId(-1);
    }

    /** Resource reload: meshes are recaptured into a fresh arena at the next frame. */
    public static void requestReset() {
        resetRequested = true;
    }

    // ---- model parts ----------------------------------------------------------------------------

    /** Returns the part's mesh for this generation, capturing and uploading it on first use. */
    public static CachedMesh mesh(ModelPart part, CachedMesh cached) {
        if (cached != null && cached.generation() == generation && cached.cubes() == part.cubes
                && cached.cubeCount() == part.cubes.size()) {
            return cached;
        }
        ModelMesh mesh = ModelMeshCapture.capture(part);
        int first = mesh == null ? -1 : uploadMesh(mesh);
        return new CachedMesh(generation, part.cubes, part.cubes.size(), mesh == null ? ModelMesh.EMPTY : mesh, first);
    }

    /** Arena contents by geometry (float or int words compared exactly), for reuse across equal meshes. */
    private static final java.util.Map<MeshKey, Integer> UPLOADED = new java.util.HashMap<>();
    private static final java.util.Map<MeshKey, Integer> UPLOADED_ITEMS = new java.util.HashMap<>();
    public static final AtomicLong MESHES_SHARED = new AtomicLong();

    /** Exact content key: equal only when every word's bits are equal. */
    static final class MeshKey {
        private final int[] bits;
        private final int hash;

        MeshKey(float[] data, int length) {
            bits = new int[length];
            for (int i = 0; i < length; i++) bits[i] = Float.floatToRawIntBits(data[i]);
            hash = java.util.Arrays.hashCode(bits);
        }

        MeshKey(int[] data, int length) {
            bits = java.util.Arrays.copyOf(data, length);
            hash = java.util.Arrays.hashCode(bits);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof MeshKey key && key.hash == hash && java.util.Arrays.equals(key.bits, bits);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    /** The arena generation; cached meshes from an older generation must be uploaded again. */
    public static int generation() {
        return generation;
    }

    /** True while the live verifier runs (GeckoLib then re-checks cached meshes against live cubes). */
    public static boolean verifying() {
        return verify;
    }

    /**
     * Uploads a captured mesh to the arena (only while the path is active on the render thread).
     *
     * @return its first arena vertex, 0 for an empty mesh, or -1 when the arena is full
     */
    public static int uploadMesh(ModelMesh mesh) {
        if (mesh.vertexCount() == 0) return 0;
        // Identical geometry reuses its arena range: renderers that build a new ModelPart every frame (or many
        // parts with the same cubes) would otherwise grow the arena without bound.
        MeshKey key = new MeshKey(mesh.data(), mesh.vertexCount() * ModelMesh.FLOATS_PER_VERTEX);
        Integer known = UPLOADED.get(key);
        if (known != null) {
            MESHES_SHARED.incrementAndGet();
            return known;
        }
        int first = backend.upload(mesh);
        if (first >= 0) UPLOADED.put(key, first);
        if (first < 0 && !arenaFullLogged) {
            arenaFullLogged = true;
            VaultRenderOptimization.LOGGER.warn("GPU entity models: mesh arena is full ({} vertices); "
                    + "further new models use the CPU path until the next resource reload", backend.arenaVertices());
        }
        return first;
    }

    /**
     * Uploads a captured item mesh to the item arena.
     *
     * @return its first arena vertex, 0 for an empty mesh, or -1 when the arena is full
     */
    public static int uploadItemMesh(ItemMesh mesh) {
        if (mesh.vertexCount() == 0) return 0;
        MeshKey key = new MeshKey(mesh.words(), mesh.vertexCount() * ItemMesh.WORDS_PER_VERTEX);
        Integer known = UPLOADED_ITEMS.get(key);
        if (known != null) {
            MESHES_SHARED.incrementAndGet();
            return known;
        }
        int first = backend.uploadItem(mesh);
        if (first >= 0) UPLOADED_ITEMS.put(key, first);
        if (first < 0 && !arenaFullLogged) {
            arenaFullLogged = true;
            VaultRenderOptimization.LOGGER.warn("GPU entity models: item arena is full; further new item models use the CPU path "
                    + "until the next resource reload");
        }
        return first;
    }

    /**
     * Reserves a cached mesh for the current pose (GeckoLib cubes with their flat-cube normal flips, Citadel
     * parts with none). The caller counts it.
     */
    public static void reserveMesh(GpuHoleBuilder builder, ModelMesh mesh, int first, PoseStack.Pose pose, int light,
                                   int overlay, float red, float green, float blue, float alpha, int flips) {
        pose.pose().store(POSE_BUFFER);
        pose.normal().store(NORMAL_BUFFER);
        int color = EntityVertexPacking.color(red, green, blue, alpha);
        builder.vro$reserve(mesh, first, POSE, NORMAL, color, overlay, light, null, flips);
    }

    /** Reserves {@code mesh}'s vertices in {@code builder} for the current pose. */
    public static void reserve(GpuHoleBuilder builder, CachedMesh mesh, PoseStack.Pose pose, int light, int overlay,
                               float red, float green, float blue, float alpha,
                               net.minecraft.client.renderer.texture.TextureAtlasSprite atlasSprite) {
        pose.pose().store(POSE_BUFFER);
        pose.normal().store(NORMAL_BUFFER);
        int color = EntityVertexPacking.color(red, green, blue, alpha);
        float[] sprite = null;
        if (atlasSprite != null) {
            // The same floats SpriteCoordinateExpander's getU/getV compute from: u0 and u1 - u0.
            SPRITE[0] = atlasSprite.getU0();
            SPRITE[1] = atlasSprite.getU1() - atlasSprite.getU0();
            SPRITE[2] = atlasSprite.getV0();
            SPRITE[3] = atlasSprite.getV1() - atlasSprite.getV0();
            sprite = SPRITE;
        }
        builder.vro$reserve(mesh.mesh(), mesh.first(), POSE, NORMAL, color, overlay, light, sprite, 0);
        PARTS_GPU.incrementAndGet();
    }

    /**
     * Called when a buffer source hands out {@code consumer} for a render type: sorted (translucent)
     * types go straight to vanilla's path, since sorting would force a CPU fill anyway.
     */
    public static void hintSorting(com.mojang.blaze3d.vertex.VertexConsumer consumer, boolean sorting) {
        if (consumer instanceof GpuHoleBuilder builder) builder.vro$setSortingHint(sorting);
    }

    public static HoleBatch borrowBatch() {
        HoleBatch batch = POOL.poll();
        return batch != null ? batch : new HoleBatch();
    }

    public static void release(HoleBatch batch) {
        batch.reset();
        if (POOL.size() < 64) POOL.push(batch);
    }

    // ---- upload hand-off ------------------------------------------------------------------------

    /** {@code BufferUploader.end} HEAD: the next pop on this thread feeds the immediate upload. */
    public static void armUpload() {
        uploadArmed = RenderSystem.isOnRenderThread();
    }

    /** {@code BufferUploader.end} RETURN. */
    public static void disarmUpload() {
        uploadArmed = false;
        if (handoff != null) {
            // Only reachable if _end skipped its hook; fill so the buffer is at least never left with holes.
            LATE_FILLS.incrementAndGet();
            handoff.fillOnCpu(handoffBuffer);
            release(handoff);
            handoff = null;
            handoffBuffer = null;
        }
    }

    // ---- Oculus batched entity rendering (public Oculus 1.6.x) ----------------------------------
    //
    // Oculus' FullyBufferedMultiBufferSource pops every render type's slice in
    // SegmentedBufferBuilder.getSegments() and draws the slices later in the same flush through
    // BufferUploader.end with a stand-in builder pointed at the same memory. Batches popped inside
    // getSegments are parked by slice address instead of being filled on the CPU; the stand-in
    // builder's upload of that memory takes them to the GPU as usual. A parked batch is dropped as
    // soon as its builder starts writing again (the memory is being reused) and at every frame start.

    /** {@code slice} views the memory the batch was popped into, at the size it was popped with. */
    private record Parked(HoleBatch batch, Object owner, int vertexCount, VertexFormat format, ByteBuffer slice) {
    }

    private static int segmentPopDepth;
    private static final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<Parked> PARKED =
            new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
    public static final AtomicLong PARKED_BATCHES = new AtomicLong();
    public static final AtomicLong PARKED_DROPPED = new AtomicLong();

    /** Oculus SegmentedBufferBuilder.getSegments HEAD/RETURN. */
    public static void beginSegmentPops() {
        segmentPopDepth++;
    }

    public static void endSegmentPops() {
        if (segmentPopDepth > 0) segmentPopDepth--;
    }

    /** {@code BufferBuilder.begin} HEAD: memory this builder handed out is about to be reused. */
    public static void builderBegins(Object owner) {
        if (PARKED.isEmpty()) return;
        var iterator = PARKED.long2ObjectEntrySet().fastIterator();
        while (iterator.hasNext()) {
            Parked parked = iterator.next().getValue();
            if (parked.owner() == owner) {
                PARKED_DROPPED.incrementAndGet();
                release(parked.batch());
                iterator.remove();
            }
        }
    }

    private static void dropAllParked() {
        if (PARKED.isEmpty()) return;
        for (Parked parked : PARKED.values()) {
            PARKED_DROPPED.incrementAndGet();
            release(parked.batch());
        }
        PARKED.clear();
    }

    /** Increases at every frame start (GPU items revalidate cached quad lists once per frame). */
    public static int frameIndex() {
        return frameIndex;
    }

    /** {@code BufferBuilder.popNextBuffer} RETURN for a draw state that has holes. */
    public static void popped(HoleBatch batch, BufferBuilder.DrawState drawState, ByteBuffer slice, Object owner) {
        boolean armed = uploadArmed;
        uploadArmed = false;
        boolean small = batch.vertices() < MIN_GPU_VERTICES;
        if (owner instanceof GpuHoleBuilder builder) builder.vro$noteBatch(small);
        if (small && !verify) {
            SMALL_FILLS.incrementAndGet();
            fillOnCpu(batch, slice);
            return;
        }
        long bytes = (long) drawState.vertexCount() * drawState.format().getVertexSize();
        boolean gpuReady = state == State.ACTIVE && bytes <= maxDrawBytes && (batch.particles()
                ? drawState.format() == DefaultVertexFormat.PARTICLE && particlesActive
                : batch.items() && !(batch.iris() ? itemsIrisActive : itemsActive) ? false
                : batch.iris() ? irisFormat != null && drawState.format() == irisFormat
                : drawState.format() == DefaultVertexFormat.NEW_ENTITY);
        if (armed && gpuReady && handoff == null) {
            handoff = batch;
            handoffBuffer = slice;
            return;
        }
        if (!armed && gpuReady && segmentPopDepth > 0 && RenderSystem.isOnRenderThread()) {
            long address = org.lwjgl.system.MemoryUtil.memAddress(slice);
            Parked previous = PARKED.put(address,
                    new Parked(batch, owner, drawState.vertexCount(), drawState.format(), slice.duplicate()));
            if (previous != null) {
                PARKED_DROPPED.incrementAndGet();
                release(previous.batch());
            }
            PARKED_BATCHES.incrementAndGet();
            return;
        }
        if (!armed) UNARMED_FILLS.incrementAndGet();
        fillOnCpu(batch, slice);
    }

    /** Item batches in sorted buffers that kept their holes (only their sort positions written on the CPU). */
    public static final AtomicLong SORTED_ITEM_BATCHES = new AtomicLong();
    public static final AtomicLong SORT_POSITION_MISMATCHES = new AtomicLong();

    /** {@code BufferBuilder.setQuadSortOrigin} HEAD for an item batch (sort positions already written). */
    public static void sortedItemBatch(HoleBatch batch, ByteBuffer builder) {
        SORTED_ITEM_BATCHES.incrementAndGet();
        if (!verify) return;
        String problem = batch.checkSortPositions(builder);
        if (problem != null) {
            SORT_POSITION_MISMATCHES.incrementAndGet();
            if (firstMismatch == null) {
                firstMismatch = problem;
                VaultRenderOptimization.LOGGER.warn("GPU items sort position mismatch: {}", problem);
            }
        }
    }

    /** Model holes filled on the CPU right before a sort, by whether the builder's source hinted at all. */
    public static final AtomicLong SORT_FILLS_HINTED = new AtomicLong();
    public static final AtomicLong SORT_FILLS_UNHINTED = new AtomicLong();
    private static final java.util.Set<String> SORT_FILL_CALLERS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * {@code setQuadSortOrigin} is about to sort a builder that holds model holes (a reservation into a sorted
     * buffer: pure overhead). Logs the first few distinct call stacks so the buffer source can be identified.
     */
    public static void sortFill(boolean hinted) {
        (hinted ? SORT_FILLS_HINTED : SORT_FILLS_UNHINTED).incrementAndGet();
        if (SORT_FILL_CALLERS.size() >= 5) return;
        StackTraceElement[] stack = new Throwable().getStackTrace();
        StringBuilder key = new StringBuilder();
        for (int i = 2; i < Math.min(stack.length, 14); i++) key.append("\n    at ").append(stack[i]);
        if (SORT_FILL_CALLERS.add(key.toString())) {
            VaultRenderOptimization.LOGGER.info("GPU entity models: model holes in a sorted buffer (source {}), sorted from:{}",
                    hinted ? "hinted" : "never hinted", key);
        }
    }

    /** Any other consumer of a buffer with holes (sorting, other uploaders): fill with vanilla's bytes. */
    public static void fillOnCpu(HoleBatch batch, ByteBuffer popped) {
        BATCHES_CPU_FILLED.incrementAndGet();
        batch.fillOnCpu(popped);
        release(batch);
    }

    /**
     * A pop without holes of its own. If it is the immediate upload of memory a parked batch belongs
     * to (Oculus' stand-in builder), hand that batch to the GPU; either way the arm is consumed.
     */
    public static void poppedWithoutHoles(BufferBuilder.DrawState drawState, ByteBuffer slice) {
        boolean armed = uploadArmed;
        uploadArmed = false;
        if (!armed || PARKED.isEmpty() || handoff != null) return;
        Parked parked = PARKED.remove(org.lwjgl.system.MemoryUtil.memAddress(slice));
        if (parked == null) return;
        if (state != State.ACTIVE || drawState.vertexCount() != parked.vertexCount()
                || drawState.format() != parked.format()) {
            // Not the draw state it was parked with: never upload holes. Fill the memory the batch was popped into,
            // at its own size: this upload's slice can be shorter, and the batch's offsets are relative to its own pop.
            LATE_FILLS.incrementAndGet();
            fillOnCpu(parked.batch(), parked.slice());
            return;
        }
        handoff = parked.batch();
        handoffBuffer = slice;
    }

    /**
     * Replaces {@code BufferUploader._end}'s vertex {@code glBufferData}. When this upload carries a
     * reserved batch, only the bytes the CPU wrote are copied: the buffer is allocated at full size
     * and each range between reserved vertices is uploaded, so reserved vertices (overwritten by the
     * GPU right after) are never copied by the driver. Any other upload is vanilla's call unchanged.
     */
    public static void uploadVertices(int target, ByteBuffer buffer, int usage, VertexFormat format) {
        HoleBatch batch = handoff;
        if (batch == null || handoffBuffer != buffer) {
            com.mojang.blaze3d.platform.GlStateManager._glBufferData(target, buffer, usage);
            if (batch != null) uploaded(buffer, format);
            return;
        }
        if (!uploadGaps(target, buffer, usage, batch)) {
            com.mojang.blaze3d.platform.GlStateManager._glBufferData(target, buffer, usage);
        }
        uploaded(buffer, format);
    }

    /** @return false when the holes are not in ascending order (never expected): upload everything */
    static boolean uploadGaps(int target, ByteBuffer buffer, int usage, HoleBatch batch) {
        int size = buffer.remaining();
        int cursor = 0;
        for (int i = 0; i < batch.holeCount(); i++) {
            int start = batch.holeStart(i);
            if (start < cursor) return false;
            cursor = batch.holeEnd(i);
        }
        if (cursor > size) return false;
        GL15C.glBufferData(target, size, usage);
        long address = org.lwjgl.system.MemoryUtil.memAddress(buffer);
        cursor = 0;
        long skipped = 0;
        for (int i = 0; i < batch.holeCount(); i++) {
            int start = batch.holeStart(i);
            int end = batch.holeEnd(i);
            if (start > cursor) GL15C.nglBufferSubData(target, cursor, start - cursor, address + cursor);
            skipped += end - start;
            cursor = end;
        }
        if (cursor < size) GL15C.nglBufferSubData(target, cursor, size - cursor, address + cursor);
        BYTES_NOT_UPLOADED.addAndGet(skipped);
        return true;
    }

    /** {@code BufferUploader._end}, right after the vertex upload and before the draw. */
    public static void uploaded(ByteBuffer buffer, VertexFormat format) {
        HoleBatch batch = handoff;
        if (batch == null) return;
        ByteBuffer expected = handoffBuffer;
        handoff = null;
        handoffBuffer = null;
        if (expected != buffer) {
            // Not the buffer that was popped for this upload; never draw holes.
            LATE_FILLS.incrementAndGet();
            fillOnCpu(batch, expected);
            return;
        }
        try {
            if (batch.particles()) {
                backend.dispatchParticles(batch, format.getOrCreateVertexBufferObject(), verify);
                PARTICLES_GPU.addAndGet(batch.particleCount());
            } else {
                backend.dispatch(batch, format.getOrCreateVertexBufferObject(), verify);
                if (batch.iris()) IRIS_DISPATCHES.incrementAndGet();
            }
            forgetProgram();
            if (verify) verify(batch, buffer, format);
            DISPATCHES.incrementAndGet();
            VERTICES_GPU.addAndGet(batch.vertices());
            release(batch);
        } catch (Throwable failure) {
            try {
                restoreProgram();
                batch.fillOnCpu(buffer);
                // The vertex buffer is still bound by BufferUploader; replace the uploaded bytes.
                int limit = buffer.limit();
                buffer.position(0);
                GL15C.glBufferSubData(GL15C.GL_ARRAY_BUFFER, 0L, buffer);
                buffer.position(0).limit(limit);
                release(batch);
            } catch (Throwable secondary) {
                failure.addSuppressed(secondary);
            }
            fail("dispatch failed", failure);
        }
    }

    // ---- status ---------------------------------------------------------------------------------

    public static State state() {
        return state;
    }

    public static String status() {
        StringBuilder text = new StringBuilder();
        text.append("GPU entity models: ");
        if (!ClientOptimizationConfig.gpuEntityModels) text.append("OFF (config render_fast_paths.gpu_entity_models)");
        else if (ClientOptimizationConfig.compareModeEnabled()) text.append("OFF (Compare Mode)");
        else text.append(state).append(" - ").append(reason);
        if (state == State.ACTIVE) {
            if (effectiveness.paused()) text.append("; paused: ").append(effectiveness.reason());
            else if (OculusShaderPackProbe.shaderPackActive() && !shaderMode(true)) {
                text.append("; models paused (Oculus shader pack active").append(irisFormat != null ? "; /vro feature gpushaders on to keep them on)" : ")")
                        .append(particlesOnlyFrame ? "; particles on the GPU" : "");
            }
            else text.append(frameActive ? "; drawing this frame" : "; idle");
        }
        String audit = GpuEntityAudit.blocker();
        text.append("\nMixin audit: ").append(audit == null ? "passed" : audit);
        String sprite = GpuEntityAudit.spriteBlocker();
        text.append("\nBlock entities (sprite wrapper): ").append(sprite == null ? "on" : "CPU only - " + sprite);
        String gecko = GpuEntityAudit.geckoBlocker();
        text.append("\nGeckoLib models: ").append(!GpuEntityAudit.geckoAudited() ? "GeckoLib 3 not installed"
                : gecko == null ? "on" : "CPU only - " + gecko);
        if (GpuEntityAudit.geckoAudited()) {
            String block = GpuEntityAudit.geckoBlockBlocker();
            text.append("; block renderers ").append(block == null ? "on" : "CPU only - " + block);
        }
        String ars = GpuEntityAudit.arsGeckoBlocker();
        text.append("\nArs Nouveau GeckoLib models: ").append(!GpuEntityAudit.arsGeckoAudited() ? "not installed"
                : ars == null ? "on" : "CPU only - " + ars);
        text.append("\nOculus shader packs (extended vertices): ").append(irisReason)
                .append(ClientOptimizationConfig.gpuEntityModelsWithShaders ? "" : "; off (config render_fast_paths.gpu_entity_models_with_shaders)");
        text.append("\n").append(GpuItems.status()).append("; ").append(itemReason);
        text.append("\n").append(GpuBlockModels.status());
        String citadel = GpuEntityAudit.citadelBlocker();
        text.append("\nParticles: ").append(!ClientOptimizationConfig.gpuParticles ? "OFF (config render_fast_paths.gpu_particles)"
                : particleReason);
        text.append("\nCitadel models (Alex's Mobs): ").append(!GpuEntityAudit.citadelAudited() ? "not installed"
                : citadel == null ? "on" : "CPU only - " + citadel);
        if (snapshot != null) {
            text.append("\nDriver: ").append(snapshot.renderer()).append(" / ").append(snapshot.version())
                    .append(" / GLSL ").append(snapshot.glslVersion());
        }
        return text.toString();
    }

    /**
     * Diagnostic: read back every dispatch's vertex buffer and compare the GPU-written vertices word
     * for word with {@link HoleBatch#fillOnCpu}'s exact vanilla bytes for the same parts. Stalls the
     * pipeline once per dispatch, so it is for testing only.
     */
    public static void setVerify(boolean enabled) {
        verify = enabled;
    }

    public static String verifyStatus() {
        return "GPU entity verify " + (verify ? "ON" : "OFF") + ": vertices checked " + VERIFIED_VERTICES.get()
                + ", mismatching vertices " + VERIFY_MISMATCHES.get()
                + (firstMismatch == null ? "" : ", first: " + firstMismatch);
    }

    private static void verify(HoleBatch batch, ByteBuffer client, VertexFormat format) {
        int bytes = client.remaining();
        ByteBuffer gpu = org.lwjgl.system.MemoryUtil.memAlloc(bytes);
        ByteBuffer cpu = org.lwjgl.system.MemoryUtil.memAlloc(bytes);
        try {
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
            org.lwjgl.system.MemoryUtil.memFree(gpu);
            org.lwjgl.system.MemoryUtil.memFree(cpu);
        }
    }

    public static String stats() {
        return "parts on GPU " + PARTS_GPU.get() + ", vertices on GPU " + VERTICES_GPU.get() + ", dispatches "
                + DISPATCHES.get() + ", batches filled on CPU " + BATCHES_CPU_FILLED.get() + ", parts not eligible "
                + PARTS_NOT_ELIGIBLE.get() + ", late fills " + LATE_FILLS.get() + " (writes skipped out of range "
                + HoleBatch.FILL_WRITES_SKIPPED.get() + "), fills outside the upload "
                + UNARMED_FILLS.get() + ", small batches filled on CPU " + SMALL_FILLS.get() + ", sorted item batches "
                + SORTED_ITEM_BATCHES.get() + " (sort position mismatches " + SORT_POSITION_MISMATCHES.get() + "), model sort fills "
                + SORT_FILLS_HINTED.get() + " hinted / " + SORT_FILLS_UNHINTED.get() + " never hinted" + ", ineffective pauses " + PAUSES.get() + ", Oculus segments parked "
                + PARKED_BATCHES.get() + " (dropped " + PARKED_DROPPED.get() + "), upload bytes skipped "
                + (BYTES_NOT_UPLOADED.get() >> 20) + " MiB, GeckoLib cubes on GPU " + GECKO_CUBES_GPU.get()
                + " (not eligible " + GECKO_NOT_ELIGIBLE.get() + ", meshes recaptured " + GECKO_MESH_CHANGED.get() + ")"
                + ", Citadel parts on GPU " + CITADEL_PARTS_GPU.get() + " (not eligible " + CITADEL_NOT_ELIGIBLE.get() + ")"
                + ", particles on GPU " + PARTICLES_GPU.get() + ", meshes shared " + MESHES_SHARED.get() + ", Oculus extended draws " + IRIS_DISPATCHES.get()
                + ", " + GpuItems.stats() + ", " + GpuBlockModels.stats()
                + (backend == null ? "" : ", arena " + backend.arenaVertices() + " vertices / "
                + (backend.arenaCapacity() >> 10) + " KiB");
    }

    public static void resetStats() {
        PARTS_GPU.set(0);
        VERTICES_GPU.set(0);
        DISPATCHES.set(0);
        BATCHES_CPU_FILLED.set(0);
        PARTS_NOT_ELIGIBLE.set(0);
        LATE_FILLS.set(0);
        UNARMED_FILLS.set(0);
        SMALL_FILLS.set(0);
        SORTED_ITEM_BATCHES.set(0);
        SORT_FILLS_HINTED.set(0);
        SORT_FILLS_UNHINTED.set(0);
        SORT_POSITION_MISMATCHES.set(0);
        PAUSES.set(0);
        BYTES_NOT_UPLOADED.set(0);
        PARKED_BATCHES.set(0);
        PARKED_DROPPED.set(0);
        VERIFIED_VERTICES.set(0);
        VERIFY_MISMATCHES.set(0);
        GECKO_CUBES_GPU.set(0);
        GECKO_NOT_ELIGIBLE.set(0);
        GECKO_MESH_CHANGED.set(0);
        CITADEL_PARTS_GPU.set(0);
        CITADEL_NOT_ELIGIBLE.set(0);
        PARTICLES_GPU.set(0);
        IRIS_DISPATCHES.set(0);
        GpuItems.resetStats();
        GpuBlockModels.resetStats();
        firstMismatch = null;
    }

    /** Re-runs the self-test with a new seed (render thread). */
    public static String selfTest(long seed) {
        if (backend == null) return "not available: " + reason;
        try {
            String result = GpuEntitySelfTest.run(backend, seed, 2048);
            restoreProgram();
            if (result != null) fail("self-test mismatch", new IllegalStateException(result));
            String particles = backend.particlesAvailable() ? GpuParticleSelfTest.run(backend, seed, 4096) : "unavailable";
            restoreProgram();
            if (particles != null && backend.particlesAvailable()) {
                particlesActive = false;
                particleReason = "CPU only - self-test: " + particles;
            }
            String iris = backend.irisAvailable() ? GpuEntitySelfTest.run(backend, seed, 2048, true) : "unavailable";
            restoreProgram();
            if (iris != null && irisFormat != null) disableIris("self-test: " + iris);
            return (result == null ? "PASS (2048 instances, seed " + seed + ")" : "FAIL: " + result)
                    + "; particles " + (particles == null ? "PASS (4096)" : "FAIL: " + particles)
                    + "; Oculus extended " + (iris == null ? "PASS (2048)" : "FAIL: " + iris);
        } catch (Throwable failure) {
            restoreProgram();
            fail("self-test failed", failure);
            return "FAIL: " + failure;
        }
    }
}
