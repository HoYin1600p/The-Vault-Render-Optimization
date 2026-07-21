package dev.hoyin1600p.vault_render_optimization.mixin;

import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "owmii.powah.block.cable.CableNet", remap = false)
public abstract class PowahCableNetClientCrashGuardMixin {
    @Redirect(
            method = "addCable",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
            )
    )
    private static Object vaultRenderOptimization$replaceStaleCable(
            Map<Object, Object> cables, Object position, Object cable) {
        cables.put(position, cable);
        return null;
    }
}
