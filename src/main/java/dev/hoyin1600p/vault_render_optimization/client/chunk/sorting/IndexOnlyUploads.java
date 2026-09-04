/* SPDX-License-Identifier: LGPL-3.0-only
 * Fresh VRO index-only upload path for Embeddium 1.18.2's arena/state contracts.
 * See THIRD_PARTY_NOTICES.md for behavioral provenance.
 */
package dev.hoyin1600p.vault_render_optimization.client.chunk.sorting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferSegment;
import me.jellysquid.mods.sodium.client.gl.arena.PendingUpload;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;

public final class IndexOnlyUploads {
    private IndexOnlyUploads() { }

    /** Preserve batch ordering: a full rebuild preceding a sort invalidates that sort's snapshot. */
    public static void upload(CommandList commands, RenderRegion region, List<ChunkBuildResult> results,
            Consumer<List<ChunkBuildResult>> nativeUpload) {
        if (results.stream().noneMatch(IndexOnlySortResult.class::isInstance)) {
            nativeUpload.accept(results);
            return;
        }
        List<ChunkBuildResult> normal = new ArrayList<>();
        List<IndexOnlySortResult> sorts = new ArrayList<>();
        Map<RenderSection, Integer> processedFrames = new LinkedHashMap<>();
        for (ChunkBuildResult result : results) {
            if (!(result instanceof IndexOnlySortResult indices)) {
                if (!sorts.isEmpty()) {
                    applyBatch(commands, region, sorts, processedFrames);
                    sorts.clear();
                }
                normal.add(result);
                continue;
            }
            if (!normal.isEmpty()) {
                nativeUpload.accept(normal);
                normal.forEach(item -> processedFrames.merge(item.render, item.buildTime, Math::max));
                normal.clear();
            }
            sorts.add(indices);
        }
        if (!sorts.isEmpty()) applyBatch(commands, region, sorts, processedFrames);
        if (!normal.isEmpty()) nativeUpload.accept(normal);
    }

    private static boolean isCurrent(IndexOnlySortResult result, RenderRegion region) {
        boolean accepted = result.render.canAcceptBuildResults(result)
                && result.render.getRegion() == region && region.getArenas() != null;
        for (var entry : result.indices.entrySet()) {
            ChunkGraphicsState state = result.render.getGraphicsState(entry.getKey());
            var generation = entry.getValue().generation();
            if (!SortBufferViews.sameGeneration(accepted, generation,
                    state == null ? null : state.getTranslucencyData())) return false;
            if (state.getVertexSegment().getLength() != generation.vertexBuffer().capacity()
                    || state.getIndexSegment().getLength() != entry.getValue().buffer().getLength()) return false;
        }
        return accepted && !result.indices.isEmpty();
    }

    private static void applyBatch(CommandList commands, RenderRegion region, List<IndexOnlySortResult> results,
            Map<RenderSection, Integer> processedFrames) {
        Map<RenderSection, IndexOnlySortResult> latest = new LinkedHashMap<>();
        List<PendingIndex> pending = new ArrayList<>();
        try {
            for (IndexOnlySortResult result : results) {
                IndexOnlySortResult previous = latest.get(result.render);
                if (!isCurrent(result, region)
                        || result.buildTime <= processedFrames.getOrDefault(result.render, Integer.MIN_VALUE)
                        || previous != null && result.buildTime <= previous.buildTime) {
                    IndexSortState.stale.increment();
                    continue;
                }
                if (previous != null) IndexSortState.stale.increment();
                latest.put(result.render, result);
            }
            for (IndexOnlySortResult result : latest.values()) {
                result.indices.forEach((pass, output) -> pending.add(new PendingIndex(result, pass, output)));
            }
            if (pending.isEmpty()) return;
            var arenas = region.getArenas();
            // One index arena submission per uninterrupted sort run, not one flush per section.
            if (arenas.indexBuffers.upload(commands, pending.stream().map(item -> item.upload))) {
                arenas.deleteTessellations(commands);
            }
            for (PendingIndex item : pending) {
                ChunkGraphicsState old = item.result.render.getGraphicsState(item.pass);
                IndexSegmentCommit.replace(old.getVertexSegment(), old.getIndexSegment(),
                    item.upload::getResult,
                    (vertices, indices) -> {
                        // Constructor reads only parts. No fake/empty vertex allocation is needed.
                        ChunkGraphicsState replacement = new ChunkGraphicsState(vertices, indices,
                                new ChunkMeshData(null, item.output.generation().parts()));
                        replacement.setTranslucencyData(item.output.generation());
                        item.result.render.setGraphicsState(item.pass, replacement);
                        item.transferred = true;
                    }, segment -> {
                        if (segment == item.upload.getResult()) item.installFailed = true;
                        segment.delete();
                    });
                IndexSortState.vertexUploadBytesAvoided.add(item.output.generation().vertexBuffer().capacity());
            }
            for (IndexOnlySortResult result : latest.values()) {
                result.applied = true;
                processedFrames.put(result.render, result.buildTime);
                IndexSortState.applied.increment();
            }
        } finally {
            for (PendingIndex item : pending) {
                if (!item.transferred) {
                    GlBufferSegment allocated = null;
                    try { allocated = item.upload.getResult(); } catch (IllegalStateException absent) { /* no allocation */ }
                    if (allocated != null && !item.installFailed) allocated.delete();
                }
            }
            // Uploaded bytes are copied into staging before upload returns. Outer delete is idempotent.
            results.forEach(IndexOnlySortResult::delete);
        }
    }

    private static final class PendingIndex {
        final IndexOnlySortResult result;
        final BlockRenderPass pass;
        final IndexOnlySortResult.Indices output;
        final PendingUpload upload;
        boolean transferred;
        boolean installFailed;

        PendingIndex(IndexOnlySortResult result, BlockRenderPass pass, IndexOnlySortResult.Indices output) {
            this.result = result;
            this.pass = pass;
            this.output = output;
            this.upload = new PendingUpload(output.buffer());
        }
    }
}
