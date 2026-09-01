/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Behavior adapted from HoYin1600p's Embeddium stability fork, commit a8cebc3a.
 */
package dev.hoyin1600p.vault_render_optimization.mixin.backport.embeddium.chunk;

import dev.hoyin1600p.vault_render_optimization.renderertransfer.RenderSectionTransferAccess;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkUpdateType;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderBuildTask;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class RenderSectionManagerMixin {
    @Shadow @Final private Long2ReferenceMap<RenderSection> sections;
    @Shadow private boolean needsUpdate;
    @Shadow public abstract boolean isChunkPrioritized(RenderSection section);
    @Shadow public abstract ChunkRenderBuildTask createRebuildTask(RenderSection section);

    @Inject(
            method = "scheduleRebuild",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/world/cloned/ClonedChunkSectionCache;invalidate(III)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true,
            require = 1
    )
    private void vro$coalesceEquivalentPendingRebuild(
            int x, int y, int z, boolean important, CallbackInfo callback
    ) {
        RenderSection section = this.sections.get(SectionPos.asLong(x, y, z));
        if (section == null || !section.isBuilt()) {
            return;
        }

        // Treat a nearby request as potentially important even on renderer
        // builds that promote it later, so VRO never suppresses an upgrade.
        boolean effectiveImportant = important || this.isChunkPrioritized(section);
        if (((RenderSectionTransferAccess) section)
                .vro$hasEquivalentPendingRebuild(effectiveImportant)) {
            this.needsUpdate = true;
            callback.cancel();
        }
    }

    @Redirect(
            method = "submitRebuildTasks",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/RenderSectionManager;createRebuildTask(Lme/jellysquid/mods/sodium/client/render/chunk/RenderSection;)Lme/jellysquid/mods/sodium/client/render/chunk/tasks/ChunkRenderBuildTask;"
            ),
            require = 1
    )
    private ChunkRenderBuildTask vro$suppressSecondActiveRebuild(
            RenderSectionManager ignored,
            RenderSection section,
            ChunkUpdateType updateType
    ) {
        if (updateType != ChunkUpdateType.INITIAL_BUILD
                && ((RenderSectionTransferAccess) section).vro$hasActiveBuildTask()) {
            return null;
        }
        return this.createRebuildTask(section);
    }
}
