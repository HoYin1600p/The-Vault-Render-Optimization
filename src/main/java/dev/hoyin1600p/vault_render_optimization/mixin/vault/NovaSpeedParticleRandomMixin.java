package dev.hoyin1600p.vault_render_optimization.mixin.vault;

import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleRandom;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Frost and Poison Nova particles call Math.random(), a CAS on the JVM-wide generator, per spawn. */
@Pseudo
@Mixin(targets = "iskallia.vault.client.particles.NovaSpeedParticle", remap = false)
public abstract class NovaSpeedParticleRandomMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/Math;random()D"), require = 0, remap = false)
    private double vro$random() {
        return ClientOptimizationConfig.particleSharedRandom && ClientOptimizationConfig.optimizationsEnabled()
                ? ParticleRandom.current().nextDouble() : Math.random();
    }
}
