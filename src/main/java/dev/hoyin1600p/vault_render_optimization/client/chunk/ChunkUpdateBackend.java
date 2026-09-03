package dev.hoyin1600p.vault_render_optimization.client.chunk;

import dev.hoyin1600p.vault_render_optimization.renderertransfer.RendererFamily;

/** Mutually exclusive startup selection. No renderer classes are loaded by this selector. */
public enum ChunkUpdateBackend {
    VANILLA, EMBEDDIUM, RUBIDIUM, BLOCKED;

    public static ChunkUpdateBackend select(
            boolean physicalClient, boolean discoveryFailed, boolean otherRenderer,
            RendererFamily family, String version
    ) {
        if (!physicalClient || discoveryFailed || otherRenderer || family == RendererFamily.AMBIGUOUS) {
            return BLOCKED;
        }
        if (family == RendererFamily.NONE) {
            return VANILLA;
        }
        if (family == RendererFamily.EMBEDDIUM && version != null
                && (version.equals("0.3.18+mc1.18.2") || version.equals("0.3.19+mc1.18.2"))) {
            return EMBEDDIUM;
        }
        if (family == RendererFamily.RUBIDIUM && "0.5.6".equals(version)) {
            return RUBIDIUM;
        }
        return BLOCKED;
    }

    public boolean usesSodiumScheduler() {
        return this == EMBEDDIUM || this == RUBIDIUM;
    }
}
