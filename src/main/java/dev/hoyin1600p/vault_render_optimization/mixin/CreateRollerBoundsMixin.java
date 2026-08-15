package dev.hoyin1600p.vault_render_optimization.mixin;

import com.simibubi.create.content.contraptions.actors.roller.RollerBlockEntity;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RollerBlockEntity.class, remap = false)
public abstract class CreateRollerBoundsMixin {
    @Inject(method = "createRenderBoundingBox", at = @At("HEAD"), cancellable = true, remap = false)
    private void vro$createDirectionalRollerBounds(CallbackInfoReturnable<AABB> cir) {
        if (!ClientOptimizationConfig.optimizationsEnabled()
                || !ClientOptimizationConfig.createSmartRenderBounds) {
            return;
        }
        RollerBlockEntity roller = (RollerBlockEntity) (Object) this;
        Direction facing = roller.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        cir.setReturnValue(new AABB(roller.getBlockPos())
                .expandTowards(facing.getStepX(), -1, facing.getStepZ())
                .inflate(0.25));
    }
}
