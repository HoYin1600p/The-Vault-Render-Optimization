/* SPDX-License-Identifier: LGPL-3.0-only
 * VRO-owned adaptation of the Embeddium 1.18.2 sort-result contract.
 * See THIRD_PARTY_NOTICES.md, "Index-only terrain transparency sorting".
 */
package dev.hoyin1600p.vault_render_optimization.client.chunk.sorting;

import java.util.Map;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBufferSorter.SortBuffer;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;

public final class IndexOnlySortResult extends ChunkBuildResult {
    public record Indices(SortBuffer generation, NativeBuffer buffer) { }

    public final Map<BlockRenderPass, Indices> indices;
    public boolean applied;
    private boolean deleted;

    public IndexOnlySortResult(RenderSection section, int frame, Map<BlockRenderPass, Indices> indices) {
        super(section, null, Map.of(), frame);
        this.indices = Map.copyOf(indices);
        setPartialUpload(true);
    }

    @Override
    public void delete() {
        if (!deleted) {
            deleted = true;
            indices.values().forEach(value -> value.buffer().free());
        }
    }
}
