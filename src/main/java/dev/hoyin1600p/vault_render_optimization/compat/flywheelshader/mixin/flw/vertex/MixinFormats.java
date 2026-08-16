package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.mixin.flw.vertex;

import com.jozufozu.flywheel.core.Formats;
import com.jozufozu.flywheel.core.vertex.BlockVertex;
import com.jozufozu.flywheel.core.vertex.PosTexNormalVertex;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.vertex.block.ExtendedBlockVertex;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.vertex.postexnormal.ExtendedPosTexNormalVertex;

/**
 * Mixin to replace the default vertex formats with the extended formats.
 */
@Mixin(Formats.class)
public class MixinFormats {

    @SuppressWarnings("ShadowTarget")
    @Final
    @Shadow(remap = false)
    public static BlockVertex BLOCK = new ExtendedBlockVertex();

    @SuppressWarnings("ShadowTarget")
    @Final
    @Shadow(remap = false)
    public static final PosTexNormalVertex POS_TEX_NORMAL = new ExtendedPosTexNormalVertex();
}
