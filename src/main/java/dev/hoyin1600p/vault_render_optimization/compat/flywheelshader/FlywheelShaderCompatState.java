package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader;

import com.jozufozu.flywheel.backend.Backend;
import net.coderbot.iris.block_rendering.BlockRenderingSettings;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;

public final class FlywheelShaderCompatState {
    private static Object pipeline;
    private static boolean failed;
    private static boolean fallbackPending;
    private static boolean successLogged;
    private static boolean dedicatedGbuffersUsed;
    private static boolean generatedGbuffersUsed;
    private static boolean dedicatedGbuffersFailed;
    private static boolean dedicatedShadowUsed;
    private static boolean generatedShadowUsed;
    private static boolean dedicatedShadowFailed;

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
        resetProgramSources();
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
        resetProgramSources();
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

    public static synchronized boolean deferModelBuilds() {
        // Oculus retains its vertex-format setting across a DH-triggered pipeline destruction.
        // Do not consume Create/Flywheel model work in that gap; rendering recreates the pipeline.
        return ModelBuildReadiness.defer(
                BlockRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat(), pipeline != null);
    }

    public static synchronized boolean hasFailed() {
        return failed;
    }

    public static synchronized void resetForConfigurationChange() {
        failed = false;
        fallbackPending = false;
        successLogged = false;
        resetProgramSources();
    }

    public static synchronized void recordSuccess() {
        if (!successLogged) {
            successLogged = true;
            VroFlywheelShaderCompat.LOGGER.info("Flywheel shader program integration succeeded");
        }
    }

    public static synchronized void recordProgramSource(boolean shadow, boolean dedicated, boolean dedicatedFailed) {
        if (shadow) {
            dedicatedShadowUsed |= dedicated;
            generatedShadowUsed |= !dedicated;
            dedicatedShadowFailed |= dedicatedFailed;
        } else {
            dedicatedGbuffersUsed |= dedicated;
            generatedGbuffersUsed |= !dedicated;
            dedicatedGbuffersFailed |= dedicatedFailed;
        }
    }

    public static synchronized String describeProgramSource(boolean shadow) {
        boolean dedicated = shadow ? dedicatedShadowUsed : dedicatedGbuffersUsed;
        boolean generated = shadow ? generatedShadowUsed : generatedGbuffersUsed;
        boolean dedicatedFailed = shadow ? dedicatedShadowFailed : dedicatedGbuffersFailed;
        if (dedicatedFailed) {
            return "GENERATED FALLBACK AFTER DEDICATED FAILURE";
        }
        if (dedicated && generated) {
            return "MIXED DEDICATED/GENERATED";
        }
        if (dedicated) {
            return "DEDICATED";
        }
        if (generated) {
            return "GENERATED FALLBACK";
        }
        return "NOT COMPILED";
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

    private static void resetProgramSources() {
        dedicatedGbuffersUsed = false;
        generatedGbuffersUsed = false;
        dedicatedGbuffersFailed = false;
        dedicatedShadowUsed = false;
        generatedShadowUsed = false;
        dedicatedShadowFailed = false;
    }

    private static boolean featureEnabled() {
        return ClientOptimizationConfig.optimizationsEnabled()
                && ClientOptimizationConfig.createFlywheelShaderCompat;
    }
}
