package dev.hoyin1600p.vault_render_optimization.mixin.entitygpu;

import net.minecraft.client.renderer.SpriteCoordinateExpander;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Adds nothing; its only purpose is the mixin plugin's postApply audit of the final
 * {@code SpriteCoordinateExpander}, whose vertex and uv methods the GPU path skips for block entities.
 */
@Mixin(SpriteCoordinateExpander.class)
public abstract class SpriteCoordinateExpanderGpuAuditMixin {
}
