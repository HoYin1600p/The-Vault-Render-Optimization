/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Particle billboard geometry adapted from Flerovium.
 * Source: https://github.com/MoePus/Flerovium/blob/240f08c62745d57bf200440c9932e0c7907bc5f7/src/main/java/com/moepus/flerovium/mixins/Particle/SingleQuadParticleMixin.java
 * VRO adaptation: portable VertexConsumer output, explicit ownership, hot configuration, and diagnostics.
 */
package dev.hoyin1600p.vault_render_optimization.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleBillboardGeometry;
import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleDiagnostics;
import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleOptimizationState;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SingleQuadParticle.class, priority = 100)
public abstract class SingleQuadParticleMixin extends Particle {
    protected SingleQuadParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Shadow
    public abstract float getQuadSize(float partialTick);

    @Shadow
    protected abstract float getU0();

    @Shadow
    protected abstract float getU1();

    @Shadow
    protected abstract float getV0();

    @Shadow
    protected abstract float getV1();

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void vro$renderBillboard(
            VertexConsumer consumer,
            Camera camera,
            float partialTick,
            CallbackInfo ci
    ) {
        if (!ParticleOptimizationState.useVroBillboardGeometry()) {
            ParticleDiagnostics.recordRendererPassthrough();
            return;
        }

        ci.cancel();
        ParticleBillboardGeometry geometry = this.vro$geometry(camera, partialTick);
        int light = this.getLightColor(partialTick);
        float minU = this.getU0();
        float maxU = this.getU1();
        float minV = this.getV0();
        float maxV = this.getV1();

        this.vro$write(consumer, geometry.x0(), geometry.y0(), geometry.z0(), maxU, maxV, light);
        this.vro$write(consumer, geometry.x1(), geometry.y1(), geometry.z1(), maxU, minV, light);
        this.vro$write(consumer, geometry.x2(), geometry.y2(), geometry.z2(), minU, minV, light);
        this.vro$write(consumer, geometry.x3(), geometry.y3(), geometry.z3(), minU, maxV, light);
        ParticleDiagnostics.recordVroBillboard(false, this.getClass());
    }

    private ParticleBillboardGeometry vro$geometry(Camera camera, float partialTick) {
        Vec3 cameraPosition = camera.getPosition();
        float positionX = (float) (Mth.lerp(partialTick, this.xo, this.x) - cameraPosition.x());
        float positionY = (float) (Mth.lerp(partialTick, this.yo, this.y) - cameraPosition.y());
        float positionZ = (float) (Mth.lerp(partialTick, this.zo, this.z) - cameraPosition.z());
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        float angle = this.roll == 0.0F ? 0.0F : Mth.lerp(partialTick, this.oRoll, this.roll);
        return ParticleBillboardGeometry.compute(
                left.x(), left.y(), left.z(),
                up.x(), up.y(), up.z(),
                angle,
                this.getQuadSize(partialTick),
                positionX, positionY, positionZ
        );
    }

    private void vro$write(
            VertexConsumer consumer,
            float x,
            float y,
            float z,
            float u,
            float v,
            int light
    ) {
        consumer.vertex(x, y, z)
                .uv(u, v)
                .color(this.rCol, this.gCol, this.bCol, this.alpha)
                .uv2(light)
                .endVertex();
    }
}
