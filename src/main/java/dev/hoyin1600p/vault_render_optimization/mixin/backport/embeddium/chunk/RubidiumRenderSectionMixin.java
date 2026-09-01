/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Rubidium 0.5.6 layout adaptation of Embeddium-fork behavior from a8cebc3a.
 */
package dev.hoyin1600p.vault_render_optimization.mixin.backport.embeddium.chunk;

import dev.hoyin1600p.vault_render_optimization.renderertransfer.ChunkRebuildDeduplicationPolicy;
import dev.hoyin1600p.vault_render_optimization.renderertransfer.RenderSectionTransferAccess;
import java.util.concurrent.CompletableFuture;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkUpdateType;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RenderSection.class, remap = false)
public abstract class RubidiumRenderSectionMixin implements RenderSectionTransferAccess {
    @Shadow private CompletableFuture<?> rebuildTask;
    @Shadow private ChunkUpdateType pendingUpdate;

    @Override
    public boolean vro$hasEquivalentPendingRebuild(boolean important) {
        return ChunkRebuildDeduplicationPolicy.hasEquivalentPending(
                important,
                this.pendingUpdate == ChunkUpdateType.REBUILD,
                this.pendingUpdate == ChunkUpdateType.IMPORTANT_REBUILD
        );
    }

    @Override
    public boolean vro$hasActiveBuildTask() {
        return ChunkRebuildDeduplicationPolicy.hasActiveTask(
                this.rebuildTask != null,
                this.rebuildTask != null && this.rebuildTask.isCancelled(),
                this.rebuildTask != null && this.rebuildTask.isDone()
        );
    }
}
