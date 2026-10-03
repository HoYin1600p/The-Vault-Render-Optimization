package dev.hoyin1600p.vault_render_optimization.mixin;

import com.jozufozu.flywheel.config.BackendType;
import com.jozufozu.flywheel.config.FlwConfig;
import dev.hoyin1600p.vault_render_optimization.client.create.FlywheelBackendOverride;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Flywheel's {@code Backend.chooseEngine} reads the backend through {@code FlwConfig.getBackendType},
 * so answering INSTANCING here turns instancing on for this session without touching the stored
 * config value. chooseEngine still falls back to OFF for shader packs or missing GL support.
 */
@Mixin(value = FlwConfig.class, remap = false)
public abstract class FlywheelBackendOverrideMixin {
    @Inject(method = "getBackendType", at = @At("RETURN"), cancellable = true, remap = false)
    private void vro$sessionInstancing(CallbackInfoReturnable<BackendType> cir) {
        if (FlywheelBackendOverride.active(cir.getReturnValue() == BackendType.OFF)) {
            cir.setReturnValue(BackendType.INSTANCING);
        }
    }
}
