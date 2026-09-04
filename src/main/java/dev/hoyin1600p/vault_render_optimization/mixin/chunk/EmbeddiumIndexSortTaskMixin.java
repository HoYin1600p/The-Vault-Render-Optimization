package dev.hoyin1600p.vault_render_optimization.mixin.chunk;

import dev.hoyin1600p.vault_render_optimization.client.chunk.sorting.IndexOnlySortTask;
import dev.hoyin1600p.vault_render_optimization.client.chunk.sorting.IndexSortState;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderBuildTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class EmbeddiumIndexSortTaskMixin {
    @Shadow private float cameraX;
    @Shadow private float cameraY;
    @Shadow private float cameraZ;
    @Shadow private int currentFrame;

    @Inject(method = "createSortTask", at = @At("HEAD"), cancellable = true, require = 1)
    private void vro$createIndexOnlySort(RenderSection section, CallbackInfoReturnable<ChunkRenderBuildTask> callback) {
        if (!IndexSortState.enabled()) return;
        ChunkRenderBuildTask task = IndexOnlySortTask.capture(section, currentFrame, cameraX, cameraY, cameraZ);
        if (task != null) callback.setReturnValue(task);
    }
}
