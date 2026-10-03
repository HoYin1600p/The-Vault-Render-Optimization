package dev.hoyin1600p.vault_render_optimization.mixin.entitygpu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * The item colours the writers tint with, and (verify mode) the installed item writer (vanilla/Forge or
 * Embeddium) run into a scratch buffer.
 */
@Mixin(ItemRenderer.class)
public interface ItemRendererGpuAccessor {
    @Accessor("itemColors")
    net.minecraft.client.color.item.ItemColors vro$itemColors();

    @Invoker("renderQuadList")
    void vro$renderQuadList(PoseStack poseStack, VertexConsumer consumer, List<BakedQuad> quads, ItemStack stack,
                            int light, int overlay);
}
