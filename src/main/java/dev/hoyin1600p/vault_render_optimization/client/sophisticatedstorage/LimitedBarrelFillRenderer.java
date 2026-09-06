package dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.client.render.LimitedBarrelRenderer;

/**
 * Allocation-light adaptation of Sophisticated Storage's 1.18.2 fill-bar
 * geometry. Upstream revision 891b0d9e29350ec78c7b8567285036d2ecdb303c,
 * GPL-3.0-only; direct vertex emission is a VRO change.
 */
public final class LimitedBarrelFillRenderer {
    private static final float PIXEL = 1.0F / 16.0F;
    private static final float BAR_PIXEL = PIXEL / 5.0F;
    private static final float BAR_WIDTH = BAR_PIXEL * 3.0F;
    private static final float MIN_U = 0.0F;
    private static final float MAX_U = 3.0F / 128.0F;
    private static final float LARGE_MAX_V = 68.0F / 128.0F;
    private static final float SMALL_MAX_V = 28.0F / 128.0F;

    private LimitedBarrelFillRenderer() {
    }

    public static void render(
            LimitedBarrelBlockEntity barrel,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        List<Float> fillLevels = barrel.getSlotFillLevels();
        int slots = fillLevels.size();
        if (slots < 1 || slots > 4) {
            return;
        }

        boolean translucent = !barrel.shouldShowFillLevels();
        VertexConsumer consumer;
        if (translucent) {
            TextureAtlasSprite sprite = LimitedBarrelRenderer.FILL_INDICATORS_TEXTURE.sprite();
            consumer = sprite.wrap(bufferSource.getBuffer(RenderType.entityTranslucent(sprite.atlas().location())));
        } else {
            consumer = LimitedBarrelRenderer.FILL_INDICATORS_TEXTURE.buffer(
                    bufferSource,
                    RenderType::entityCutoutNoCull
            );
        }

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, -0.001D);
        switch (slots) {
            case 1 -> renderBar(poseStack, consumer, packedLight, packedOverlay,
                    fillLevels.get(0), PIXEL, PIXEL, true, translucent);
            case 2 -> {
                renderBar(poseStack, consumer, packedLight, packedOverlay,
                        fillLevels.get(0), PIXEL, 9.0F * PIXEL, false, translucent);
                renderBar(poseStack, consumer, packedLight, packedOverlay,
                        fillLevels.get(1), PIXEL, PIXEL, false, translucent);
            }
            case 3 -> {
                renderBar(poseStack, consumer, packedLight, packedOverlay,
                        fillLevels.get(0), PIXEL, 9.0F * PIXEL, false, translucent);
                renderBar(poseStack, consumer, packedLight, packedOverlay,
                        fillLevels.get(1), 14.0F * PIXEL, PIXEL, false, translucent);
                renderBar(poseStack, consumer, packedLight, packedOverlay,
                        fillLevels.get(2), PIXEL, PIXEL, false, translucent);
            }
            case 4 -> {
                renderBar(poseStack, consumer, packedLight, packedOverlay,
                        fillLevels.get(0), 14.0F * PIXEL, 9.0F * PIXEL, false, translucent);
                renderBar(poseStack, consumer, packedLight, packedOverlay,
                        fillLevels.get(1), PIXEL, 9.0F * PIXEL, false, translucent);
                renderBar(poseStack, consumer, packedLight, packedOverlay,
                        fillLevels.get(2), 14.0F * PIXEL, PIXEL, false, translucent);
                renderBar(poseStack, consumer, packedLight, packedOverlay,
                        fillLevels.get(3), PIXEL, PIXEL, false, translucent);
            }
            default -> {
            }
        }
        poseStack.popPose();
    }

    private static void renderBar(
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay,
            float fillLevel,
            float x,
            float y,
            boolean large,
            boolean translucent
    ) {
        float clampedFill = Math.max(0.0F, Math.min(1.0F, fillLevel));
        float barHeightPixels = large ? 14.0F : 6.0F;
        float height = clampedFill * BAR_PIXEL * (barHeightPixels * 5.0F - 2.0F);
        float maxV = large ? LARGE_MAX_V : SMALL_MAX_V;
        float minV = (1.0F - clampedFill) * maxV;
        float alpha = translucent ? 0.5F : 1.0F;

        poseStack.pushPose();
        poseStack.translate(x + BAR_PIXEL, y + BAR_PIXEL, 0.0D);
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, 0.0F, height, MAX_U, minV, alpha, packedOverlay, packedLight);
        vertex(consumer, pose, 0.0F, 0.0F, MAX_U, maxV, alpha, packedOverlay, packedLight);
        vertex(consumer, pose, BAR_WIDTH, 0.0F, MIN_U, maxV, alpha, packedOverlay, packedLight);
        vertex(consumer, pose, BAR_WIDTH, height, MIN_U, minV, alpha, packedOverlay, packedLight);
        poseStack.popPose();
        SophisticatedStorageDiagnostics.recordFillBarRendered();
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float u,
            float v,
            float alpha,
            int packedOverlay,
            int packedLight
    ) {
        consumer.vertex(pose.pose(), x, y, 0.0F)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(u, v)
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
