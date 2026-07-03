package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.diagnostics.ChunkRebuildDiagnostics;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Mixin(value = RenderRegionManager.class, remap = false)
public abstract class RenderRegionManagerDiagnosticsMixin {
    @Unique
    private long vault_render_optimization$uploadStartNs;

    @Unique
    private long vault_render_optimization$setupUploadBatchesStartNs;

    @Inject(method = "upload(Lme/jellysquid/mods/sodium/client/gl/device/CommandList;Ljava/util/Iterator;)V", at = @At("HEAD"), remap = false)
    private void vault_render_optimization$beforeUpload(CommandList commandList, Iterator<ChunkBuildResult> queue, CallbackInfo ci) {
        if (ChunkRebuildDiagnostics.enabled()) {
            this.vault_render_optimization$uploadStartNs = System.nanoTime();
        }
    }

    @Inject(method = "upload(Lme/jellysquid/mods/sodium/client/gl/device/CommandList;Ljava/util/Iterator;)V", at = @At("RETURN"), remap = false)
    private void vault_render_optimization$afterUpload(CommandList commandList, Iterator<ChunkBuildResult> queue, CallbackInfo ci) {
        if (ChunkRebuildDiagnostics.enabled()) {
            ChunkRebuildDiagnostics.recordUpload(System.nanoTime() - this.vault_render_optimization$uploadStartNs);
        }
    }

    @Inject(method = "setupUploadBatches", at = @At("HEAD"), remap = false)
    private void vault_render_optimization$beforeSetupUploadBatches(Iterator<ChunkBuildResult> renders, CallbackInfoReturnable<Map<RenderRegion, List<ChunkBuildResult>>> cir) {
        if (ChunkRebuildDiagnostics.enabled()) {
            this.vault_render_optimization$setupUploadBatchesStartNs = System.nanoTime();
        }
    }

    @Inject(method = "setupUploadBatches", at = @At("RETURN"), remap = false)
    private void vault_render_optimization$afterSetupUploadBatches(Iterator<ChunkBuildResult> renders, CallbackInfoReturnable<Map<RenderRegion, List<ChunkBuildResult>>> cir) {
        if (ChunkRebuildDiagnostics.enabled()) {
            ChunkRebuildDiagnostics.recordSetupBatches(System.nanoTime() - this.vault_render_optimization$setupUploadBatchesStartNs);
        }
    }
}
