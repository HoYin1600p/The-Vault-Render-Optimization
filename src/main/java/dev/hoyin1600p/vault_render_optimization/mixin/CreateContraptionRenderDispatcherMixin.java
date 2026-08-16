package dev.hoyin1600p.vault_render_optimization.mixin;

import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.jozufozu.flywheel.event.BeginFrameEvent;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllMovementBehaviours;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.contraptions.render.ContraptionRenderDispatcher;
import dev.hoyin1600p.vault_render_optimization.client.create.CreateRenderContext;
import dev.hoyin1600p.vault_render_optimization.client.create.SectionedContraptionRenderer;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContraptionRenderDispatcher.class, remap = false)
public abstract class CreateContraptionRenderDispatcherMixin {
    @Inject(method = "beginFrame", at = @At("HEAD"), remap = false)
    private static void vro$captureFrameFrustum(BeginFrameEvent event, CallbackInfo ci) {
        CreateRenderContext.beginFrame(event.getFrustum());
    }

    @Redirect(
            method = "renderFromEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V",
                    remap = true
            ),
            remap = false
    )
    private static void vro$skipEmptySpecialBlockEntityFlush(MultiBufferSource.BufferSource buffers,
                                                              AbstractContraptionEntity entity,
                                                              Contraption contraption,
                                                              MultiBufferSource originalBuffers) {
        if (ClientOptimizationConfig.optimizationsEnabled()
                && ClientOptimizationConfig.createEmptyBufferFlushSkip
                && contraption.getSpecialRenderedBEs().isEmpty()) {
            CreateRenderContext.recordEmptyFlushSkipped();
            return;
        }
        buffers.endBatch();
    }

    @Inject(method = "renderActors", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vro$renderVisibleActors(Level world,
                                                 VirtualRenderWorld renderWorld,
                                                 Contraption contraption,
                                                 ContraptionMatrices matrices,
                                                 MultiBufferSource buffers,
                                                 CallbackInfo ci) {
        if (!ClientOptimizationConfig.optimizationsEnabled()
                || !ClientOptimizationConfig.createActorCulling) {
            return;
        }

        PoseStack poseStack = matrices.getModel();
        for (Pair<StructureTemplate.StructureBlockInfo, MovementContext> actor : contraption.getActors()) {
            MovementContext context = actor.getRight();
            if (context == null) {
                continue;
            }
            if (context.world == null) {
                context.world = world;
            }

            StructureTemplate.StructureBlockInfo blockInfo = actor.getLeft();
            MovementBehaviour behaviour = AllMovementBehaviours.getBehaviour(blockInfo.state);
            if (behaviour == null || contraption.isHiddenInPortal(blockInfo.pos)) {
                continue;
            }

            boolean visible = CreateRenderContext.isVisible(
                    new AABB(blockInfo.pos).inflate(3),
                    matrices.getLight()
            );
            CreateRenderContext.recordActor(!visible);
            if (!visible) {
                continue;
            }

            poseStack.pushPose();
            TransformStack.cast(poseStack).translate(blockInfo.pos);
            behaviour.renderInContraption(context, renderWorld, matrices, buffers);
            poseStack.popPose();
        }
        ci.cancel();
    }

    @Inject(method = "reset", at = @At("HEAD"), remap = false)
    private static void vro$clearSectionedMeshes(CallbackInfo ci) {
        SectionedContraptionRenderer.clearFallbackMeshes();
    }
}
