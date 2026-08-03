package dev.hoyin1600p.vault_render_optimization.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import iskallia.vault.client.particles.ElixirOrbParticle;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ElixirOrbParticle.class, remap = false)
public abstract class ElixirOrbParticleMixin {
    @Shadow
    @Final
    private static ResourceLocation TEXTURE;

    @Unique
    private static final MultiBufferSource.BufferSource VRO$ELIXIR_TEXT_BUFFER =
            MultiBufferSource.immediate(new BufferBuilder(256));

    @Redirect(
            method = "renderValue(Lnet/minecraft/client/Camera;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderBuffers;bufferSource()Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;",
                    remap = true
            ),
            remap = false
    )
    private MultiBufferSource.BufferSource vault_render_optimization$useIsolatedTextBuffer(
            RenderBuffers renderBuffers) {
        return VRO$ELIXIR_TEXT_BUFFER;
    }

    @Inject(
            method = "renderValue(Lnet/minecraft/client/Camera;F)V",
            at = @At("RETURN"),
            remap = false
    )
    private void vault_render_optimization$restoreParticleRenderState(
            Camera camera, float partialTick, CallbackInfo callbackInfo) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gameRenderer.lightTexture().turnOnLightLayer();

        RenderSystem.setShader(GameRenderer::getParticleShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
    }
}
