package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.lighting.DynamicLightEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public abstract class MinecraftDynamicLightMixin {
    @Inject(method = "setLevel", at = @At("HEAD"))
    private void vro$clearDynamicLights(@Nullable ClientLevel level, CallbackInfo ci) {
        DynamicLightEngine.clear();
    }
}
