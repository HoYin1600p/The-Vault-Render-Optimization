package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleRandom;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import java.util.Random;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Particle packets (Frost Nova sends 400 per cast) spread each particle with six synchronized
 * {@code nextGaussian()} calls on the listener's Random. The spread loop runs on the client thread
 * (after {@code ensureRunningOnSameThread}), so the per-thread generator gives the same distribution
 * without the monitor and atomic seed.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerParticleRandomMixin {
    @Redirect(method = "handleParticleEvent", at = @At(value = "INVOKE", target = "Ljava/util/Random;nextGaussian()D"),
            require = 1)
    private double vro$spreadGaussian(Random random) {
        return ClientOptimizationConfig.particleSharedRandom && ClientOptimizationConfig.optimizationsEnabled()
                ? ParticleRandom.current().nextGaussian() : random.nextGaussian();
    }
}
