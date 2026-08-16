package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.mixin.flw;

import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShaderInstance.class)
public interface ShaderInstanceAccessor {
    @Mutable
    @Accessor("MODEL_VIEW_MATRIX")
    void vroFlywheel$setModelViewMatrix(Uniform uniform);
}
