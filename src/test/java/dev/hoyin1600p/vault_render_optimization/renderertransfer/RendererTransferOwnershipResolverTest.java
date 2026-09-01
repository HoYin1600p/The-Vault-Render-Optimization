package dev.hoyin1600p.vault_render_optimization.renderertransfer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RendererTransferOwnershipResolverTest {
    @Test
    void validatedStockRenderersAreOwnedByVro() {
        assertStatus(RendererTransferStatus.APPLIED, RendererTransferFeature.ADJACENT_BLOCK_OCCLUSION,
                false, true, RendererFamily.EMBEDDIUM, "0.3.18+mc1.18.2", null);
        assertStatus(RendererTransferStatus.APPLIED, RendererTransferFeature.ADJACENT_BLOCK_OCCLUSION,
                false, true, RendererFamily.EMBEDDIUM, "0.3.19+mc1.18.2", null);
        assertStatus(RendererTransferStatus.APPLIED, RendererTransferFeature.ADJACENT_BLOCK_OCCLUSION,
                false, true, RendererFamily.RUBIDIUM, "0.5.6", null);
    }

    @Test
    void unknownVersionsAndAmbiguousRenderersFailClosed() {
        assertStatus(RendererTransferStatus.BLOCKED, RendererTransferFeature.ADJACENT_BLOCK_OCCLUSION,
                false, true, RendererFamily.EMBEDDIUM, "0.3.19", null);
        assertStatus(RendererTransferStatus.BLOCKED, RendererTransferFeature.ADJACENT_BLOCK_OCCLUSION,
                false, true, RendererFamily.EMBEDDIUM, "0.3.20+mc1.18.2", null);
        assertStatus(RendererTransferStatus.BLOCKED, RendererTransferFeature.ADJACENT_BLOCK_OCCLUSION,
                false, true, RendererFamily.AMBIGUOUS, null, null);
    }

    @Test
    void compareModeKeepsCorrectnessFixesAndYieldsPerformanceFeatures() {
        assertStatus(RendererTransferStatus.APPLIED, RendererTransferFeature.ADJACENT_BLOCK_OCCLUSION,
                true, true, RendererFamily.RUBIDIUM, "0.5.6", null);
        assertStatus(RendererTransferStatus.YIELDED, RendererTransferFeature.CHUNK_REBUILD_DEDUPLICATION,
                true, true, RendererFamily.RUBIDIUM, "0.5.6", null);
    }

    @Test
    void cclLookupIsOnlyOwnedOnValidatedEmbeddiumBridge() {
        assertStatus(RendererTransferStatus.BLOCKED, RendererTransferFeature.DIRECT_CCL_RENDERER_LOOKUP,
                false, true, RendererFamily.RUBIDIUM, "0.5.6", null);
        assertStatus(RendererTransferStatus.BLOCKED, RendererTransferFeature.DIRECT_CCL_RENDERER_LOOKUP,
                false, true, RendererFamily.EMBEDDIUM, "0.3.18+mc1.18.2", "CodeChickenLib is not installed");
    }

    private static void assertStatus(
            RendererTransferStatus expected,
            RendererTransferFeature feature,
            boolean compareMode,
            boolean configured,
            RendererFamily family,
            String version,
            String blocker
    ) {
        assertEquals(expected, RendererTransferOwnershipResolver.resolve(
                feature, true, compareMode, configured, family, version, blocker
        ).status());
    }
}
