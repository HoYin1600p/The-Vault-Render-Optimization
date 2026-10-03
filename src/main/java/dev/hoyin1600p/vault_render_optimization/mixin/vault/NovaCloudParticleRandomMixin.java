package dev.hoyin1600p.vault_render_optimization.mixin.vault;

import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleRandom;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import java.util.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Nova cloud particles allocate a new Random only to pick their sprite; draw it from the shared one. */
@Pseudo
@Mixin(targets = "iskallia.vault.client.particles.NovaExplosionCloudParticle", remap = false)
public abstract class NovaCloudParticleRandomMixin {
    @Redirect(method = "<init>", at = @At(value = "NEW", target = "java/util/Random"), require = 0, remap = false)
    private static Random vro$spriteRandom() {
        return ClientOptimizationConfig.particleSharedRandom && ClientOptimizationConfig.optimizationsEnabled()
                ? ParticleRandom.current() : new Random();
    }
}
