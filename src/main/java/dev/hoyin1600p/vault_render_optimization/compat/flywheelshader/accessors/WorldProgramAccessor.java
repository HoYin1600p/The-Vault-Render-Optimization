package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.accessors;

import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.flywheel.IrisFlwCompatShaderWarp;

public interface WorldProgramAccessor {
    void setShader(IrisFlwCompatShaderWarp shader);

    void instanceUnbind();
}
