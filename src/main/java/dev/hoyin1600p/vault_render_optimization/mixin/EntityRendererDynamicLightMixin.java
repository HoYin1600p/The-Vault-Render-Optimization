package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.lighting.DynamicLightEngine;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererDynamicLightMixin<T extends Entity> {
    @Inject(method = "getBlockLightLevel", at = @At("RETURN"), cancellable = true)
    private void vro$applyDynamicEntityLight(T entity, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(DynamicLightEngine.applyEntityBlockLight(entity, pos, cir.getReturnValue()));
    }
}
