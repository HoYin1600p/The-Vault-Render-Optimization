package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.config.VroScreenScale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fixed virtual size for VRO's own screens; see {@link VroScreenScale}. */
@Mixin(Minecraft.class)
public abstract class MinecraftScreenScaleMixin {
    @Inject(method = "setScreen", at = @At("HEAD"))
    private void vro$rememberLeavingScreen(Screen screen, CallbackInfo ci) {
        VroScreenScale.beforeSetScreen((Minecraft) (Object) this);
    }

    @Inject(method = "setScreen", at = @At("RETURN"))
    private void vro$applyScreenScale(Screen screen, CallbackInfo ci) {
        VroScreenScale.afterSetScreen((Minecraft) (Object) this);
    }

    // Window resize, fullscreen toggle and GUI-scale changes all end here: vanilla resets the scale
    // first, then VRO re-applies its own while one of its screens is open.
    @Inject(method = "resizeDisplay", at = @At("RETURN"))
    private void vro$keepScreenScale(CallbackInfo ci) {
        VroScreenScale.afterResizeDisplay((Minecraft) (Object) this);
    }
}
