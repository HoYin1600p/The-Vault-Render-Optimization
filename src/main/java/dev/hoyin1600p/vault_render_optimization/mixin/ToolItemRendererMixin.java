package dev.hoyin1600p.vault_render_optimization.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.vault_render_optimization.cache.VaultToolRenderCache;
import iskallia.vault.item.render.ToolItemRenderer;
import iskallia.vault.item.render.core.SpecialItemRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ToolItemRenderer.class, remap = false)
public abstract class ToolItemRendererMixin extends SpecialItemRenderer {
    @Inject(method = "m_108829_", at = @At("HEAD"), cancellable = true)
    private void vault_render_optimization$renderCachedToolModels(
            ItemStack stack,
            ItemTransforms.TransformType transformType,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            CallbackInfo ci
    ) {
        VaultToolRenderCache.ToolModels models = VaultToolRenderCache.getStaticModels(stack);
        if (models == null) {
            return;
        }

        this.renderModel(
                models.handle(),
                0xFFFFFF,
                stack,
                transformType,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                null
        );
        this.renderModel(
                models.head(),
                0xFFFFFF,
                stack,
                transformType,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                null
        );
        ci.cancel();
    }
}
