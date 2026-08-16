package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.mixin.flw;

import com.jozufozu.flywheel.backend.gl.GlObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = GlObject.class, remap = false)
public interface GlObjectAccessor {
    @Accessor("handle")
    void vroFlywheel$replaceHandle(int handle);
}
