package dev.hoyin1600p.vault_render_optimization.client.compat.manastealer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import java.util.Collection;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

final class ManaStealerDrainStreamRenderer {
    private static final double UNIT_DOUBLE = 0x1.0p-53;
    private static final RenderType STREAM_RENDER_TYPE = RenderType.entityTranslucent(TextureAtlas.LOCATION_PARTICLES);

    private ManaStealerDrainStreamRenderer() {
    }

    static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || !ManaStealerVisualConfig.drainStreamActive()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        TextureAtlasSprite sprite = ManaStealerOrbParticle.streamSprite();
        Collection<ManaStealerDrainStreamController.StreamState> streams =
                ManaStealerDrainStreamController.streams();
        if (level == null || sprite == null || streams.isEmpty()) {
            return;
        }

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(STREAM_RENDER_TYPE);
        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        float partialTick = event.getPartialTick();
        double animationTicks = level.getGameTime() + partialTick;
        int quality = quality(minecraft.options.particles);
        boolean rendered = false;

        for (ManaStealerDrainStreamController.StreamState stream : streams) {
            if (stream.level() != level) {
                continue;
            }
            Player player = level.getPlayerByUUID(stream.playerId());
            if (player == null) {
                continue;
            }
            double playerX = Mth.lerp(partialTick, player.xo, player.getX());
            double playerY = Mth.lerp(partialTick, player.yo, player.getY());
            double playerZ = Mth.lerp(partialTick, player.zo, player.getZ());
            Vec3 destination = stream.center();
            if (!ManaStealerDrainStreamPolicy.insideSphere(
                    playerX,
                    playerY,
                    playerZ,
                    destination.x,
                    destination.y,
                    destination.z,
                    stream.radius()
            )) {
                continue;
            }
            Vec3 origin = player.getEyePosition(partialTick).add(0.0D, -0.45D, 0.0D);
            Vec3 path = destination.subtract(origin);
            double distance = path.length();
            int orbCount = ManaStealerDrainStreamPolicy.visibleOrbCount(
                    distance,
                    ManaStealerVisualConfig.drainStreamDensity(),
                    ManaStealerVisualConfig.drainStreamMinimumOrbs(),
                    ManaStealerVisualConfig.drainStreamMaximumOrbs(),
                    quality
            );
            if (orbCount <= 0) {
                continue;
            }

            Vec3 direction = path.scale(1.0D / distance);
            Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (side.lengthSqr() < 1.0E-6D) {
                side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
            }
            side = side.normalize();
            Vec3 vertical = side.cross(direction).normalize();
            double normalizedTravel = animationTicks * ManaStealerVisualConfig.drainStreamSpeed() / distance;

            for (int slot = 0; slot < orbCount; slot++) {
                double unwrapped = normalizedTravel + (double) slot / (double) orbCount;
                long cycle = (long) Math.floor(unwrapped);
                double progress = ManaStealerDrainStreamPolicy.progress(normalizedTravel, slot, orbCount);
                long first = mix64(stream.visualSeed() ^ (long) slot * 0x9E3779B97F4A7C15L ^ cycle);
                long second = mix64(first);
                double angle = unit(first) * Math.PI * 2.0D + progress * Math.PI * 3.0D;
                double spreadEnvelope = Math.sin(Math.PI * progress);
                double spread = ManaStealerVisualConfig.drainStreamSpread()
                        * Math.fma(unit(second), 0.55D, 0.45D)
                        * spreadEnvelope;
                double sideOffset = Math.cos(angle) * spread;
                double verticalOffset = Math.sin(angle) * spread;
                double positionX = Mth.lerp(progress, origin.x, destination.x)
                        + side.x * sideOffset + vertical.x * verticalOffset;
                double positionY = Mth.lerp(progress, origin.y, destination.y)
                        + side.y * sideOffset + vertical.y * verticalOffset;
                double positionZ = Mth.lerp(progress, origin.z, destination.z)
                        + side.z * sideOffset + vertical.z * verticalOffset;

                float endpointEnvelope = endpointEnvelope(progress);
                float diameter = ManaStealerVisualConfig.drainStreamOrbDiameter()
                        * (float) Math.fma(unit(mix64(second)), 0.30D, 0.85D)
                        * Math.fma(0.30F, endpointEnvelope, 0.70F);
                float alpha = endpointEnvelope;
                renderOrb(
                        poseStack,
                        consumer,
                        camera,
                        cameraPosition,
                        sprite,
                        positionX,
                        positionY,
                        positionZ,
                        diameter,
                        ManaStealerVisualConfig.innerRatio(),
                        alpha
                );
                rendered = true;
            }
        }

        if (rendered) {
            buffers.endBatch(STREAM_RENDER_TYPE);
        }
    }

    private static void renderOrb(
            PoseStack poseStack,
            VertexConsumer consumer,
            Camera camera,
            Vec3 cameraPosition,
            TextureAtlasSprite sprite,
            double positionX,
            double positionY,
            double positionZ,
            float diameter,
            float innerRatio,
            float alpha
    ) {
        poseStack.pushPose();
        poseStack.translate(
                positionX - cameraPosition.x,
                positionY - cameraPosition.y,
                positionZ - cameraPosition.z
        );
        poseStack.mulPose(camera.rotation());
        PoseStack.Pose pose = poseStack.last();
        float outerRadius = diameter * 0.5F;
        writeLayer(
                pose,
                consumer,
                sprite,
                outerRadius,
                ManaStealerOrbParticle.OUTER_RED,
                ManaStealerOrbParticle.OUTER_GREEN,
                ManaStealerOrbParticle.OUTER_BLUE,
                ManaStealerOrbParticle.OUTER_ALPHA * alpha,
                0.0F
        );
        writeLayer(
                pose,
                consumer,
                sprite,
                outerRadius * innerRatio,
                ManaStealerOrbParticle.INNER_RED,
                ManaStealerOrbParticle.INNER_GREEN,
                ManaStealerOrbParticle.INNER_BLUE,
                ManaStealerOrbParticle.INNER_ALPHA * alpha,
                -ManaStealerOrbParticle.INNER_DEPTH_BIAS
        );
        poseStack.popPose();
    }

    private static void writeLayer(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            TextureAtlasSprite sprite,
            float radius,
            float red,
            float green,
            float blue,
            float alpha,
            float depthOffset
    ) {
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        writeVertex(matrix, normal, consumer, -radius, -radius, depthOffset,
                sprite.getU1(), sprite.getV1(), red, green, blue, alpha);
        writeVertex(matrix, normal, consumer, radius, -radius, depthOffset,
                sprite.getU0(), sprite.getV1(), red, green, blue, alpha);
        writeVertex(matrix, normal, consumer, radius, radius, depthOffset,
                sprite.getU0(), sprite.getV0(), red, green, blue, alpha);
        writeVertex(matrix, normal, consumer, -radius, radius, depthOffset,
                sprite.getU1(), sprite.getV0(), red, green, blue, alpha);
    }

    private static void writeVertex(
            Matrix4f matrix,
            Matrix3f normal,
            VertexConsumer consumer,
            float x,
            float y,
            float z,
            float u,
            float v,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        consumer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, Vector3f.ZP.x(), Vector3f.ZP.y(), Vector3f.ZP.z())
                .endVertex();
    }

    private static float endpointEnvelope(double progress) {
        float fadeIn = Mth.clamp((float) (progress / 0.08D), 0.0F, 1.0F);
        float fadeOut = Mth.clamp((float) ((1.0D - progress) / 0.06D), 0.0F, 1.0F);
        return smooth(Math.min(fadeIn, fadeOut));
    }

    private static float smooth(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static int quality(ParticleStatus status) {
        return switch (status) {
            case DECREASED -> ManaStealerDrainStreamPolicy.DECREASED;
            case MINIMAL -> ManaStealerDrainStreamPolicy.MINIMAL;
            default -> ManaStealerDrainStreamPolicy.ALL;
        };
    }

    private static double unit(long value) {
        return (value >>> 11) * UNIT_DOUBLE;
    }

    private static long mix64(long value) {
        value += 0x9E3779B97F4A7C15L;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
