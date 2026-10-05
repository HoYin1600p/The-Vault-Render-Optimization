package dev.hoyin1600p.vault_render_optimization.mixin.benchmark;

import dev.hoyin1600p.vault_render_optimization.client.benchmark.scene.VirtualScene;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevent clicks on a virtual actor from producing ServerboundInteractPacket. */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeSceneMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void vro$skipAttack(Player player, Entity entity, CallbackInfo ci) {
        if (VirtualScene.isVirtual(entity)) ci.cancel();
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void vro$skipInteract(Player player, Entity entity, InteractionHand hand,
                                  CallbackInfoReturnable<InteractionResult> cir) {
        if (VirtualScene.isVirtual(entity)) cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "interactAt", at = @At("HEAD"), cancellable = true)
    private void vro$skipInteractAt(Player player, Entity entity, EntityHitResult hit,
                                    InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (VirtualScene.isVirtual(entity)) cir.setReturnValue(InteractionResult.FAIL);
    }
}
