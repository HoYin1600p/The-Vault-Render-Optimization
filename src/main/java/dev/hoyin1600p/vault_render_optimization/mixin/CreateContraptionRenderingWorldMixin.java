package dev.hoyin1600p.vault_render_optimization.mixin;

import com.simibubi.create.content.contraptions.render.ContraptionRenderingWorld;
import dev.hoyin1600p.vault_render_optimization.client.create.SectionedContraptionRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContraptionRenderingWorld.class, remap = false)
public abstract class CreateContraptionRenderingWorldMixin {
    @Inject(method = "removeDeadRenderers", at = @At("TAIL"), remap = false)
    private void vro$removeDeadSectionedFallbacks(CallbackInfo ci) {
        SectionedContraptionRenderer.removeDeadFallbacks();
    }
}
