package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.lwjgl.opengl.GL20C;
import static dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuMeshCache.*;
import static dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuVerifier.*;
import static dev.hoyin1600p.vault_render_optimization.client.entitygpu.ParkedSegments.*;

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

    static State state = State.UNINITIALIZED;
    static String reason = "not initialized";
    static GpuEntityCapabilities.Snapshot snapshot;
    static GpuEntityBackend backend;
    static long maxDrawBytes;

    /** Read on every model part: true only while the whole path is usable for this frame. */
    static boolean frameActive;
    static boolean watchXaeroTracing;
    static int generation = 1;
    private static boolean resetRequested;
    static boolean arenaFullLogged;

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
    static volatile boolean particlesActive;
    /** Oculus extended entity vertices: the format when that program passed its self-test, else null. */
    static volatile VertexFormat irisFormat;
    static volatile String irisReason = "not initialized";
    /** Items: the self-tests of the item program without and with Oculus' extended format. */
    static volatile boolean itemsActive;
    static volatile boolean itemsIrisActive;
    static volatile String itemReason = "not initialized";

    /** Draws of Oculus extended entity vertices written by the GPU. */
    public static final AtomicLong IRIS_DISPATCHES = new AtomicLong();
    static volatile String particleReason = "not initialized";
    public static final AtomicLong CITADEL_NOT_ELIGIBLE = new AtomicLong();

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
    static boolean particlesOnlyFrame;

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
        // getSegments always returns within a frame; a depth left over (a lost RETURN hook or an exception
        // thrown out of getSegments) would otherwise park unrelated pops forever.
        segmentPopDepth = 0;
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

    static final Effectiveness effectiveness = new Effectiveness();

    private static String bufferSourceName() {
        try {
            return Minecraft.getInstance().renderBuffers().bufferSource().getClass().getName();
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

    static void fail(String why, Throwable failure) {
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
    static void forgetProgram() {
        dev.hoyin1600p.vault_render_optimization.mixin.entitygpu.ShaderInstanceGpuAccessor.vro$setLastProgramId(-1);
    }

    /** The next {@code ShaderInstance.apply()} must bind its own program again. */
    static void restoreProgram() {
        GL20C.glUseProgram(0);
        dev.hoyin1600p.vault_render_optimization.mixin.entitygpu.ShaderInstanceGpuAccessor.vro$setLastProgramId(-1);
    }

    /** Resource reload: meshes are recaptured into a fresh arena at the next frame. */
    public static void requestReset() {
        resetRequested = true;
    }

    // ---- model parts ----------------------------------------------------------------------------

    public static final AtomicLong MESHES_SHARED = new AtomicLong();

    /** The arena generation; cached meshes from an older generation must be uploaded again. */
    public static int generation() {
        return generation;
    }

    /** True while the live verifier runs (GeckoLib then re-checks cached meshes against live cubes). */
    public static boolean verifying() {
        return verify;
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
                               TextureAtlasSprite atlasSprite) {
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
    public static void hintSorting(VertexConsumer consumer, boolean sorting) {
        if (consumer instanceof GpuHoleBuilder builder) builder.vro$setSortingHint(sorting);
    }

    public static HoleBatch borrowBatch() {
        HoleBatch batch = POOL.poll();
        return batch != null ? batch : new HoleBatch();
    }

    public static void release(HoleBatch batch) {
        batch.reset();
        // The pool is render-thread state; a batch filled on another thread is simply not reused.
        if (POOL.size() < 64 && RenderSystem.isOnRenderThread()) POOL.push(batch);
    }

    // ---- upload hand-off ------------------------------------------------------------------------

    public static final AtomicLong PARKED_BATCHES = new AtomicLong();
    public static final AtomicLong PARKED_DROPPED = new AtomicLong();

    /** Increases at every frame start (GPU items revalidate cached quad lists once per frame). */
    public static int frameIndex() {
        return frameIndex;
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
    private static final Set<String> SORT_FILL_CALLERS = ConcurrentHashMap.newKeySet();

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

    // ---- status ---------------------------------------------------------------------------------

    public static State state() {
        return state;
    }

    // ---- delegates: the public entry points mixins call, implemented by the classes in this package ----

    /** Returns the part's mesh for this generation, capturing and uploading it on first use. */
    public static CachedMesh mesh(ModelPart part, CachedMesh cached) {
        return GpuMeshCache.mesh(part, cached);
    }

    /**
     * Uploads a captured mesh to the arena (only while the path is active on the render thread).
     *
     * @return its first arena vertex, 0 for an empty mesh, or -1 when the arena is full
     */
    public static int uploadMesh(ModelMesh mesh) {
        return GpuMeshCache.uploadMesh(mesh);
    }

    /**
     * Uploads a captured item mesh to the item arena.
     *
     * @return its first arena vertex, 0 for an empty mesh, or -1 when the arena is full
     */
    public static int uploadItemMesh(ItemMesh mesh) {
        return GpuMeshCache.uploadItemMesh(mesh);
    }

    /** {@code BufferUploader.end} HEAD: the next pop on this thread feeds the immediate upload. */
    public static void armUpload() {
        GpuUploadPath.armUpload();
    }

    /** {@code BufferUploader.end} RETURN. */
    public static void disarmUpload() {
        GpuUploadPath.disarmUpload();
    }

    /** {@code BufferBuilder.popNextBuffer} RETURN for a draw state that has holes. */
    public static void popped(HoleBatch batch, BufferBuilder.DrawState drawState, ByteBuffer slice, Object owner) {
        GpuUploadPath.popped(batch, drawState, slice, owner);
    }

    /** A pop without holes of its own (see {@link GpuUploadPath#poppedWithoutHoles}). */
    public static void poppedWithoutHoles(BufferBuilder.DrawState drawState, ByteBuffer slice) {
        GpuUploadPath.poppedWithoutHoles(drawState, slice);
    }

    /** Replaces {@code BufferUploader._end}'s vertex {@code glBufferData}. */
    public static void uploadVertices(int target, ByteBuffer buffer, int usage, VertexFormat format) {
        GpuUploadPath.uploadVertices(target, buffer, usage, format);
    }

    /** @return false when the holes are not in ascending order (never expected): upload everything */
    static boolean uploadGaps(int target, ByteBuffer buffer, int usage, HoleBatch batch) {
        return GpuUploadPath.uploadGaps(target, buffer, usage, batch);
    }

    /** {@code BufferUploader._end}, right after the vertex upload and before the draw. */
    public static void uploaded(ByteBuffer buffer, VertexFormat format) {
        GpuUploadPath.uploaded(buffer, format);
    }

    /** Oculus SegmentedBufferBuilder.getSegments HEAD. */
    public static void beginSegmentPops() {
        ParkedSegments.beginSegmentPops();
    }

    /** Oculus SegmentedBufferBuilder.getSegments RETURN. */
    public static void endSegmentPops() {
        ParkedSegments.endSegmentPops();
    }

    /** {@code BufferBuilder.begin} HEAD: this builder's parked batches are dropped unfilled. */
    public static void builderBegins(Object owner) {
        ParkedSegments.builderBegins(owner);
    }

    static String lateFillReason(State current, int parkedVertices, int uploadVertices, VertexFormat parkedFormat,
                                 VertexFormat uploadFormat, HoleBatch.Kind kind, Object owner, int parkedBytes,
                                 int uploadBytes) {
        return ParkedSegments.lateFillReason(current, parkedVertices, uploadVertices, parkedFormat, uploadFormat,
                kind, owner, parkedBytes, uploadBytes);
    }

    public static String status() {
        return GpuStats.status();
    }

    public static String stats() {
        return GpuStats.stats();
    }

    public static void resetStats() {
        GpuStats.resetStats();
    }

    /**
     * Diagnostic: read back every dispatch's vertex buffer and compare the GPU-written vertices word
     * for word with {@link HoleBatch#fillOnCpu}'s exact vanilla bytes for the same parts.
     */
    public static void setVerify(boolean enabled) {
        GpuVerifier.setVerify(enabled);
    }

    public static String verifyStatus() {
        return GpuVerifier.verifyStatus();
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
