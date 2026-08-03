package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.lighting.DynamicLightEngine;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityDynamicLightMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void vro$observeDynamicLight(CallbackInfo ci) {
        DynamicLightEngine.observeEntity((Entity) (Object) this);
    }

    @Inject(method = "remove", at = @At("TAIL"))
    private void vro$removeDynamicLight(Entity.RemovalReason reason, CallbackInfo ci) {
        DynamicLightEngine.removeEntity((Entity) (Object) this);
    }
}
