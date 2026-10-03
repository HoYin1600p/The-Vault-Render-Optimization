package dev.hoyin1600p.vault_render_optimization.mixin.vault;

import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleRandom;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import java.util.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The Nova explosion provider allocates a Random per particle for one nextFloat. */
@Pseudo
@Mixin(targets = "iskallia.vault.client.particles.NovaExplosionParticle$Provider", remap = false)
public abstract class NovaExplosionProviderRandomMixin {
    @Redirect(method = "createParticle(Lnet/minecraft/core/particles/SimpleParticleType;Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At(value = "NEW", target = "java/util/Random"), require = 0, remap = false)
    private Random vro$providerRandom() {
        return ClientOptimizationConfig.particleSharedRandom && ClientOptimizationConfig.optimizationsEnabled()
                ? ParticleRandom.current() : new Random();
    }
}
