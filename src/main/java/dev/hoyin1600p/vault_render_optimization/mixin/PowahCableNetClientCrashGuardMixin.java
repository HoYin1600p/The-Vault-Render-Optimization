package dev.hoyin1600p.vault_render_optimization.mixin;

import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;
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

    @Redirect(
            method = "removeCable",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;",
                    ordinal = 0
            )
    )
    private static Object vaultRenderOptimization$removeCableOnlyIfCurrent(
            Map<Object, Object> cables, Object position, @Coerce Object cable) {
        if (cables.get(position) == cable) {
            cables.remove(position);
        }

        // Powah compares this return value with the cable being unloaded. A stale
        // unload is already complete when a replacement occupies the position.
        return cable;
    }
}
