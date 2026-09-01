/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Particle billboard geometry adapted from Flerovium.
 * Source: https://github.com/MoePus/Flerovium/blob/240f08c62745d57bf200440c9932e0c7907bc5f7/src/main/java/com/moepus/flerovium/mixins/Particle/SingleQuadParticleMixin.java
 * VRO adaptation: Rubidium/Embeddium packed output, explicit ownership, hot configuration, and diagnostics.
 */
package dev.hoyin1600p.vault_render_optimization.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleBillboardGeometry;
import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleDiagnostics;
import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleOptimizationState;
import me.jellysquid.mods.sodium.client.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.client.model.vertex.VertexDrain;
import me.jellysquid.mods.sodium.client.model.vertex.formats.particle.ParticleVertexSink;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
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
public abstract class SodiumSingleQuadParticleMixin extends Particle {
    protected SodiumSingleQuadParticleMixin(ClientLevel level, double x, double y, double z) {
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
        Vec3 cameraPosition = camera.getPosition();
        float positionX = (float) (Mth.lerp(partialTick, this.xo, this.x) - cameraPosition.x());
        float positionY = (float) (Mth.lerp(partialTick, this.yo, this.y) - cameraPosition.y());
        float positionZ = (float) (Mth.lerp(partialTick, this.zo, this.z) - cameraPosition.z());
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        float angle = this.roll == 0.0F ? 0.0F : Mth.lerp(partialTick, this.oRoll, this.roll);
        ParticleBillboardGeometry geometry = ParticleBillboardGeometry.compute(
                left.x(), left.y(), left.z(),
                up.x(), up.y(), up.z(),
                angle,
                this.getQuadSize(partialTick),
                positionX, positionY, positionZ
        );

        float minU = this.getU0();
        float maxU = this.getU1();
        float minV = this.getV0();
        float maxV = this.getV1();
        int light = this.getLightColor(partialTick);
        int color = ColorABGR.pack(this.rCol, this.gCol, this.bCol, this.alpha);
        ParticleVertexSink drain = VertexDrain.of(consumer).createSink(VanillaVertexTypes.PARTICLES);
        drain.ensureCapacity(4);
        drain.writeParticle(geometry.x0(), geometry.y0(), geometry.z0(), maxU, maxV, color, light);
        drain.writeParticle(geometry.x1(), geometry.y1(), geometry.z1(), maxU, minV, color, light);
        drain.writeParticle(geometry.x2(), geometry.y2(), geometry.z2(), minU, minV, color, light);
        drain.writeParticle(geometry.x3(), geometry.y3(), geometry.z3(), minU, maxV, color, light);
        drain.flush();
        ParticleDiagnostics.recordVroBillboard(true, this.getClass());
    }
}
