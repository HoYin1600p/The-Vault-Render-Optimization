package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.diagnostics.ChunkRebuildDiagnostics;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class RenderSectionManagerDiagnosticsMixin {
    @Unique
    private long vault_render_optimization$updateChunksStartNs;

    @Inject(method = "scheduleRebuild", at = @At("HEAD"), remap = false)
    private void vault_render_optimization$recordScheduleRebuild(int x, int y, int z, boolean important, CallbackInfo ci) {
        ChunkRebuildDiagnostics.recordSchedule(x, y, z, important);
    }

    @Inject(method = "updateChunks", at = @At("HEAD"), remap = false)
    private void vault_render_optimization$beforeUpdateChunks(CallbackInfo ci) {
        if (ChunkRebuildDiagnostics.enabled()) {
            this.vault_render_optimization$updateChunksStartNs = System.nanoTime();
        }
    }

    @Inject(method = "updateChunks", at = @At("RETURN"), remap = false)
    private void vault_render_optimization$afterUpdateChunks(CallbackInfo ci) {
        if (ChunkRebuildDiagnostics.enabled()) {
            ChunkRebuildDiagnostics.recordUpdateChunks(System.nanoTime() - this.vault_render_optimization$updateChunksStartNs);
        }
    }
}
