package dev.hoyin1600p.vault_render_optimization.backport;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RenderBackportCompatibilityTest {
    @Test
    void fluidloggedBlocksOnlyChunkMeshing() {
        assertNotNull(blocker(RenderBackportFeature.CHUNK_MESHING, true, false, false, false, false, true));
        assertNull(blocker(RenderBackportFeature.MODEL_VARIANT_TRAVERSAL, true, false, false, false, false, true));
    }

    @Test
    void eitherKnownRenderModBlocksTheBufferGuard() {
        assertNotNull(blocker(RenderBackportFeature.BUFFER_BUILDER_LEAK_FIX, false, true, false, false, false, true));
        assertNotNull(blocker(RenderBackportFeature.BUFFER_BUILDER_LEAK_FIX, false, false, true, false, false, true));
    }

    @Test
    void rubidiumOnlyIsBlockedWhileEmbeddiumPathsRemainEligible() {
        RenderBackportFeature feature = RenderBackportFeature.MODEL_DATA_MANAGER_CONCURRENCY;
        assertNotNull(blocker(feature, false, false, false, true, false, true));
        assertNull(blocker(feature, false, false, false, true, true, true));
        assertNull(blocker(feature, false, false, false, false, true, true));
    }

    @Test
    void ctmRequiresTheExactValidatedLayoutProbe() {
        RenderBackportFeature feature = RenderBackportFeature.CTM_METADATA_CACHE_CONCURRENCY;
        assertNotNull(blocker(feature, false, false, false, false, false, false));
        assertNull(blocker(feature, false, false, false, false, false, true));
    }

    @Test
    void failedDiscoveryBlocksEveryFeature() {
        assertNotNull(RenderBackportCompatibility.blocker(
                RenderBackportFeature.MODEL_SELECTOR_PREDICATE_CACHE,
                true,
                false,
                false,
                false,
                false,
                false,
                false
        ));
    }

    private static String blocker(
            RenderBackportFeature feature,
            boolean fluidlogged,
            boolean isometric,
            boolean witherStorm,
            boolean rubidium,
            boolean embeddium,
            boolean ctmCompatible
    ) {
        return RenderBackportCompatibility.blocker(
                feature,
                false,
                fluidlogged,
                isometric,
                witherStorm,
                rubidium,
                embeddium,
                ctmCompatible
        );
    }
}
