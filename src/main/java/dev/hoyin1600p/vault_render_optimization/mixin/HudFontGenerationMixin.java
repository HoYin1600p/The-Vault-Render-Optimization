package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.hud.HudTextGeometry;
import net.minecraft.client.gui.font.FontSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FontSet.class)
public abstract class HudFontGenerationMixin {
    @Inject(method = {"reload", "close"}, at = @At("HEAD"))
    private void vro$invalidateReplacedGlyphs(CallbackInfo callback) {
        HudTextGeometry.invalidateFonts();
    }
}
