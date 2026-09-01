package dev.hoyin1600p.vault_render_optimization.renderertransfer;

/** Resolves renderer ownership while accounting for Embeddium's Rubidium compatibility alias. */
public final class RendererFamilyDetector {
    private RendererFamilyDetector() {
    }

    public static RendererFamily resolve(
            boolean embeddiumLoaded,
            boolean rubidiumLoaded,
            boolean rendererIdsShareModFile
    ) {
        if (embeddiumLoaded && rubidiumLoaded) {
            return rendererIdsShareModFile ? RendererFamily.EMBEDDIUM : RendererFamily.AMBIGUOUS;
        }
        if (embeddiumLoaded) {
            return RendererFamily.EMBEDDIUM;
        }
        if (rubidiumLoaded) {
            return RendererFamily.RUBIDIUM;
        }
        return RendererFamily.NONE;
    }
}
