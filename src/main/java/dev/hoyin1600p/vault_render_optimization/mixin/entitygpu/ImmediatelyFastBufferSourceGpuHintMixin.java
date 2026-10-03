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
 * The ImmediatelyFast mod (not VRO's built-in port, which hints itself) replaces vanilla's immediate buffer source
 * with its ImmediateAdapter, whose {@code getBuffer} never reaches {@link BufferSourceGpuHintMixin}. Without the
 * hint, translucent parts were reserved and then filled on the CPU right before its sort. Without the mod this
 * does not apply.
 */
@Pseudo
@Mixin(targets = "net.raphimc.immediatelyfast.feature.core.ImmediateAdapter", remap = false)
public abstract class ImmediatelyFastBufferSourceGpuHintMixin {
    @Inject(method = {"getBuffer", "m_6299_"}, at = @At("RETURN"), require = 0, remap = false)
    private void vro$hintSorting(RenderType renderType, CallbackInfoReturnable<VertexConsumer> cir) {
        GpuEntityModels.hintSorting(cir.getReturnValue(), renderType.sortOnUpload);
    }
}
