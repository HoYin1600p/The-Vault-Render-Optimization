package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleRandom;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import java.util.Random;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Particle.class)
public abstract class ParticleRandomMixin {
    /** {@code protected final Random random = new Random();} runs in the base constructor. */
    @Redirect(method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDD)V",
            at = @At(value = "NEW", target = "java/util/Random"), require = 1)
    private static Random vro$particleRandom() {
        return ClientOptimizationConfig.particleSharedRandom && ClientOptimizationConfig.optimizationsEnabled()
                ? ParticleRandom.current() : new Random();
    }

    /**
     * The velocity constructor draws six Math.random() values, each a CAS on the JVM-wide generator.
     * An INVOKE redirect handler must match the (instance) constructor, unlike the NEW redirect above.
     */
    @Redirect(method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDD)V",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;random()D"), require = 1)
    private double vro$particleMathRandom() {
        return ClientOptimizationConfig.particleSharedRandom && ClientOptimizationConfig.optimizationsEnabled()
                ? ParticleRandom.current().nextDouble() : Math.random();
    }
}
