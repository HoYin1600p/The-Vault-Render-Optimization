package dev.hoyin1600p.vault_render_optimization.mixin;

import iskallia.vault.entity.renderer.VaultDamageNumberRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.text.DecimalFormat;

@Mixin(value = VaultDamageNumberRenderer.class, remap = false)
public abstract class VaultDamageNumberRendererMixin {
    @Shadow
    @Final
    private static DecimalFormat df;

    @Redirect(method = "render", at = @At(value = "NEW", target = "java/text/DecimalFormat"))
    private DecimalFormat vault_render_optimization$reuseDamageFormatter(String pattern) {
        return df;
    }
}
