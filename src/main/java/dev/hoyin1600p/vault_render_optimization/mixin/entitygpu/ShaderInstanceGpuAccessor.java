package dev.hoyin1600p.vault_render_optimization.mixin.entitygpu;

import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the GPU entity path make the next {@code ShaderInstance.apply()} rebind its program. */
@Mixin(ShaderInstance.class)
public interface ShaderInstanceGpuAccessor {
    @Accessor("lastProgramId")
    static void vro$setLastProgramId(int programId) {
        throw new AssertionError();
    }
}
