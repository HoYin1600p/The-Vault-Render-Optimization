package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.mixin.flw;

import com.jozufozu.flywheel.backend.ShadersModHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.FlywheelShaderCompatState;

@Mixin(ShadersModHandler.class)
public class IrisHandlerMixin {
    @Inject(at = @At("HEAD"), method = "isShaderPackInUse()Z", cancellable = true,remap = false)
    private static void isShaderPackInUse(CallbackInfoReturnable<Boolean> cir){
        if (FlywheelShaderCompatState.shouldUseShaderInstancing()) {
            cir.setReturnValue(false);
        }
    }}
