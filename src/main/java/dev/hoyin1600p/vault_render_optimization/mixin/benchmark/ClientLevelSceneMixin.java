package dev.hoyin1600p.vault_render_optimization.mixin.benchmark;

import dev.hoyin1600p.vault_render_optimization.client.benchmark.scene.VirtualScene;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelSceneMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void vro$skipVirtualTick(Entity entity, CallbackInfo ci) {
        if (VirtualScene.isVirtual(entity)) ci.cancel();
    }
}
