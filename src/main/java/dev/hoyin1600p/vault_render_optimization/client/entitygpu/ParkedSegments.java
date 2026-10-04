package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import static dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuEntityModels.*;

/** Oculus batched entity rendering: reserved batches parked by slice address until their stand-in upload. */
final class ParkedSegments {
    private ParkedSegments() {
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
    record Parked(HoleBatch batch, Object owner, int vertexCount, VertexFormat format, ByteBuffer slice) {
    }

    static int segmentPopDepth;
    static final Long2ObjectOpenHashMap<Parked> PARKED =
            new Long2ObjectOpenHashMap<>();

    /** Oculus SegmentedBufferBuilder.getSegments HEAD/RETURN. */
    public static void beginSegmentPops() {
        if (RenderSystem.isOnRenderThread()) segmentPopDepth++;
    }

    public static void endSegmentPops() {
        if (segmentPopDepth > 0 && RenderSystem.isOnRenderThread()) segmentPopDepth--;
    }

    /**
     * {@code BufferBuilder.begin} HEAD (and ImmediatelyFast's release of a pooled builder's memory): memory
     * this builder handed out is about to be reused or freed, so its parked batches are dropped unfilled.
     */
    public static void builderBegins(Object owner) {
        if (PARKED.isEmpty() || !RenderSystem.isOnRenderThread()) return;
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

    /**
     * Frame start: batches still parked were never drawn. They are released unfilled: their memory may
     * already be freed by its owner, and writing into it would be worse than a missed draw.
     */
    static void dropAllParked() {
        if (PARKED.isEmpty()) return;
        for (Parked parked : PARKED.values()) {
            PARKED_DROPPED.incrementAndGet();
            release(parked.batch());
        }
        PARKED.clear();
    }

    /** Distinct late-fill reasons logged so far (capped), and the latest one for {@code /vro gpuentity stats}. */
    static final Set<String> LATE_FILL_REASONS = ConcurrentHashMap.newKeySet();
    static volatile String lastLateFill = "none";

    /**
     * Why a parked Oculus segment was filled on the CPU instead of going to the GPU: which of the three
     * conditions failed, both vertex counts and formats, the batch kind, the builder and both byte sizes.
     */
    static String lateFillReason(State current, int parkedVertices, int uploadVertices, VertexFormat parkedFormat,
                                 VertexFormat uploadFormat, HoleBatch.Kind kind, Object owner, int parkedBytes,
                                 int uploadBytes) {
        StringBuilder why = new StringBuilder();
        if (current != State.ACTIVE) why.append("GPU path ").append(current).append("; ");
        if (parkedVertices != uploadVertices) {
            why.append("vertex count ").append(parkedVertices).append(" parked vs ").append(uploadVertices)
                    .append(" uploaded; ");
        }
        if (parkedFormat != uploadFormat) {
            why.append("format ").append(formatName(parkedFormat)).append(" parked vs ").append(formatName(uploadFormat))
                    .append(" uploaded; ");
        }
        return why.append(kind).append(" batch, builder ")
                .append(owner == null ? "unknown" : owner.getClass().getName())
                .append(", ").append(parkedBytes).append(" bytes parked vs ").append(uploadBytes).append(" uploaded")
                .toString();
    }

    static String formatName(VertexFormat format) {
        if (format == DefaultVertexFormat.NEW_ENTITY) return "NEW_ENTITY";
        if (format == DefaultVertexFormat.PARTICLE) return "PARTICLE";
        if (format != null && format == irisFormat) return "OCULUS_ENTITY";
        return format == null ? "none" : format.getElements().size() + "-element format";
    }

    static void noteLateFill(String reason) {
        lastLateFill = reason;
        // Log each distinct reason once (at most 8), so a report shows why without flooding the log.
        if (LATE_FILL_REASONS.size() < 8 && LATE_FILL_REASONS.add(reason)) {
            VaultRenderOptimization.LOGGER.warn("GPU entity models: parked Oculus segment filled on the CPU ({})", reason);
        }
    }
}
