package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleDiagnostics;
import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleSharedLightCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Particle.class)
public abstract class ParticleLightCacheMixin {
    @Shadow
    protected ClientLevel level;

    @Shadow
    protected double x;

    @Shadow
    protected double y;

    @Shadow
    protected double z;

    @Unique
    private long vro$lightCacheTick = Long.MIN_VALUE;

    @Unique
    private long vro$lightCachePosition = Long.MIN_VALUE;

    @Unique
    private int vro$lightCacheValue;

    @Inject(method = "getLightColor", at = @At("HEAD"), cancellable = true)
    private void vro$cacheLightColor(float partialTick, CallbackInfoReturnable<Integer> cir) {
        if (!ClientOptimizationConfig.optimizationsEnabled()
                || !ClientOptimizationConfig.particleLightCache) {
            return;
        }

        int blockX = Mth.floor(this.x);
        int blockY = Mth.floor(this.y);
        int blockZ = Mth.floor(this.z);
        long position = BlockPos.asLong(blockX, blockY, blockZ);
        long tick = this.level.getGameTime();
        if (tick == this.vro$lightCacheTick && position == this.vro$lightCachePosition) {
            ParticleDiagnostics.recordParticleLightHit();
            cir.setReturnValue(this.vro$lightCacheValue);
            return;
        }

        int light;
        if (ClientOptimizationConfig.particleSharedLightCache) {
            light = ParticleSharedLightCache.get(this.level, tick, blockX, blockY, blockZ);
        } else {
            BlockPos blockPos = new BlockPos(blockX, blockY, blockZ);
            light = this.level.hasChunkAt(blockPos)
                    ? LevelRenderer.getLightColor(this.level, blockPos)
                    : 0;
            ParticleDiagnostics.recordLightLookup();
        }
        this.vro$lightCacheTick = tick;
        this.vro$lightCachePosition = position;
        this.vro$lightCacheValue = light;
        cir.setReturnValue(light);
    }
}
