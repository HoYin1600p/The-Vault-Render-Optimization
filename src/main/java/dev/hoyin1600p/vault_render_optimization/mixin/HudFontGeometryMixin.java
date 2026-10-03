package dev.hoyin1600p.vault_render_optimization.mixin;

import com.mojang.math.Matrix4f;
import dev.hoyin1600p.vault_render_optimization.client.hud.HudTextGeometry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Style;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Font.class)
public abstract class HudFontGeometryMixin {
    @Shadow abstract FontSet getFontSet(ResourceLocation font);
    @Inject(method = "drawInBatch(Ljava/lang/String;FFIZLcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;ZIIZ)I",
            at = @At("HEAD"), cancellable = true)
    private void vro$reuseUnchangedHudText(String text, float x, float y, int color, boolean shadow,
            Matrix4f pose, MultiBufferSource buffers, boolean seeThrough, int background, int light,
            boolean bidi, CallbackInfoReturnable<Integer> callback) {
        if (!HudTextGeometry.eligibleScope()) return;
        Integer result = HudTextGeometry.draw((Font) (Object) this, getFontSet(Style.DEFAULT_FONT), text, x, y, color, shadow, pose,
                buffers, seeThrough, background, light, bidi);
        if (result != null) callback.setReturnValue(result);
    }
}
