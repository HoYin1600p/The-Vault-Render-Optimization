/* SPDX-License-Identifier: LGPL-3.0-only
 * VRO-owned adaptation of Embeddium's ChunkRenderSortTask / SortBuffer contract.
 * The native sorter is called unchanged; no newer Sodium implementation is copied.
 */
package dev.hoyin1600p.vault_render_optimization.client.chunk.sorting;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;
import me.jellysquid.mods.sodium.client.gl.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBufferSorter;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBufferSorter.SortBuffer;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderBuildTask;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;

public final class IndexOnlySortTask extends ChunkRenderBuildTask {
    private final RenderSection section;
    private final int frame;
    private final float x, y, z;
    private final Map<BlockRenderPass, SortBuffer> snapshots;

    private IndexOnlySortTask(RenderSection section, int frame, float x, float y, float z,
            Map<BlockRenderPass, SortBuffer> snapshots) {
        this.section = section;
        this.frame = frame;
        this.x = x - section.getOriginX();
        this.y = y - section.getOriginY();
        this.z = z - section.getOriginZ();
        this.snapshots = snapshots;
    }

    /** Null leaves unknown snapshot shapes to the original renderer. No GL/worker state captured. */
    public static IndexOnlySortTask capture(RenderSection section, int frame, float x, float y, float z) {
        Map<BlockRenderPass, SortBuffer> snapshots = new EnumMap<>(BlockRenderPass.class);
        for (BlockRenderPass pass : BlockRenderPass.VALUES) {
            if (!pass.isTranslucent()) continue;
            SortBuffer data = section.getTranslucencyData(pass);
            if (data == null) continue;
            if (data.vertexBuffer().isDirect() || data.indexBuffer().isDirect()
                    || data.indexBuffer().capacity() == 0 || data.indexBuffer().capacity() % 12 != 0) {
                IndexSortState.fallback.increment();
                return null;
            }
            snapshots.put(pass, data);
        }
        if (snapshots.isEmpty()) return null;
        IndexSortState.scheduled.increment();
        return new IndexOnlySortTask(section, frame, x, y, z, snapshots);
    }

    @Override
    public ChunkBuildResult performBuild(ChunkBuildContext context, CancellationSource cancellation) {
        Map<BlockRenderPass, IndexOnlySortResult.Indices> outputs = new EnumMap<>(BlockRenderPass.class);
        boolean transferred = false;
        try {
            for (var entry : snapshots.entrySet()) {
                if (cancellation.isCancelled()) return null;
                SortBuffer source = entry.getValue();
                ByteBuffer indices = SortBufferViews.indices(source.indexBuffer());
                SortBuffer view = new SortBuffer(SortBufferViews.vertices(source.vertexBuffer()),
                        indices, source.vertexFormat(), source.parts());
                ChunkBufferSorter.sort(view, x, y, z);
                if (cancellation.isCancelled()) return null;
                NativeBuffer nativeIndices = new NativeBuffer(indices.capacity());
                try {
                    indices.clear();
                    nativeIndices.getDirectBuffer().put(indices);
                    outputs.put(entry.getKey(), new IndexOnlySortResult.Indices(source, nativeIndices));
                } catch (RuntimeException | Error failure) {
                    nativeIndices.free();
                    throw failure;
                }
                // Native path clones heap vertices, then copies them into a native result buffer.
                IndexSortState.vertexCopyBytesAvoided.add(2L * source.vertexBuffer().capacity());
            }
            IndexOnlySortResult result = new IndexOnlySortResult(section, frame, outputs);
            transferred = true;
            return result;
        } finally {
            if (!transferred) outputs.values().forEach(value -> value.buffer().free());
        }
    }

    @Override
    public void releaseResources() {
        // Only heap snapshots belong to the task. Native output belongs to its result.
        snapshots.clear();
    }
}
