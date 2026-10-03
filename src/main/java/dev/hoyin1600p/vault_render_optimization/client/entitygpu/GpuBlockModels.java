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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.IModelData;

/**
 * Whole block models drawn by block entity renderers ({@code ModelBlockRenderer.renderModel}: vault portals,
 * spawners, crate crackers and so on) on the GPU, through the item program. Both writers ignore the quads' baked
 * colour and write {@code tinted ? clamp(r, g, b) : white} with alpha 255, and Embeddium's overwrite writes the raw
 * light, so a mesh captured with {@link ItemMeshCapture} and its baked colour forced to white (and, for Embeddium,
 * its baked light to 0) makes {@link ItemReference} and the item shader produce exactly their bytes; the normal and
 * Oculus rules are the item ones.
 *
 * <p>The writers seed the model's random with 42 per side but use different generators (Forge
 * {@code java.util.Random}, Embeddium {@code XoRoShiRoRandom}), so only models whose quad lists do not depend on the
 * random are eligible; that is checked once per model when it is captured.
 */
public final class GpuBlockModels {
    private static final Direction[] SIDES = Direction.values();

    private static final class Entry {
        final int generation;
        final List<?>[] lists = new List<?>[7];
        final BakedQuad[] quads;
        final ItemMesh mesh;
        final int first;
        int validatedFrame;

        Entry(int generation, List<?>[] lists, BakedQuad[] quads, ItemMesh mesh, int first) {
            this.generation = generation;
            System.arraycopy(lists, 0, this.lists, 0, 7);
            this.quads = quads;
            this.mesh = mesh;
            this.first = first;
            this.validatedFrame = GpuEntityModels.frameIndex();
        }

        boolean gpu() {
            return first >= 0;
        }
    }

    private static final int CACHE_LIMIT = 4096;
    private static final Map<BakedModel, Entry> CACHE = new IdentityHashMap<>();
    private static final Random RANDOM = new Random();
    private static final Random OTHER = new Random();
    private static final List<?>[] LISTS = new List<?>[7];
    private static final float[] POSE = new float[16];
    private static final float[] NORMAL = new float[9];
    private static final FloatBuffer POSE_BUFFER = FloatBuffer.wrap(POSE);
    private static final FloatBuffer NORMAL_BUFFER = FloatBuffer.wrap(NORMAL);
    private static int[] tints = new int[64];

    public static final AtomicLong MODELS_GPU = new AtomicLong();
    public static final AtomicLong MODELS_NOT_ELIGIBLE = new AtomicLong();
    public static final AtomicLong ORACLE_VERTICES = new AtomicLong();
    public static final AtomicLong ORACLE_MISMATCHES = new AtomicLong();
    private static volatile String firstOracleMismatch;
    private static boolean oracleRunning;
    private static BufferBuilder oracleBuffer;

    private GpuBlockModels() {
    }

    public static ItemWriter writer() {
        return GpuEntityAudit.blockModelWriter();
    }

    /** @return true when the model's vertices were reserved and {@code renderModel} must not run */
    public static boolean tryReserve(ModelBlockRenderer renderer, PoseStack.Pose pose, VertexConsumer consumer,
                                     BlockState state, BakedModel model, float red, float green, float blue, int light,
                                     int overlay, IModelData data) {
        if (oracleRunning) return false;
        ItemWriter active = writer();
        if (active == null || !ClientOptimizationConfig.gpuItems || !GpuEntityModels.itemFrameActive()
                || !RenderSystem.isOnRenderThread() || consumer.getClass() != BufferBuilder.class) {
            return false;
        }
        GpuHoleBuilder builder = (GpuHoleBuilder) consumer;
        if (!builder.vro$canReserveItem()) return false;
        if (builder.vro$format() != DefaultVertexFormat.NEW_ENTITY && !GpuEntityModels.itemsIrisActive()) return false;
        for (int s = 0; s < 7; s++) {
            RANDOM.setSeed(42L);
            LISTS[s] = model.getQuads(state, s < 6 ? SIDES[s] : null, RANDOM, data);
        }
        Entry entry = entry(model, state, data, active);
        if (entry == null || !entry.gpu()) {
            MODELS_NOT_ELIGIBLE.incrementAndGet();
            return false;
        }
        int count = entry.quads.length;
        if (count == 0) return false;
        // Both writers: tinted quads take (int)(clamp(c) * 255F) per lane with alpha 255, the others white.
        int tinted = Mth.clamp((int) (Mth.clamp(red, 0.0F, 1.0F) * 255.0F), 0, 255)
                | Mth.clamp((int) (Mth.clamp(green, 0.0F, 1.0F) * 255.0F), 0, 255) << 8
                | Mth.clamp((int) (Mth.clamp(blue, 0.0F, 1.0F) * 255.0F), 0, 255) << 16 | 0xFF000000;
        if (tints.length < count) tints = new int[Math.max(count, tints.length * 2)];
        for (int q = 0; q < count; q++) tints[q] = entry.quads[q].isTinted() ? tinted : -1;
        pose.pose().store(POSE_BUFFER);
        pose.normal().store(NORMAL_BUFFER);
        if (GpuEntityModels.verifying()) oracle(renderer, pose, state, model, red, green, blue, light, overlay, data, entry);
        boolean embeddium = active == ItemWriter.EMBEDDIUM;
        for (int start = 0; start < count; ) {
            int end = start + 1;
            while (end < count && tints[end] == tints[start]) end++;
            if (!builder.vro$reserveItem(entry.mesh, entry.first, start * 4, (end - start) * 4, POSE, NORMAL,
                    tints[start], overlay, light, embeddium)) {
                if (start == 0) {
                    MODELS_NOT_ELIGIBLE.incrementAndGet();
                    return false;
                }
                throw new IllegalStateException("GPU block models: a batch refused a later run of the same model");
            }
            start = end;
        }
        MODELS_GPU.incrementAndGet();
        return true;
    }

    /** The cached mesh for the quad lists in {@link #LISTS}, captured (and checked for randomness) on first use. */
    private static Entry entry(BakedModel model, BlockState state, IModelData data, ItemWriter active) {
        Entry entry = CACHE.get(model);
        int generation = GpuEntityModels.generation();
        int frame = GpuEntityModels.frameIndex();
        if (entry != null && entry.generation == generation) {
            if (entry.validatedFrame == frame && !GpuEntityModels.verifying()) return entry;
            boolean same = true;
            for (int s = 0; s < 7 && same; s++) same = entry.lists[s] == LISTS[s];
            if (same) {
                entry.validatedFrame = frame;
                return entry;
            }
        }
        // Random-dependent models (weighted, multipart with random parts) would differ between the writers'
        // generators: require every side's list to be the same object under another seed.
        List<BakedQuad> all = new ArrayList<>();
        boolean independent = true;
        for (int s = 0; s < 7; s++) {
            OTHER.setSeed(0x5EEDL + s);
            if (model.getQuads(state, s < 6 ? SIDES[s] : null, OTHER, data) != LISTS[s]) independent = false;
            @SuppressWarnings("unchecked") List<BakedQuad> list = (List<BakedQuad>) LISTS[s];
            all.addAll(list);
        }
        ItemMesh mesh = independent ? ItemMeshCapture.capture(all, active) : null;
        int first = -1;
        if (mesh != null) {
            int[] words = mesh.words();
            for (int v = 0; v < mesh.vertexCount(); v++) {
                int d = v * ItemMesh.WORDS_PER_VERTEX;
                words[d + 8] = -1;                                       // baked colour is not read
                if (active == ItemWriter.EMBEDDIUM) words[d + 9] = 0;    // Embeddium writes the raw light
            }
            first = GpuEntityModels.uploadItemMesh(mesh);
        }
        entry = new Entry(generation, LISTS, all.toArray(new BakedQuad[0]), mesh, first);
        if (CACHE.size() >= CACHE_LIMIT) CACHE.clear();
        CACHE.put(model, entry);
        return entry;
    }

    public static void reset() {
        CACHE.clear();
    }

    /** Verify mode: the installed writer into a scratch builder begun like the target, compared with the reference. */
    private static void oracle(ModelBlockRenderer renderer, PoseStack.Pose pose, BlockState state, BakedModel model,
                               float red, float green, float blue, int light, int overlay, IModelData data, Entry entry) {
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
                renderer.renderModel(pose, scratch, state, model, red, green, blue, light, overlay, data);
            } finally {
                oracleRunning = false;
            }
            scratch.end();
            Pair<BufferBuilder.DrawState, ByteBuffer> popped = scratch.popNextBuffer();
            ByteBuffer actual = popped.getSecond().order(ByteOrder.LITTLE_ENDIAN);
            int vertices = entry.mesh.vertexCount();
            boolean embeddium = writer() == ItemWriter.EMBEDDIUM;
            int[] nine = new int[vertices * EntityVertexPacking.WORDS_PER_VERTEX];
            int count = entry.quads.length;
            for (int start = 0; start < count; ) {
                int end = start + 1;
                while (end < count && tints[end] == tints[start]) end++;
                ItemReference.expand(entry.mesh, start * 4, (end - start) * 4, POSE, NORMAL, tints[start], overlay,
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
            int padWord = stride - 1;
            int padMask = iris ? 0x0000FFFF : 0x00FFFFFF;
            for (int v = 0; v < vertices; v++) {
                boolean same = true;
                for (int w = 0; w < stride; w++) {
                    int mask = w == padWord ? padMask : -1;
                    int got = actual.getInt((v * stride + w) * 4);
                    if ((got & mask) != (expected[v * stride + w] & mask)) {
                        same = false;
                        mismatch(model.getClass().getName() + " vertex " + v + " word " + w + ": writer 0x"
                                + Integer.toHexString(got) + " reference 0x" + Integer.toHexString(expected[v * stride + w])
                                + " (" + writer() + (iris ? ", Oculus" : "") + ")");
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
            VaultRenderOptimization.LOGGER.warn("GPU block models oracle mismatch: {}", what);
        }
    }

    public static String status() {
        String blocker = GpuEntityAudit.blockModelBlocker();
        return "Block models (renderModel): " + (blocker != null ? "CPU only - " + blocker : "on (" + writer() + " writer)");
    }

    public static String stats() {
        return "block models on GPU " + MODELS_GPU.get() + " (not eligible " + MODELS_NOT_ELIGIBLE.get() + ", cached "
                + CACHE.size() + "), block model oracle vertices " + ORACLE_VERTICES.get() + " mismatching "
                + ORACLE_MISMATCHES.get() + (firstOracleMismatch == null ? "" : " (first: " + firstOracleMismatch + ")");
    }

    public static void resetStats() {
        MODELS_GPU.set(0);
        MODELS_NOT_ELIGIBLE.set(0);
        ORACLE_VERTICES.set(0);
        ORACLE_MISMATCHES.set(0);
        firstOracleMismatch = null;
    }
}
