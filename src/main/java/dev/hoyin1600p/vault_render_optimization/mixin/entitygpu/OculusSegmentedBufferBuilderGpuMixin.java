package dev.hoyin1600p.vault_render_optimization.mixin.entitygpu;

import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuEntityModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Public Oculus 1.6.x batched entity rendering: marks the window in which its SegmentedBufferBuilder
 * pops each render type's slice, so reserved vertices are parked for the later draw of that slice
 * instead of being filled on the CPU. Without Oculus (or with a different layout) this does not
 * apply and every such pop is filled on the CPU exactly as before.
 */
@Pseudo
@Mixin(targets = "net.coderbot.batchedentityrendering.impl.SegmentedBufferBuilder", remap = false)
public abstract class OculusSegmentedBufferBuilderGpuMixin {
    @Inject(method = "getSegments", at = @At("HEAD"), require = 0, remap = false)
    private void vro$beginSegmentPops(CallbackInfoReturnable<?> cir) {
        GpuEntityModels.beginSegmentPops();
    }

    @Inject(method = "getSegments", at = @At("RETURN"), require = 0, remap = false)
    private void vro$endSegmentPops(CallbackInfoReturnable<?> cir) {
        GpuEntityModels.endSegmentPops();
    }
}
