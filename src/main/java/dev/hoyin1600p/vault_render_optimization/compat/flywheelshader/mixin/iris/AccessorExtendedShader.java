package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.mixin.iris;

import net.coderbot.iris.gl.framebuffer.GlFramebuffer;
import net.coderbot.iris.pipeline.newshader.ExtendedShader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ExtendedShader.class,remap = false)
public interface AccessorExtendedShader {
    @Accessor
    GlFramebuffer getWritingToBeforeTranslucent();
}
