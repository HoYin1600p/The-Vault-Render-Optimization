package dev.hoyin1600p.vault_render_optimization.mixin;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderRegistry;
import com.jozufozu.flywheel.config.BackendType;
import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.render.BlockEntityRenderHelper;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.RegisteredObjects;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.hoyin1600p.vault_render_optimization.client.create.CreateRenderContext;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Iterator;

@Mixin(value = BlockEntityRenderHelper.class, remap = false)
public abstract class CreateBlockEntityRenderHelperMixin {
    private static final ThreadLocal<BlockPos.MutableBlockPos> VRO_LIGHT_POSITION =
            ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    @Inject(
            method = "renderBlockEntities(Lnet/minecraft/world/level/Level;Lcom/jozufozu/flywheel/core/virtual/VirtualRenderWorld;Ljava/lang/Iterable;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;F)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void vro$renderVirtualBlockEntities(Level world,
                                                        @Nullable VirtualRenderWorld renderWorld,
                                                        Iterable<BlockEntity> blockEntities,
                                                        PoseStack poseStack,
                                                        @Nullable Matrix4f lightTransform,
                                                        MultiBufferSource buffers,
                                                        float partialTick,
                                                        CallbackInfo ci) {
        if (!ClientOptimizationConfig.optimizationsEnabled()) {
            return;
        }

        boolean instancedWorld = Backend.getBackendType() == BackendType.INSTANCING
                && Backend.isFlywheelWorld(renderWorld);
        Iterator<BlockEntity> iterator = blockEntities.iterator();
        while (iterator.hasNext()) {
            BlockEntity blockEntity = iterator.next();
            if (instancedWorld && InstancedRenderRegistry.shouldSkipRender(blockEntity)) {
                continue;
            }

            BlockEntityRenderer<BlockEntity> renderer = Minecraft.getInstance()
                    .getBlockEntityRenderDispatcher()
                    .getRenderer(blockEntity);
            if (renderer == null) {
                iterator.remove();
                continue;
            }

            BlockPos localPos = blockEntity.getBlockPos();
            if (ClientOptimizationConfig.createBlockEntityCulling
                    && renderWorld != null
                    && lightTransform != null
                    && !renderer.shouldRenderOffScreen(blockEntity)) {
                AABB localBounds = new AABB(localPos).inflate(1.5);
                boolean visible = CreateRenderContext.isVisible(localBounds, lightTransform);
                CreateRenderContext.recordBlockEntity(!visible);
                if (!visible) {
                    continue;
                }
            }

            poseStack.pushPose();
            TransformStack.cast(poseStack).translate(localPos);
            Level originalLevel = blockEntity.getLevel();
            try {
                BlockPos worldLightPos = localPos;
                if (lightTransform != null) {
                    worldLightPos = CreateRenderContext.transformCenter(
                            lightTransform,
                            localPos,
                            VRO_LIGHT_POSITION.get()
                    );
                }
                int packedLight = LevelRenderer.getLightColor(world, worldLightPos);
                if (renderWorld != null) {
                    packedLight = SuperByteBuffer.maxLight(
                            packedLight,
                            LevelRenderer.getLightColor(renderWorld, localPos)
                    );
                    blockEntity.setLevel(renderWorld);
                }
                renderer.render(
                        blockEntity,
                        partialTick,
                        poseStack,
                        buffers,
                        packedLight,
                        OverlayTexture.NO_OVERLAY
                );
            } catch (Exception exception) {
                iterator.remove();
                String message = "BlockEntity " + RegisteredObjects.getKeyOrThrow(blockEntity.getType())
                        + " could not be rendered virtually.";
                if (AllConfigs.client().explainRenderErrors.get()) {
                    Create.LOGGER.error(message, exception);
                } else {
                    Create.LOGGER.error(message);
                }
            } finally {
                if (renderWorld != null) {
                    blockEntity.setLevel(originalLevel != null ? originalLevel : world);
                }
                poseStack.popPose();
            }
        }
        ci.cancel();
    }
}
