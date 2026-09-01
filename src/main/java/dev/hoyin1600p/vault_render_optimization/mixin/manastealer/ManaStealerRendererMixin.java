package dev.hoyin1600p.vault_render_optimization.mixin.manastealer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.vault_render_optimization.client.compat.manastealer.ManaStealerVisualConfig;
import dev.hoyin1600p.vault_render_optimization.client.compat.manastealer.ManaStealerVisualController;
import iskallia.vault.entity.entity.ManaStealerEntity;
import iskallia.vault.entity.renderer.ManaStealerRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ManaStealerRenderer.class, remap = false)
public abstract class ManaStealerRendererMixin {
    @Inject(
            method = "render(Liskallia/vault/entity/entity/ManaStealerEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void vro$replaceManaStealerSigil(
            ManaStealerEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            CallbackInfo ci
    ) {
        if (ManaStealerVisualController.canReplace() && ManaStealerVisualConfig.replaceGroundSigil()) {
            ci.cancel();
        }
    }
}
