package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader;

import com.jozufozu.flywheel.backend.Backend;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;

public final class FlywheelShaderCompatState {
    private static Object pipeline;
    private static boolean failed;
    private static boolean fallbackPending;
    private static boolean successLogged;

    private FlywheelShaderCompatState() {
    }

    public static synchronized void beginPipeline(Object newPipeline) {
        if (pipeline == newPipeline) {
            return;
        }
        pipeline = newPipeline;
        failed = false;
        fallbackPending = false;
        successLogged = false;
        VroFlywheelShaderCompat.LOGGER.info("Flywheel shader instancing compatibility available for the new Oculus pipeline");
    }

    public static synchronized void endPipeline(Object oldPipeline) {
        if (pipeline != oldPipeline) {
            return;
        }
        pipeline = null;
        failed = false;
        fallbackPending = false;
        successLogged = false;
    }

    public static synchronized boolean shouldUseShaderInstancing() {
        return featureEnabled() && !failed && VroFlywheelShaderCompat.isShaderPackInUse();
    }

    public static synchronized boolean isRenderPathActive() {
        return featureEnabled()
                && pipeline != null
                && !failed
                && Backend.isOn()
                && VroFlywheelShaderCompat.isShaderPackInUse();
    }

    public static synchronized boolean hasPipeline() {
        return pipeline != null;
    }

    public static synchronized boolean hasFailed() {
        return failed;
    }

    public static synchronized void resetForConfigurationChange() {
        failed = false;
        fallbackPending = false;
        successLogged = false;
    }

    public static synchronized void recordSuccess() {
        if (!successLogged) {
            successLogged = true;
            VroFlywheelShaderCompat.LOGGER.info("Flywheel shader program integration succeeded");
        }
    }

    public static synchronized void recordFailure(String program, Throwable throwable) {
        if (failed) {
            return;
        }
        failed = true;
        fallbackPending = true;
        if (throwable == null) {
            VroFlywheelShaderCompat.LOGGER.error("Flywheel shader program integration failed for {}; falling back to the standard Create renderer", program);
        } else {
            VroFlywheelShaderCompat.LOGGER.error("Flywheel shader program integration failed for {}; falling back to the standard Create renderer", program, throwable);
        }
    }

    public static synchronized void applyPendingFallback() {
        if (!fallbackPending) {
            return;
        }
        fallbackPending = false;
        Backend.refresh();
        Backend.reloadWorldRenderers();
    }

    private static boolean featureEnabled() {
        return ClientOptimizationConfig.optimizationsEnabled()
                && ClientOptimizationConfig.createFlywheelShaderCompat;
    }
}
