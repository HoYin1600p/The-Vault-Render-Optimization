package dev.hoyin1600p.vault_render_optimization.client.chunk;

import dev.hoyin1600p.vault_render_optimization.renderertransfer.RendererFamily;
import dev.hoyin1600p.vault_render_optimization.renderertransfer.ValidatedRendererVersions;

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
        if (!ValidatedRendererVersions.supports(family, version)) {
            return BLOCKED;
        }
        return family == RendererFamily.EMBEDDIUM ? EMBEDDIUM : RUBIDIUM;
    }

    public boolean usesSodiumScheduler() {
        return this == EMBEDDIUM || this == RUBIDIUM;
    }
}
