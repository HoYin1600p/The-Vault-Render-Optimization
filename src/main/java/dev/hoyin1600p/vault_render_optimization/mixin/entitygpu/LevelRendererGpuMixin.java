package dev.hoyin1600p.vault_render_optimization.mixin.entitygpu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuEntityModels;
import dev.hoyin1600p.vault_render_optimization.client.render.AllocationProbe;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Decides once per level frame whether model parts may reserve GPU vertices. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererGpuMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void vro$beginGpuEntityFrame(PoseStack poseStack, float partialTick, long finishNanoTime,
                                         boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
                                         LightTexture lightTexture, Matrix4f projection, CallbackInfo ci) {
        GpuEntityModels.beginFrame();
        AllocationProbe.frame();
        dev.hoyin1600p.vault_render_optimization.client.render.FastloadFrustum.beginFrame();
    }
}
