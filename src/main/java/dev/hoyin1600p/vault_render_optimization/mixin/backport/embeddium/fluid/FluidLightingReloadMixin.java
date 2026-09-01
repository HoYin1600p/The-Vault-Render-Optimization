package dev.hoyin1600p.vault_render_optimization.mixin.backport.embeddium.fluid;

import dev.hoyin1600p.vault_render_optimization.renderertransfer.SmoothFluidLightingCache;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SodiumWorldRenderer.class, remap = false)
public abstract class FluidLightingReloadMixin {
    @Inject(method = "reload", at = @At("HEAD"), require = 1)
    private void vro$clearFluidLightingCache(CallbackInfo callback) {
        SmoothFluidLightingCache.clear();
    }
}
