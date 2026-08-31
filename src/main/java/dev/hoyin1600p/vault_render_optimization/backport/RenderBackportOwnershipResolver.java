package dev.hoyin1600p.vault_render_optimization.backport;

public final class RenderBackportOwnershipResolver {
    private RenderBackportOwnershipResolver() {
    }

    public static RenderBackportDecision resolve(
            RenderBackportFeature feature,
            boolean physicalClient,
            boolean compareMode,
            boolean configuredEnabled,
            boolean vhAcceleratorProvidesFeature,
            ModernFixOwnership modernFixOwnership,
            String compatibilityBlocker
    ) {
        if (!physicalClient) {
            return decision(feature, RenderBackportOwner.UNAVAILABLE, "not available on a dedicated server");
        }
        if (compatibilityBlocker != null) {
            return decision(feature, RenderBackportOwner.UNAVAILABLE, compatibilityBlocker);
        }
        if (vhAcceleratorProvidesFeature) {
            return decision(
                    feature,
                    RenderBackportOwner.VH_ACCELERATOR,
                    "the current VH Accelerator still contains this exact feature"
            );
        }
        if (modernFixOwnership == ModernFixOwnership.ACTIVE) {
            return decision(feature, RenderBackportOwner.MODERNFIX, "the effective ModernFix option owns this path");
        }
        if (modernFixOwnership == ModernFixOwnership.UNKNOWN) {
            return decision(feature, RenderBackportOwner.UNAVAILABLE, "ModernFix ownership could not be verified");
        }
        if (compareMode) {
            return decision(feature, RenderBackportOwner.DISABLED, "Compare Mode was enabled at launch");
        }
        if (!configuredEnabled) {
            return decision(feature, RenderBackportOwner.DISABLED, "disabled in the VRO client configuration");
        }
        return decision(feature, RenderBackportOwner.VRO, "enabled and owned by VRO");
    }

    private static RenderBackportDecision decision(
            RenderBackportFeature feature,
            RenderBackportOwner owner,
            String reason
    ) {
        return new RenderBackportDecision(feature, owner, reason);
    }
}
