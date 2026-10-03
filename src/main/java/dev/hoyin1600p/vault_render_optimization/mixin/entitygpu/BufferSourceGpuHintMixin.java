package dev.hoyin1600p.vault_render_optimization.mixin.entitygpu;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuEntityModels;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Marks the builder vanilla's buffer source hands out with whether its render type sorts quads on
 * upload, so translucent parts (players, for example) skip a reservation that would only be filled
 * on the CPU again. Purely an efficiency hint: without it the holes are still filled exactly.
 */
@Mixin(MultiBufferSource.BufferSource.class)
public abstract class BufferSourceGpuHintMixin {
    @Inject(method = "getBuffer", at = @At("RETURN"), require = 0)
    private void vro$hintSorting(RenderType renderType, CallbackInfoReturnable<VertexConsumer> cir) {
        GpuEntityModels.hintSorting(cir.getReturnValue(), renderType.sortOnUpload);
    }
}
