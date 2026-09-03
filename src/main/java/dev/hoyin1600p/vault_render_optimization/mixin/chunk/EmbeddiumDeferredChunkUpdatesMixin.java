package dev.hoyin1600p.vault_render_optimization.mixin.chunk;

import dev.hoyin1600p.vault_render_optimization.client.chunk.ChunkUpdateState;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Selects the existing deferred branch, not a replacement scheduler.
 * The renderer retains task priority, result draining, cancellation and GPU ownership.
 * Deliberately independent of VRO's optional rebuild de-duplication transfer.
 */
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager", remap = false)
public abstract class EmbeddiumDeferredChunkUpdatesMixin {
    @Shadow private boolean alwaysDeferChunkUpdates;

    @Redirect(
            method = "submitRebuildTasks",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/RenderSectionManager;alwaysDeferChunkUpdates:Z"),
            require = 1, allow = 1
    )
    private boolean vro$selectNativeDeferredScheduling(RenderSectionManager manager) {
        return ChunkUpdateState.defer(this.alwaysDeferChunkUpdates);
    }
}
