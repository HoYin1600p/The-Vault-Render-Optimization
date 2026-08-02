package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.client.tutorial.TutorialSteps;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Tutorial.class)
public abstract class TutorialMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private List<?> timedToasts;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void vro$skipCompletedTutorial(CallbackInfo ci) {
        if (ClientOptimizationConfig.optimizationsEnabled()
                && ClientOptimizationConfig.inactiveTutorialSkip
                && this.minecraft.options.tutorialStep == TutorialSteps.NONE
                && this.timedToasts.isEmpty()) {
            ci.cancel();
        }
    }
}
