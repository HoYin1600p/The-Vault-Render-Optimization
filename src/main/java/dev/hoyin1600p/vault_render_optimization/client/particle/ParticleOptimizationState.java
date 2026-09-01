package dev.hoyin1600p.vault_render_optimization.client.particle;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;

public final class ParticleOptimizationState {
    private static volatile boolean rendererAvailable;
    private static volatile boolean externalBillboardOwner;

    private ParticleOptimizationState() {
    }

    public static void configureEnvironment(boolean renderer, boolean externalOwner) {
        rendererAvailable = renderer;
        externalBillboardOwner = externalOwner;
    }

    public static boolean useVroBillboardGeometry() {
        return shouldUseVroBillboardGeometry(
                ClientOptimizationConfig.optimizationsEnabled(),
                ClientOptimizationConfig.particleBillboardFastPath,
                ClientOptimizationConfig.particleBillboardOwner,
                rendererAvailable,
                externalBillboardOwner
        );
    }

    static boolean shouldUseVroBillboardGeometry(
            boolean optimizationsEnabled,
            boolean billboardFastPath,
            ParticleBillboardOwner configuredOwner,
            boolean renderer,
            boolean externalOwner
    ) {
        if (!optimizationsEnabled || !billboardFastPath || externalOwner) {
            return false;
        }

        return configuredOwner != ParticleBillboardOwner.RENDERER || !renderer;
    }

    public static boolean rendererAvailable() {
        return rendererAvailable;
    }

    public static boolean externalBillboardOwner() {
        return externalBillboardOwner;
    }

    public static String resolvedBillboardOwner() {
        if (externalBillboardOwner) {
            return "external";
        }
        if (!ClientOptimizationConfig.optimizationsEnabled()
                || !ClientOptimizationConfig.particleBillboardFastPath) {
            return rendererAvailable ? "renderer" : "vanilla";
        }
        if (ClientOptimizationConfig.particleBillboardOwner == ParticleBillboardOwner.RENDERER
                && rendererAvailable) {
            return "renderer";
        }
        return "vro";
    }
}
