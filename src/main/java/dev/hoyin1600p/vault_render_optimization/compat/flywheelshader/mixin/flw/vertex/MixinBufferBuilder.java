package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.mixin.flw.vertex;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.VroFlywheelShaderCompat;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.accessors.BufferBuilderAccessor;

@Mixin(value = BufferBuilder.class, priority = 1010)
public class MixinBufferBuilder implements BufferBuilderAccessor {

    @SuppressWarnings("unused")
    @Unique
    private boolean irisFlw$forceBlockFormat;

    // This field is from Iris's mixin.
    private boolean extending;

    @Override
    public void vroFlywheel$setForceBlockFormat(boolean isFlyWheel) {
        this.irisFlw$forceBlockFormat = isFlyWheel;
    }

    @ModifyVariable(method = "begin", at = @At("HEAD"), argsOnly = true)
    private VertexFormat vroFlywheel$begin(VertexFormat format) {
        // If forceBlockFormat is true, we set the format to BLOCK.
        // Notice that we only handle the format when shader pack is in use.
        if (irisFlw$forceBlockFormat && VroFlywheelShaderCompat.isUsingExtendedVertexFormat())
        {
            extending = false;
            return DefaultVertexFormat.BLOCK;
        }

        // If we are using extended vertex format, we should leave the format as it is.
        // Since the Iris's mixin will handle the format.
        return format;
    }
}
