package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.mixin.flw;

import com.simibubi.create.content.contraptions.render.FlwContraptionManager;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.FlywheelShaderCompatState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Client render preparation only; server contraption simulation is untouched. */
@Mixin(value = FlwContraptionManager.class, remap = false)
public class MixinFlwContraptionManager {
    @Inject(method = "tick()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void vro$waitForShaderPipeline(CallbackInfo ci) {
        if (FlywheelShaderCompatState.deferModelBuilds()) ci.cancel();
    }
}
