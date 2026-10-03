package dev.hoyin1600p.vault_render_optimization.mixin.entitygpu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.geckolib.GeckoLibGpuModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import software.bernie.geckolib3.geo.render.built.GeoCube;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

/**
 * GeckoLib 3 entity cubes: {@code renderRecursively} calls {@code renderCube} once per cube inside its own
 * push/pop. An eligible cube's vertices are reserved for the GPU instead; anything else runs GeckoLib's own
 * {@code renderCube} unchanged. Without GeckoLib 3 this mixin does not apply.
 */
@Pseudo
@Mixin(targets = "software.bernie.geckolib3.renderers.geo.GeoEntityRenderer", remap = false)
public abstract class GeoEntityRendererGpuMixin {
    @Redirect(method = "renderRecursively", require = 0, remap = false,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lsoftware/bernie/geckolib3/renderers/geo/GeoEntityRenderer;renderCube(Lsoftware/bernie/geckolib3/geo/render/built/GeoCube;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void vro$renderCubeOnGpu(GeoEntityRenderer<?> renderer, GeoCube cube, PoseStack poseStack,
                                     VertexConsumer consumer, int light, int overlay, float red, float green,
                                     float blue, float alpha) {
        if (!GeckoLibGpuModels.tryReserve(renderer, cube, poseStack, consumer, light, overlay, red, green, blue, alpha)) {
            renderer.renderCube(cube, poseStack, consumer, light, overlay, red, green, blue, alpha);
        }
    }
}
