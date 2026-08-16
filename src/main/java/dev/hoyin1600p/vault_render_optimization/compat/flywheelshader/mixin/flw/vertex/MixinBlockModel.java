package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.mixin.flw.vertex;

import com.jozufozu.flywheel.core.model.BlockModel;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.coderbot.iris.vertices.IrisVertexFormats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.VroFlywheelShaderCompat;

@Mixin(value = BlockModel.class)
public class MixinBlockModel {
    @Redirect(method = "<init>*", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferBuilder$DrawState;format()Lcom/mojang/blaze3d/vertex/VertexFormat;"))
    private VertexFormat irisFlw$ReturnIrisFormat(BufferBuilder.DrawState drawState) {
        // If we are using extended vertex format and the drawState format is TERRAIN, we must return the BLOCK format to avoid BlockModel throwing an exception.
        if (VroFlywheelShaderCompat.isUsingExtendedVertexFormat() && drawState.format() == IrisVertexFormats.TERRAIN) {
            return DefaultVertexFormat.BLOCK;
        }
        return drawState.format();
    }
}
