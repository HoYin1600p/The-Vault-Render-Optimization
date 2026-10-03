package dev.hoyin1600p.vault_render_optimization.mixin.entitygpu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuItems;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Items: {@code renderQuadList} writes one side's quads of an item model (vanilla, or Embeddium's overwrite of
 * it). An eligible list reserves its vertices for the GPU instead. The later priority places this callback
 * after any foreign HEAD hook that draws the quads itself and cancels (Quark's item-sharing fade), so those
 * lists never reach VRO; the mixin audit checks the final method.
 */
@Mixin(value = ItemRenderer.class, priority = 1100)
public abstract class ItemRendererGpuMixin {
    @Inject(method = "renderQuadList", at = @At("HEAD"), cancellable = true, require = 0)
    private void vro$quadsOnGpu(PoseStack poseStack, VertexConsumer consumer, List<BakedQuad> quads, ItemStack stack,
                                int light, int overlay, CallbackInfo ci) {
        if (GpuItems.tryReserve((ItemRenderer) (Object) this, poseStack, consumer, quads, stack, light, overlay)) {
            ci.cancel();
        }
    }
}
