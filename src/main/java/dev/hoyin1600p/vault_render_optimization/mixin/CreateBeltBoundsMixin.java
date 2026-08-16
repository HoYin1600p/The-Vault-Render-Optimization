package dev.hoyin1600p.vault_render_optimization.mixin;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BeltBlockEntity.class, remap = false)
public abstract class CreateBeltBoundsMixin {
    @org.spongepowered.asm.mixin.Shadow public int beltLength;

    @Unique private int vro$cachedBeltLength = Integer.MIN_VALUE;
    @Unique private BlockState vro$cachedBeltState;
    @Unique private boolean vro$cachedController;

    @Inject(method = "createRenderBoundingBox", at = @At("HEAD"), cancellable = true, remap = false)
    private void vro$createDirectionalBeltBounds(CallbackInfoReturnable<AABB> cir) {
        if (!ClientOptimizationConfig.optimizationsEnabled()
                || !ClientOptimizationConfig.createSmartRenderBounds) {
            return;
        }

        BeltBlockEntity belt = (BeltBlockEntity) (Object) this;
        if (!belt.isController() || beltLength <= 0) {
            return;
        }

        int length = Math.max(0, beltLength - 1);
        Vec3i direction = belt.getBeltChainDirection();
        AABB bounds = new AABB(belt.getBlockPos());
        bounds = bounds.expandTowards(
                direction.getX() * length,
                direction.getY() * length,
                direction.getZ() * length
        );
        cir.setReturnValue(bounds.inflate(0.25));
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/belt/BeltBlockEntity;invalidateRenderBoundingBox()V"
            ),
            remap = false
    )
    private void vro$invalidateBoundsOnlyWhenShapeChanges(BeltBlockEntity belt) {
        if (!ClientOptimizationConfig.optimizationsEnabled()
                || !ClientOptimizationConfig.createSmartRenderBounds) {
            ((CreateCachedRenderBoundsAccessor) (Object) belt).vro$invalidateRenderBoundingBox();
            return;
        }

        BlockState state = belt.getBlockState();
        boolean controller = belt.isController();
        if (vro$cachedBeltLength != beltLength
                || vro$cachedBeltState != state
                || vro$cachedController != controller) {
            vro$cachedBeltLength = beltLength;
            vro$cachedBeltState = state;
            vro$cachedController = controller;
            ((CreateCachedRenderBoundsAccessor) (Object) belt).vro$invalidateRenderBoundingBox();
        }
    }
}
