package dev.hoyin1600p.vault_render_optimization.mixin;

import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlockEntity;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PortableStorageInterfaceBlockEntity.class, remap = false)
public abstract class CreatePortableStorageInterfaceBoundsMixin {
    @Inject(method = "createRenderBoundingBox", at = @At("HEAD"), cancellable = true, remap = false)
    private void vro$createDirectionalInterfaceBounds(CallbackInfoReturnable<AABB> cir) {
        if (!ClientOptimizationConfig.optimizationsEnabled()
                || !ClientOptimizationConfig.createSmartRenderBounds) {
            return;
        }
        PortableStorageInterfaceBlockEntity blockEntity =
                (PortableStorageInterfaceBlockEntity) (Object) this;
        Direction facing = blockEntity.getBlockState().getValue(BlockStateProperties.FACING);
        cir.setReturnValue(new AABB(blockEntity.getBlockPos())
                .expandTowards(facing.getStepX() * 2, facing.getStepY() * 2, facing.getStepZ() * 2)
                .inflate(0.25));
    }
}
