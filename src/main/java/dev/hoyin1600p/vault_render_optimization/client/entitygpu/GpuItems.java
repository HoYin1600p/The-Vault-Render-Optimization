package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;
import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import dev.hoyin1600p.vault_render_optimization.mixin.entitygpu.ItemRendererGpuAccessor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;

/**
 * Items on the GPU. {@code ItemRenderer.renderQuadList} writes one side's quads of an item model; for an
 * eligible list VRO reserves its vertices and the compute shader writes exactly what the installed writer
 * would (Forge's {@code putBulkData}, or Embeddium's overwrite of {@code renderQuadList}; see
 * {@link ItemReference}). Only plain {@code BufferBuilder}s on unsorted render types qualify, which in practice
 * means solid and cutout block items; flat items, translucent and glinting items stay on the CPU.
 *
 * <p>Meshes are cached per quad list (models built per stack, such as Vault's gear wrappers, still hand out the
 * same baked lists), validated by size and quad identity on every use. Tints are looked up per tinted quad, as
 * the writers do, and consecutive quads with the same tint share one instance record.
 */
public final class GpuItems {
    private static final class Entry {
        final int generation;
        final BakedQuad[] quads;
        final ItemMesh mesh;
        final int first;

        Entry(int generation, BakedQuad[] quads, ItemMesh mesh, int first) {
            this.generation = generation;
            this.quads = quads;
            this.mesh = mesh;
            this.first = first;
        }

        boolean gpu() {
            return first >= 0;
        }

        BakedQuad[] quads() {
            return quads;
        }

        ItemMesh mesh() {
            return mesh;
        }

        int first() {
            return first;
        }
    }

    /** Bounded: cleared when full; lists are keyed by identity (BakedQuad lists do not define equality usefully). */
    private static final int CACHE_LIMIT = 8192;
    private static final Map<List<BakedQuad>, Entry> CACHE = new IdentityHashMap<>();


    private static final float[] POSE = new float[16];
    private static final float[] NORMAL = new float[9];
    private static final FloatBuffer POSE_BUFFER = FloatBuffer.wrap(POSE);
    private static final FloatBuffer NORMAL_BUFFER = FloatBuffer.wrap(NORMAL);
    private static int[] tints = new int[64];

    public static final AtomicLong LISTS_GPU = new AtomicLong();
    public static final AtomicLong QUADS_GPU = new AtomicLong();
    public static final AtomicLong LISTS_NOT_ELIGIBLE = new AtomicLong();
    public static final AtomicLong ORACLE_VERTICES = new AtomicLong();
    public static final AtomicLong ORACLE_MISMATCHES = new AtomicLong();
    private static volatile String firstOracleMismatch;

    private static boolean oracleRunning;
    /** Set while ItemColors providers run: a nested item render from inside one stays on the CPU (tints is shared). */
    private static boolean tinting;
    private static BufferBuilder oracleBuffer;

    private GpuItems() {
    }

    /** @return null when items may use the GPU (audit passed), otherwise why not */
    public static String blocker() {
        return GpuEntityAudit.itemBlocker();
    }

    /** The writer the audited {@code renderQuadList} runs (Forge or Embeddium), or null. */
    public static ItemWriter writer() {
        return GpuEntityAudit.itemWriter();
    }

    /**
     * @return true when the list's vertices were reserved and {@code renderQuadList} must not run
     */
    public static boolean tryReserve(ItemRenderer renderer, PoseStack poseStack, VertexConsumer consumer,
                                     List<BakedQuad> quads, ItemStack stack, int light, int overlay) {
        if (oracleRunning || tinting || quads.isEmpty()) return false;
        ItemWriter active = writer();
        if (active == null || !ClientOptimizationConfig.gpuItems || !GpuEntityModels.itemFrameActive()
                || !RenderSystem.isOnRenderThread() || consumer.getClass() != BufferBuilder.class) {
            return false;
        }
        GpuHoleBuilder builder = (GpuHoleBuilder) consumer;
        if (!builder.vro$canReserveItem()) return false;
        if (builder.vro$format() != DefaultVertexFormat.NEW_ENTITY && !GpuEntityModels.itemsIrisActive()) return false;
        Entry entry = entry(quads, active);
        if (!entry.gpu()) {
            LISTS_NOT_ELIGIBLE.incrementAndGet();
            return false;
        }
        int count = entry.quads().length;
        if (tints.length < count) tints = new int[Math.max(count, tints.length * 2)];
        // The writers look the tint up per tinted quad (vanilla: flag && quad.isTinted(); Embeddium: provider
        // non-null && quad.isTinted()); both give -1 when the stack is empty or the quad is untinted.
        boolean hasStack = !stack.isEmpty();
        var colors = ((ItemRendererGpuAccessor) renderer).vro$itemColors();
        tinting = true;
        try {
            for (int q = 0; q < count; q++) {
                BakedQuad quad = entry.quads()[q];
                tints[q] = hasStack && quad.isTinted() ? abgr(colors.getColor(stack, quad.getTintIndex())) : -1;
            }
        } finally {
            tinting = false;
        }
        PoseStack.Pose pose = poseStack.last();
        pose.pose().store(POSE_BUFFER);
        pose.normal().store(NORMAL_BUFFER);
        if (GpuEntityModels.verifying()) oracle(renderer, poseStack, quads, stack, light, overlay, entry, builder);
        boolean embeddium = active == ItemWriter.EMBEDDIUM;
        int start = 0;
        while (start < count) {
            int end = start + 1;
            while (end < count && tints[end] == tints[start]) end++;
            if (!builder.vro$reserveItem(entry.mesh(), entry.first(), start * 4, (end - start) * 4, POSE, NORMAL,
                    tints[start], overlay, light, embeddium)) {
                // Only the first run can be refused (the batch holds another kind); nothing was reserved yet.
                if (start == 0) {
                    LISTS_NOT_ELIGIBLE.incrementAndGet();
                    return false;
                }
                throw new IllegalStateException("GPU items: a batch refused a later run of the same list");
            }
            start = end;
        }
        LISTS_GPU.incrementAndGet();
        QUADS_GPU.addAndGet(count);
        return true;
    }

    /** {@code ItemColors} ARGB to the ABGR word the writers pack (tint alpha is ignored, written as 255). */
    static int abgr(int argb) {
        return argb == -1 ? -1 : ((argb >> 16) & 255) | (argb & 0xFF00) | ((argb & 255) << 16) | 0xFF000000;
    }

    private static Entry entry(List<BakedQuad> quads, ItemWriter active) {
        Entry entry = CACHE.get(quads);
        int generation = GpuEntityModels.generation();
        // Every use: a dynamic model can refill one list with different quads between two stacks in a frame.
        if (entry != null && entry.generation == generation && same(entry.quads, quads)) return entry;
        // Capture from the snapshot the entry keeps, so its quads and its mesh always agree.
        BakedQuad[] snapshot = quads.toArray(new BakedQuad[0]);
        ItemMesh mesh = ItemMeshCapture.capture(Arrays.asList(snapshot), active);
        int first = mesh == null ? -1 : GpuEntityModels.uploadItemMesh(mesh);
        entry = new Entry(generation, snapshot, mesh, first);
        if (CACHE.size() >= CACHE_LIMIT) CACHE.clear();
        CACHE.put(quads, entry);
        return entry;
    }

    private static boolean same(BakedQuad[] cached, List<BakedQuad> live) {
        if (cached.length != live.size()) return false;
        for (int i = 0; i < cached.length; i++) {
            if (cached[i] != live.get(i)) return false;
        }
        return true;
    }

    /** Resource reload (with the arena reset). */
    public static void reset() {
        CACHE.clear();
    }

    // ---- verify: the installed writer is the oracle -----------------------------------------------

    /**
     * Writes the same list through the installed writer into a scratch builder begun exactly like the target
     * (Oculus extends it too while a shader pack is active) and compares it with {@link ItemReference}.
     */
    private static void oracle(ItemRenderer renderer, PoseStack poseStack, List<BakedQuad> quads, ItemStack stack,
                               int light, int overlay, Entry entry, GpuHoleBuilder target) {
        try {
            if (oracleBuffer == null) oracleBuffer = new BufferBuilder(1 << 16);
            BufferBuilder scratch = oracleBuffer;
            scratch.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
            VertexFormat format = ((GpuHoleBuilder) scratch).vro$format();
            boolean iris = format != DefaultVertexFormat.NEW_ENTITY;
            int ids = iris ? OculusExtendedEntity.idsWord() : 0;
            int item = iris ? OculusExtendedEntity.itemWord() : 0;
            oracleRunning = true;
            try {
                ((ItemRendererGpuAccessor) renderer).vro$renderQuadList(poseStack, scratch, quads, stack, light, overlay);
            } finally {
                oracleRunning = false;
            }
            scratch.end();
            Pair<BufferBuilder.DrawState, ByteBuffer> popped = scratch.popNextBuffer();
            ByteBuffer actual = popped.getSecond().order(ByteOrder.LITTLE_ENDIAN);
            if (iris && format != GpuEntityModels.irisFormat()) {
                mismatch("scratch builder format " + format + " is neither NEW_ENTITY nor Oculus' entity format");
                return;
            }
            int vertices = entry.mesh().vertexCount();
            boolean embeddium = writer() == ItemWriter.EMBEDDIUM;
            int[] nine = new int[vertices * EntityVertexPacking.WORDS_PER_VERTEX];
            int count = entry.quads().length;
            for (int start = 0; start < count; ) {
                int end = start + 1;
                while (end < count && tints[end] == tints[start]) end++;
                ItemReference.expand(entry.mesh(), start * 4, (end - start) * 4, POSE, NORMAL, tints[start], overlay,
                        light, embeddium, nine, start * 4 * EntityVertexPacking.WORDS_PER_VERTEX);
                start = end;
            }
            int[] expected = nine;
            int stride = EntityVertexPacking.WORDS_PER_VERTEX;
            if (iris) {
                stride = IrisEntityExtension.WORDS_PER_VERTEX;
                expected = new int[vertices * stride];
                ItemReference.extendIris(nine, 0, vertices, ids, item, embeddium, expected, 0);
            }
            if (actual.remaining() != expected.length * 4) {
                mismatch("installed writer wrote " + actual.remaining() + " bytes, reference " + expected.length * 4);
                return;
            }
            // Padding the writers never write (NEW_ENTITY byte 35 for Forge, Oculus bytes 54-55) is masked.
            int padWord = stride - 1;
            int padMask = iris ? 0x0000FFFF : 0x00FFFFFF;
            for (int v = 0; v < vertices; v++) {
                boolean same = true;
                for (int w = 0; w < stride; w++) {
                    int mask = w == padWord ? padMask : -1;
                    int got = actual.getInt((v * stride + w) * 4);
                    if ((got & mask) != (expected[v * stride + w] & mask)) {
                        same = false;
                        mismatch("vertex " + v + " word " + w + ": writer 0x" + Integer.toHexString(got)
                                + " reference 0x" + Integer.toHexString(expected[v * stride + w]) + " (" + writer()
                                + (iris ? ", Oculus" : "") + ")");
                        break;
                    }
                }
                ORACLE_VERTICES.incrementAndGet();
                if (!same) ORACLE_MISMATCHES.incrementAndGet();
            }
        } catch (Throwable failure) {
            mismatch("oracle failed: " + failure);
        }
    }

    private static void mismatch(String what) {
        if (firstOracleMismatch == null) {
            firstOracleMismatch = what;
            VaultRenderOptimization.LOGGER.warn("GPU items oracle mismatch: {}", what);
        }
    }

    public static String status() {
        String blocker = blocker();
        return "Items: " + (!ClientOptimizationConfig.gpuItems ? "OFF (config render_fast_paths.gpu_items)"
                : blocker != null ? "CPU only - " + blocker : "on (" + writer() + " writer)");
    }

    public static String stats() {
        return "item lists on GPU " + LISTS_GPU.get() + " (" + QUADS_GPU.get() + " quads, not eligible "
                + LISTS_NOT_ELIGIBLE.get() + ", cached lists " + CACHE.size() + "), item oracle vertices "
                + ORACLE_VERTICES.get() + " mismatching " + ORACLE_MISMATCHES.get()
                + (firstOracleMismatch == null ? "" : " (first: " + firstOracleMismatch + ")");
    }

    public static void resetStats() {
        LISTS_GPU.set(0);
        QUADS_GPU.set(0);
        LISTS_NOT_ELIGIBLE.set(0);
        ORACLE_VERTICES.set(0);
        ORACLE_MISMATCHES.set(0);
        firstOracleMismatch = null;
    }
}
