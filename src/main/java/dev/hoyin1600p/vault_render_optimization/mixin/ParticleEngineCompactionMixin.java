package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleListCompaction;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import java.util.Collection;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineCompactionMixin {
    @Shadow
    protected abstract void tickParticle(Particle particle);

    @Shadow
    protected abstract void updateCount(ParticleGroup group, int delta);

    @Inject(method = "tickParticleList", at = @At("HEAD"), cancellable = true)
    private void vro$tickAndCompact(Collection<Particle> particles, CallbackInfo ci) {
        if (!ClientOptimizationConfig.particleTickCompaction || !ClientOptimizationConfig.optimizationsEnabled()) {
            return;
        }
        ci.cancel();
        ParticleListCompaction.tick(particles, this::tickParticle, Particle::isAlive,
                particle -> particle.getParticleGroup().ifPresent(group -> this.updateCount(group, -1)));
    }
}
