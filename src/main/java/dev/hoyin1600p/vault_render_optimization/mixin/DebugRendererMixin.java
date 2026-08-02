package dev.hoyin1600p.vault_render_optimization.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.vault_render_optimization.client.render.GameTestMarkerState;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.debug.GameTestDebugRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugRenderer.class)
public abstract class DebugRendererMixin {
    @Shadow
    private boolean renderChunkborder;

    @Shadow
    @Final
    private GameTestDebugRenderer gameTestDebugRenderer;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void vro$skipEmptyRender(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                                     double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        if (!ClientOptimizationConfig.optimizationsEnabled()
                || !ClientOptimizationConfig.emptyDebugRenderSkip) {
            return;
        }

        boolean chunkBorderVisible = this.renderChunkborder && !Minecraft.getInstance().showOnlyReducedInfo();
        boolean hasGameTestMarkers = ((GameTestMarkerState) this.gameTestDebugRenderer).vro$hasGameTestMarkers();
        if (!chunkBorderVisible && !hasGameTestMarkers) {
            ci.cancel();
        }
    }
}
