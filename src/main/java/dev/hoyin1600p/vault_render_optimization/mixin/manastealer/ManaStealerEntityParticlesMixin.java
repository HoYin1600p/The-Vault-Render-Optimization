package dev.hoyin1600p.vault_render_optimization.mixin.manastealer;

import dev.hoyin1600p.vault_render_optimization.client.compat.manastealer.ManaStealerVisualController;
import iskallia.vault.entity.entity.ManaStealerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ManaStealerEntity.class, remap = false)
public abstract class ManaStealerEntityParticlesMixin {
    @Inject(method = "spawnClientParticles()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void vro$replaceManaStealerParticles(CallbackInfo ci) {
        if (!ManaStealerVisualController.canReplace()) {
            return;
        }
        ManaStealerEntity entity = (ManaStealerEntity) (Object) this;
        ManaStealerVisualController.maintain(entity, entity.getRadius());
        ci.cancel();
    }
}
