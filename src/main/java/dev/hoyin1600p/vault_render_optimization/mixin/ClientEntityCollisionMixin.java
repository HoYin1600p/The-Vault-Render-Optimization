package dev.hoyin1600p.vault_render_optimization.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class ClientEntityCollisionMixin {
    @Inject(method = "isInWall", at = @At("HEAD"), cancellable = true)
    private void vaultRenderOptimization$skipClientWallCheck(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity.level.isClientSide) {
            cir.setReturnValue(false);
        }
    }
}
