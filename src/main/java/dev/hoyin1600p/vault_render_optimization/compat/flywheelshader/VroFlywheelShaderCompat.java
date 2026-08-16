package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader;

import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import net.irisshaders.iris.api.v0.IrisApi;
import org.apache.logging.log4j.Logger;

public final class VroFlywheelShaderCompat {
    public static final Logger LOGGER = VaultRenderOptimization.LOGGER;

    private VroFlywheelShaderCompat() {
    }

    public static boolean isShaderPackInUse() {
        return IrisApi.getInstance().isShaderPackInUse();
    }

    public static boolean isUsingExtendedVertexFormat() {
        return FlywheelShaderCompatState.isRenderPathActive();
    }
}
