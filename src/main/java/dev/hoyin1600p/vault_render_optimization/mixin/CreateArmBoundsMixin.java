package dev.hoyin1600p.vault_render_optimization.mixin;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ArmBlockEntity.class, remap = false)
public abstract class CreateArmBoundsMixin {
    @Inject(method = "createRenderBoundingBox", at = @At("HEAD"), cancellable = true, remap = false)
    private void vro$createArmBounds(CallbackInfoReturnable<AABB> cir) {
        if (!ClientOptimizationConfig.optimizationsEnabled()
                || !ClientOptimizationConfig.createSmartRenderBounds) {
            return;
        }
        ArmBlockEntity arm = (ArmBlockEntity) (Object) this;
        cir.setReturnValue(new AABB(arm.getBlockPos()).inflate(2).expandTowards(0, 1, 0));
    }
}
