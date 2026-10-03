package dev.hoyin1600p.vault_render_optimization.mixin.entitygpu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.geckolib.GeckoLibGpuModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import software.bernie.geckolib3.geo.render.built.GeoCube;
import software.bernie.geckolib3.renderers.geo.IGeoRenderer;

/**
 * GeckoLib 3 block renderers reach their cubes through {@code IGeoRenderer.renderCubesOfBone}, an interface default
 * method (Mixin cannot inject there) that calls {@code renderCube} once per cube between its own push and pop. This
 * adds a {@code renderCube} override to {@code GeoBlockRenderer}: an eligible cube is reserved for the GPU exactly as
 * for entities, anything else runs GeckoLib's own default unchanged. Subclasses that declare their own
 * {@code renderCube} still win (and are left to the CPU). Without GeckoLib 3 this mixin does not apply.
 */
@Pseudo
@Mixin(targets = "software.bernie.geckolib3.renderers.geo.GeoBlockRenderer", remap = false)
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class GeoBlockRendererGpuMixin implements IGeoRenderer {
    @Override
    public void renderCube(GeoCube cube, PoseStack poseStack, VertexConsumer consumer, int light, int overlay, float red,
                           float green, float blue, float alpha) {
        if (!GeckoLibGpuModels.tryReserve((IGeoRenderer<?>) this, cube, poseStack, consumer, light, overlay, red, green,
                blue, alpha)) {
            IGeoRenderer.super.renderCube(cube, poseStack, consumer, light, overlay, red, green, blue, alpha);
        }
    }
}
