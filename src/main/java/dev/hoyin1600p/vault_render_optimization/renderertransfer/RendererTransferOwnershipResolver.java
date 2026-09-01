package dev.hoyin1600p.vault_render_optimization.renderertransfer;

public final class RendererTransferOwnershipResolver {
    private RendererTransferOwnershipResolver() {
    }

    public static RendererTransferDecision resolve(
            RendererTransferFeature feature,
            boolean physicalClient,
            boolean compareMode,
            boolean configured,
            RendererFamily family,
            String rendererVersion,
            String compatibilityBlocker
    ) {
        if (!physicalClient) {
            return blocked(feature, "not running on the physical client");
        }
        if (!configured) {
            return yielded(feature, "disabled by VRO configuration");
        }
        if (compareMode && !feature.activeInCompareMode()) {
            return yielded(feature, "disabled by Compare Mode");
        }
        if (family == RendererFamily.NONE) {
            return blocked(feature, "no supported renderer is installed");
        }
        if (family == RendererFamily.AMBIGUOUS) {
            return blocked(feature, "multiple renderer implementations were detected");
        }
        if (!feature.supports(family)) {
            return blocked(feature, family.name().toLowerCase() + " does not expose the validated implementation");
        }
        if (!isSupportedVersion(family, rendererVersion)) {
            return blocked(feature, "unvalidated " + family.name().toLowerCase() + " version " + rendererVersion);
        }
        if (compatibilityBlocker != null) {
            return blocked(feature, compatibilityBlocker);
        }
        return new RendererTransferDecision(
                feature,
                RendererTransferStatus.APPLIED,
                "VRO owns the validated " + family.name().toLowerCase() + " implementation"
        );
    }

    static boolean isSupportedVersion(RendererFamily family, String version) {
        if (version == null) {
            return false;
        }
        return family == RendererFamily.EMBEDDIUM && version.startsWith("0.3.18")
                || family == RendererFamily.RUBIDIUM && version.startsWith("0.5.6");
    }

    private static RendererTransferDecision yielded(RendererTransferFeature feature, String reason) {
        return new RendererTransferDecision(feature, RendererTransferStatus.YIELDED, reason);
    }

    private static RendererTransferDecision blocked(RendererTransferFeature feature, String reason) {
        return new RendererTransferDecision(feature, RendererTransferStatus.BLOCKED, reason);
    }
}
