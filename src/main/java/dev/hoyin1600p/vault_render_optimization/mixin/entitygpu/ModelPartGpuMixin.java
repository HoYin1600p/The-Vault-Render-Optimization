/*
 * Structure adapted from Accelerated Rendering (MIT, Copyright (c) 2023 Argon4W),
 * features/modelparts/mixins/ModelPartMixin.java at 11f149ac716ec757907209dc77b04baaa3f915fc.
 * Modified by HoYin1600p for The Vault Render Optimization (VRO), 2026: 1.18.2 ModelPart, vertices
 * reserved in the vanilla buffer instead of a separate accelerated buffer, no culling.
 */
package dev.hoyin1600p.vault_render_optimization.mixin.entitygpu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuEntityModels;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuHoleBuilder;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces {@code render} for eligible parts with the same steps in the same order - visibility,
 * push, {@code translateAndRotate}, this part's vertices, each child's real {@code render}, pop -
 * except that this part's vertices are reserved for the GPU instead of computed here. The mixin
 * audit refuses the path if any other mod changed {@code render} or {@code compile}, except the
 * reviewed cancel-or-fall-through HEAD hooks of Skin Layers 3D and wildbackport, which must run
 * first. Priority 1100 applies this mixin after default-priority ones so its HEAD callback is
 * placed after theirs.
 */
@Mixin(value = ModelPart.class, priority = 1100)
public abstract class ModelPartGpuMixin {
    @Unique
    private GpuEntityModels.CachedMesh vro$gpuMesh;

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void vro$renderOnGpu(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, float red,
                                 float green, float blue, float alpha, CallbackInfo ci) {
        if (!GpuEntityModels.frameActive() || !RenderSystem.isOnRenderThread()) return;
        GpuHoleBuilder builder;
        TextureAtlasSprite sprite = null;
        if (consumer.getClass() == BufferBuilder.class) {
            builder = (GpuHoleBuilder) consumer;
        } else if (consumer.getClass() == SpriteCoordinateExpander.class && GpuEntityModels.spriteWrappersAllowed()) {
            // Block entities (chests, beds, signs, ...): an atlas sprite remap around the entity buffer.
            SpriteCoordinateExpander wrapper = (SpriteCoordinateExpander) consumer;
            if (wrapper.delegate.getClass() != BufferBuilder.class) return;
            builder = (GpuHoleBuilder) wrapper.delegate;
            sprite = wrapper.sprite;
        } else {
            return;
        }
        ModelPart self = (ModelPart) (Object) this;
        if (!self.visible || (self.cubes.isEmpty() && self.children.isEmpty())) return;
        if (!builder.vro$canReserve()) return;
        GpuEntityModels.CachedMesh mesh = GpuEntityModels.mesh(self, vro$gpuMesh);
        vro$gpuMesh = mesh;
        if (!mesh.gpu()) {
            GpuEntityModels.PARTS_NOT_ELIGIBLE.incrementAndGet();
            return;
        }
        ci.cancel();
        poseStack.pushPose();
        self.translateAndRotate(poseStack);
        if (mesh.mesh().vertexCount() > 0) {
            GpuEntityModels.reserve(builder, mesh, poseStack.last(), light, overlay, red, green, blue, alpha, sprite);
        }
        for (ModelPart child : self.children.values()) {
            child.render(poseStack, consumer, light, overlay, red, green, blue, alpha);
        }
        poseStack.popPose();
    }
}
