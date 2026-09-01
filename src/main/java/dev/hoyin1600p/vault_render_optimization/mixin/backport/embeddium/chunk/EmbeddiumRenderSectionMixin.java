/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Behavior adapted from HoYin1600p's Embeddium stability fork, commit a8cebc3a.
 */
package dev.hoyin1600p.vault_render_optimization.mixin.backport.embeddium.chunk;

import dev.hoyin1600p.vault_render_optimization.renderertransfer.ChunkRebuildDeduplicationPolicy;
import dev.hoyin1600p.vault_render_optimization.renderertransfer.RenderSectionTransferAccess;
import java.lang.ref.WeakReference;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkUpdateType;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RenderSection.class, remap = false)
public abstract class EmbeddiumRenderSectionMixin implements RenderSectionTransferAccess {
    @Shadow private WeakReference<?> rebuildTask;
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
        Object task = this.rebuildTask == null ? null : this.rebuildTask.get();
        if (task == null) {
            return false;
        }
        EmbeddiumWrappedTaskAccess access = (EmbeddiumWrappedTaskAccess) task;
        return ChunkRebuildDeduplicationPolicy.hasActiveTask(
                true, access.vro$isCancelled(), access.vro$getFuture().isDone()
        );
    }
}
