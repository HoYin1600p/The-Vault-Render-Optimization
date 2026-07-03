package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.diagnostics.ChunkRebuildDiagnostics;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChunkBuilder.class, remap = false)
public abstract class ChunkBuilderDiagnosticsMixin {
    @Unique
    private long vault_render_optimization$stealTaskStartNs;

    @Inject(method = "stealTask", at = @At("HEAD"), remap = false)
    private void vault_render_optimization$beforeStealTask(CallbackInfoReturnable<Boolean> cir) {
        if (ChunkRebuildDiagnostics.enabled()) {
            this.vault_render_optimization$stealTaskStartNs = System.nanoTime();
        }
    }

    @Inject(method = "stealTask", at = @At("RETURN"), remap = false)
    private void vault_render_optimization$afterStealTask(CallbackInfoReturnable<Boolean> cir) {
        if (ChunkRebuildDiagnostics.enabled()) {
            ChunkRebuildDiagnostics.recordStealTask(System.nanoTime() - this.vault_render_optimization$stealTaskStartNs);
        }
    }
}
