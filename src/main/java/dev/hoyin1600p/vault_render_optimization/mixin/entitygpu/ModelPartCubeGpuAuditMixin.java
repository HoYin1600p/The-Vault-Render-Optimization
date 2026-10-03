package dev.hoyin1600p.vault_render_optimization.mixin.entitygpu;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Adds nothing; its only purpose is the mixin plugin's postApply audit of the final
 * {@code ModelPart$Cube}, whose {@code compile} the GPU path skips.
 */
@Mixin(ModelPart.Cube.class)
public abstract class ModelPartCubeGpuAuditMixin {
}
