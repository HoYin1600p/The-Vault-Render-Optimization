package dev.hoyin1600p.vault_render_optimization.mixin.sophisticatedstorage;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage.BarrelFrontVisibility;
import net.minecraft.client.renderer.MultiBufferSource;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.p3pp3rf1y.sophisticatedstorage.client.render.BarrelRenderer", remap = false)
public abstract class BarrelRendererMixin {
    @Inject(
            method = "renderFrontFace(Lnet/p3pp3rf1y/sophisticatedstorage/block/BarrelBlockEntity;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IIZ)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private void vro$cullHiddenFrontDisplay(
            BarrelBlockEntity barrel,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            boolean flatTop,
            CallbackInfo ci
    ) {
        if (!BarrelFrontVisibility.shouldRender(barrel)) {
            ci.cancel();
        }
    }
}
