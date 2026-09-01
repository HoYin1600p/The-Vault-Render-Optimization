package dev.hoyin1600p.vault_render_optimization.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleDiagnostics;
import java.util.Map;
import java.util.Queue;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineDiagnosticsMixin {
    @Shadow
    @Final
    private Map<ParticleRenderType, Queue<Particle>> particles;

    @Unique
    private long vro$renderStarted;

    @Unique
    private long vro$tickStarted;

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V",
            at = @At("HEAD"),
            remap = false
    )
    private void vro$beginRender(
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            LightTexture lightTexture,
            Camera camera,
            float partialTick,
            @Nullable Frustum frustum,
            CallbackInfo ci
    ) {
        this.vro$renderStarted = ParticleDiagnostics.beginRender(this.particles);
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V",
            at = @At("RETURN"),
            remap = false
    )
    private void vro$endRender(
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            LightTexture lightTexture,
            Camera camera,
            float partialTick,
            @Nullable Frustum frustum,
            CallbackInfo ci
    ) {
        ParticleDiagnostics.endRender(this.vro$renderStarted);
        this.vro$renderStarted = 0L;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void vro$beginTick(CallbackInfo ci) {
        this.vro$tickStarted = ParticleDiagnostics.beginTick();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void vro$endTick(CallbackInfo ci) {
        ParticleDiagnostics.endTick(this.vro$tickStarted);
        this.vro$tickStarted = 0L;
    }
}
