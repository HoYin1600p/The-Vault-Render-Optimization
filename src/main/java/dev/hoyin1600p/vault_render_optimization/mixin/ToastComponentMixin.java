package dev.hoyin1600p.vault_render_optimization.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.vault_render_optimization.client.render.ToastVisibilityProbe;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Deque;

@Mixin(ToastComponent.class)
public abstract class ToastComponentMixin {
    @Shadow
    @Final
    private Deque<Toast> queued;

    @Unique
    private boolean vro$hasToastWork;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void vro$skipEmptyRender(PoseStack poseStack, CallbackInfo ci) {
        if (ClientOptimizationConfig.optimizationsEnabled()
                && ClientOptimizationConfig.emptyToastRenderSkip
                && !this.vro$hasToastWork) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void vro$updateRenderActivity(PoseStack poseStack, CallbackInfo ci) {
        if (!ClientOptimizationConfig.optimizationsEnabled()
                || !ClientOptimizationConfig.emptyToastRenderSkip
                || !this.queued.isEmpty()
                || !ToastVisibilityProbe.hasNoVisibleToasts((ToastComponent) (Object) this)) {
            return;
        }
        this.vro$hasToastWork = false;
    }

    @Inject(method = "addToast", at = @At("HEAD"))
    private void vro$markToastAdded(Toast toast, CallbackInfo ci) {
        this.vro$hasToastWork = true;
    }

    @Inject(method = "clear", at = @At("TAIL"))
    private void vro$markToastsCleared(CallbackInfo ci) {
        this.vro$hasToastWork = false;
    }
}
