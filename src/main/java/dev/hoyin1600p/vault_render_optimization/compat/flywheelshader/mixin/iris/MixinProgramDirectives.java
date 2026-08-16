package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.mixin.iris;

import net.coderbot.iris.gl.blending.AlphaTest;
import net.coderbot.iris.shaderpack.ProgramDirectives;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.accessors.ProgramDirectivesAccessor;

import java.util.Optional;

@Mixin(value = ProgramDirectives.class,remap = false)
public class MixinProgramDirectives implements ProgramDirectivesAccessor {
    @Unique
    private AlphaTest flwAlphaTestOverride;

    @Override
    public void setFlwAlphaTestOverride(AlphaTest alphaTest) {
        flwAlphaTestOverride = alphaTest;
    }

    @Inject(method = "getAlphaTestOverride", at = @At("HEAD"), cancellable = true)
    private void injectAlphaTestOverride(CallbackInfoReturnable<Optional<AlphaTest>> cir){
        if (flwAlphaTestOverride!=null)
            cir.setReturnValue(Optional.of(flwAlphaTestOverride));
    }
}
