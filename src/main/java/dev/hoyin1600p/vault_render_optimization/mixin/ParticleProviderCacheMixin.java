package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleProviderCache;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class ParticleProviderCacheMixin {
    @Shadow
    @Final
    private Map<ResourceLocation, ParticleProvider<?>> providers;

    @Shadow
    protected ClientLevel level;

    @Unique
    private final ParticleProviderCache<ParticleType<?>, ParticleProvider<?>> vro$providerCache = new ParticleProviderCache<>();

    @Inject(method = "register(Lnet/minecraft/core/particles/ParticleType;Lnet/minecraft/client/particle/ParticleProvider;)V",
            at = @At("HEAD"))
    private void vro$invalidateOnProvider(CallbackInfo ci) {
        this.vro$providerCache.invalidate();
    }

    @Inject(method = "register(Lnet/minecraft/core/particles/ParticleType;Lnet/minecraft/client/particle/ParticleEngine$SpriteParticleRegistration;)V",
            at = @At("HEAD"))
    private void vro$invalidateOnSpriteProvider(CallbackInfo ci) {
        this.vro$providerCache.invalidate();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "makeParticle", at = @At("HEAD"), cancellable = true)
    private void vro$cachedProvider(ParticleOptions options, double x, double y, double z,
                                    double xd, double yd, double zd, CallbackInfoReturnable<Particle> cir) {
        if (!ClientOptimizationConfig.particleProviderCache || !ClientOptimizationConfig.optimizationsEnabled()) {
            return;
        }
        ParticleProvider provider = this.vro$providerCache.get(options.getType(), this.providers.size(),
                type -> this.providers.get(Registry.PARTICLE_TYPE.getKey(type)));
        cir.setReturnValue(provider == null ? null : provider.createParticle(options, this.level, x, y, z, xd, yd, zd));
    }
}
