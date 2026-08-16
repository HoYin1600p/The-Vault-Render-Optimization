package dev.hoyin1600p.vault_render_optimization.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.render.ContraptionRenderInfo;
import com.simibubi.create.content.contraptions.render.SBBContraptionManager;
import dev.hoyin1600p.vault_render_optimization.client.create.SectionedContraptionRenderer;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SBBContraptionManager.class, remap = false)
public abstract class CreateSbbContraptionManagerMixin {
    @Inject(method = "renderContraptionLayerSBB", at = @At("HEAD"), cancellable = true, remap = false)
    private void vro$renderSectionedFallback(ContraptionRenderInfo renderInfo,
                                             RenderType layer,
                                             VertexConsumer consumer,
                                             CallbackInfo ci) {
        if (SectionedContraptionRenderer.renderFallback(renderInfo, layer, consumer)) {
            ci.cancel();
        }
    }

    @Inject(method = "invalidate", at = @At("HEAD"), remap = false)
    private void vro$invalidateSectionedFallback(Contraption contraption,
                                                 CallbackInfoReturnable<Boolean> cir) {
        SectionedContraptionRenderer.invalidateFallback(contraption);
    }
}
