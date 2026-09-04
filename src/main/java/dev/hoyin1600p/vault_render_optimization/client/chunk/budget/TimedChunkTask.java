/* SPDX-License-Identifier: LGPL-3.0-only
 * Fresh VRO adapter to Embeddium's task lifecycle; see THIRD_PARTY_NOTICES.md.
 */
package dev.hoyin1600p.vault_render_optimization.client.chunk.budget;

import me.jellysquid.mods.sodium.client.gl.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderBuildTask;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;

/** Original cancellation source, result and resource lifecycle pass through unchanged. */
public final class TimedChunkTask extends ChunkRenderBuildTask {
    private final ChunkRenderBuildTask delegate;
    private final AdaptiveChunkBudget controller;
    private final boolean sort;
    public TimedChunkTask(ChunkRenderBuildTask delegate, AdaptiveChunkBudget controller, boolean sort) {
        this.delegate = delegate;
        this.controller = controller;
        this.sort = sort;
    }
    @Override public ChunkBuildResult performBuild(ChunkBuildContext context, CancellationSource cancellation) {
        long start = System.nanoTime();
        ChunkBuildResult result = delegate.performBuild(context, cancellation);
        if (result != null) controller.observeBuild(sort, System.nanoTime() - start, BudgetResults.bytes(result));
        return result;
    }
    @Override public void releaseResources() { delegate.releaseResources(); }
}
