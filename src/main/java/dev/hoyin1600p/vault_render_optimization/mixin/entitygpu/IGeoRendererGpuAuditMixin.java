package dev.hoyin1600p.vault_render_optimization.mixin.entitygpu;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * Adds nothing; its only purpose is the mixin plugin's postApply audit of the final GeckoLib 3
 * {@code IGeoRenderer}, whose {@code renderCube} and {@code createVerticesOfQuad} bodies the GPU path skips
 * for eligible cubes. Another mod's change to either keeps GeckoLib models on the CPU.
 */
@Pseudo
@Mixin(targets = "software.bernie.geckolib3.renderers.geo.IGeoRenderer", remap = false)
public interface IGeoRendererGpuAuditMixin {
}
