package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.shader.UniformLocationCache;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "iskallia.vault.client.shader.glsl.NativeShader", remap = false)
public abstract class VaultNativeShaderUniformMixin {
    @Shadow
    private int loadedShader;

    @Unique private UniformLocationCache vro$uniformLocations;

    @Inject(method = {"loadShader", "destroy"}, at = @At("HEAD"), remap = false, require = 2)
    private void vro$invalidateUniformLocations(CallbackInfo ci) {
        // loadShader is called from the target constructor: initialize lazily.
        if (vro$uniformLocations != null) vro$uniformLocations.clear();
    }

    @Inject(method = "applyFloatValue", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void vault_render_optimization$bindProgramForFloatUniform(
            String uniformName,
            float value,
            CallbackInfo ci
    ) {
        if (loadedShader == -1) {
            ci.cancel();
            return;
        }

        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        boolean restoreProgram = previousProgram != loadedShader;
        if (restoreProgram) {
            GL20.glUseProgram(loadedShader);
        }

        try {
            if (vro$uniformLocations == null) vro$uniformLocations = new UniformLocationCache();
            int location = vro$uniformLocations.get(loadedShader, uniformName,
                    (program, name) -> GL20.glGetUniformLocation(program, name));
            GL20.glUniform1f(location, value);
        } finally {
            if (restoreProgram) {
                GL20.glUseProgram(previousProgram);
            }
        }

        ci.cancel();
    }
}
