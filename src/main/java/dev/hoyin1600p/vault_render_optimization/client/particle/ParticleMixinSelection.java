package dev.hoyin1600p.vault_render_optimization.client.particle;

public final class ParticleMixinSelection {
    private ParticleMixinSelection() {
    }

    public static boolean rendererPath(
            boolean discoveryFailed,
            boolean rendererAvailable,
            boolean externalOwner
    ) {
        return !discoveryFailed && rendererAvailable && !externalOwner;
    }

    public static boolean portablePath(
            boolean discoveryFailed,
            boolean rendererAvailable,
            boolean externalOwner
    ) {
        return !discoveryFailed && !rendererAvailable && !externalOwner;
    }
}
