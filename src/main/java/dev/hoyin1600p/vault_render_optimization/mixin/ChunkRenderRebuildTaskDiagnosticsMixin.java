package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.diagnostics.ChunkRebuildDiagnostics;
import me.jellysquid.mods.sodium.client.gl.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChunkRenderRebuildTask.class, remap = false)
public abstract class ChunkRenderRebuildTaskDiagnosticsMixin {
    @Unique
    private long vault_render_optimization$performBuildStartNs;

    @Inject(method = "performBuild", at = @At("HEAD"), remap = false)
    private void vault_render_optimization$beforePerformBuild(ChunkBuildContext buildContext, CancellationSource cancellationSource, CallbackInfoReturnable<ChunkBuildResult> cir) {
        if (ChunkRebuildDiagnostics.enabled()) {
            this.vault_render_optimization$performBuildStartNs = System.nanoTime();
        }
    }

    @Inject(method = "performBuild", at = @At("RETURN"), remap = false)
    private void vault_render_optimization$afterPerformBuild(ChunkBuildContext buildContext, CancellationSource cancellationSource, CallbackInfoReturnable<ChunkBuildResult> cir) {
        if (ChunkRebuildDiagnostics.enabled()) {
            ChunkRebuildDiagnostics.recordRebuild(System.nanoTime() - this.vault_render_optimization$performBuildStartNs);
        }
    }
}
