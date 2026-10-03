package dev.hoyin1600p.vault_render_optimization.mixin.entitygpu;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuEntityModels;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Public Oculus 1.6.x batched entity rendering (active even with shaders off) hands out its own builders, so
 * {@link BufferSourceGpuHintMixin} never sees those calls: translucent items and parts were reserved and then
 * filled on the CPU right before sorting. Same hint, same meaning (Oculus' wrapped render types copy
 * {@code sortOnUpload}). Without Oculus this does not apply.
 */
@Pseudo
@Mixin(targets = "net.coderbot.batchedentityrendering.impl.FullyBufferedMultiBufferSource", remap = false)
public abstract class OculusBufferSourceGpuHintMixin {
    @Inject(method = {"getBuffer", "m_6299_"}, at = @At("RETURN"), require = 0, remap = false)
    private void vro$hintSorting(RenderType renderType, CallbackInfoReturnable<VertexConsumer> cir) {
        GpuEntityModels.hintSorting(cir.getReturnValue(), renderType.sortOnUpload);
    }
}
