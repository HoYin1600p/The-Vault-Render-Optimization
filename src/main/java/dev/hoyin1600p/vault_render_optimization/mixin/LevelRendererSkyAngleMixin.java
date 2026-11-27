/*
 * Informed by BadOptimizations (ISC license) sky-angle caching in LevelRenderer.renderSky.
 */
package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererSkyAngleMixin {
    @Unique
    private float vro$cachedSkyAngle;

    @Unique
    private boolean vro$hasCachedSkyAngle;

    @Redirect(
            method = "renderSky",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;getTimeOfDay(F)F",
                    ordinal = 0
            )
    )
    private float vro$captureSkyAngle(ClientLevel level, float partialTick) {
        float angle = level.getTimeOfDay(partialTick);
        if (ClientOptimizationConfig.optimizationsEnabled()
                && ClientOptimizationConfig.skyAngleCaching) {
            this.vro$cachedSkyAngle = angle;
            this.vro$hasCachedSkyAngle = true;
        } else {
            this.vro$hasCachedSkyAngle = false;
        }
        return angle;
    }

    @Redirect(
            method = "renderSky",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;getTimeOfDay(F)F",
                    ordinal = 1
            )
    )
    private float vro$reuseSkyAngle(ClientLevel level, float partialTick) {
        if (this.vro$hasCachedSkyAngle
                && ClientOptimizationConfig.optimizationsEnabled()
                && ClientOptimizationConfig.skyAngleCaching) {
            this.vro$hasCachedSkyAngle = false;
            return this.vro$cachedSkyAngle;
        }
        this.vro$hasCachedSkyAngle = false;
        return level.getTimeOfDay(partialTick);
    }
}
