package dev.hoyin1600p.vault_render_optimization.mixin.entitygpu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuBlockModels;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.IModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Whole block models drawn by block entity renderers: Forge's {@code renderModel} with model data (vanilla's
 * overload delegates to it; Embeddium overwrites it). An eligible model reserves its vertices for the GPU; the audit
 * checks the final method.
 */
@Mixin(value = ModelBlockRenderer.class, priority = 1100)
public abstract class ModelBlockRendererGpuMixin {
    @Inject(method = "renderModel(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            + "Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/resources/model/BakedModel;"
            + "FFFIILnet/minecraftforge/client/model/data/IModelData;)V", at = @At("HEAD"), cancellable = true,
            require = 0, remap = false)
    private void vro$modelOnGpu(PoseStack.Pose pose, VertexConsumer consumer, BlockState state, BakedModel model,
                                float red, float green, float blue, int light, int overlay, IModelData data,
                                CallbackInfo ci) {
        if (GpuBlockModels.tryReserve((ModelBlockRenderer) (Object) this, pose, consumer, state, model, red, green,
                blue, light, overlay, data)) {
            ci.cancel();
        }
    }
}
