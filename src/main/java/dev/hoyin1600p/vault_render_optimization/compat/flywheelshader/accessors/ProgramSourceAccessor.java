package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.accessors;

import net.coderbot.iris.gl.blending.BlendModeOverride;
import net.coderbot.iris.shaderpack.ShaderProperties;

public interface ProgramSourceAccessor {
    ShaderProperties getShaderProperties();

    BlendModeOverride getBlendModeOverride();
}
