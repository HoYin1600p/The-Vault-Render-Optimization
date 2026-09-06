package dev.hoyin1600p.vault_render_optimization.mixin.sophisticatedstorage;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage.BarrelFrontVisibility;
import dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage.LimitedBarrelCountRenderer;
import dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage.LimitedBarrelFillRenderer;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.VerticalFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.p3pp3rf1y.sophisticatedstorage.client.render.LimitedBarrelRenderer", remap = false)
public abstract class LimitedBarrelRendererMixin {
    @Unique
    private boolean vro$frontDisplayVisible = true;

    @Inject(
            method = "render(Lnet/p3pp3rf1y/sophisticatedstorage/block/LimitedBarrelBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"),
            require = 1
    )
    private void vro$prepareFrontVisibility(
            LimitedBarrelBlockEntity barrel,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            CallbackInfo ci
    ) {
        vro$frontDisplayVisible = BarrelFrontVisibility.shouldRender(barrel);
    }

    @Inject(
            method = "renderFrontFace(Lnet/p3pp3rf1y/sophisticatedstorage/block/LimitedBarrelBlockEntity;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/world/level/block/state/BlockState;ZLnet/minecraft/core/Direction;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private void vro$cullHiddenFrontDisplay(
            LimitedBarrelBlockEntity barrel,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            BlockState state,
            boolean flatTop,
            Direction horizontalFacing,
            CallbackInfo ci
    ) {
        if (!vro$frontDisplayVisible) {
            ci.cancel();
        }
    }

    @Inject(
            method = "renderItemCounts(Lnet/p3pp3rf1y/sophisticatedstorage/block/LimitedBarrelBlockEntity;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IZLnet/minecraft/core/Direction;Lnet/p3pp3rf1y/sophisticatedstorage/block/VerticalFacing;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private void vro$renderCachedCounts(
            LimitedBarrelBlockEntity barrel,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            boolean flatTop,
            Direction horizontalFacing,
            VerticalFacing verticalFacing,
            CallbackInfo ci
    ) {
        if (!vro$frontDisplayVisible) {
            ci.cancel();
            return;
        }
        if (ClientOptimizationConfig.optimizationsEnabled()
                && ClientOptimizationConfig.sophisticatedStorageCountCache) {
            LimitedBarrelCountRenderer.render(
                    barrel, poseStack, bufferSource, packedLight, flatTop, horizontalFacing, verticalFacing
            );
            ci.cancel();
        }
    }

    @Inject(
            method = "renderFillLevels(Lnet/p3pp3rf1y/sophisticatedstorage/block/LimitedBarrelBlockEntity;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private void vro$renderFillLevelsWithoutTemporaryVertices(
            LimitedBarrelBlockEntity barrel,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            CallbackInfo ci
    ) {
        if (ClientOptimizationConfig.optimizationsEnabled()
                && ClientOptimizationConfig.sophisticatedStorageFillFastPath) {
            LimitedBarrelFillRenderer.render(barrel, poseStack, bufferSource, packedLight, packedOverlay);
            ci.cancel();
        }
    }
}
