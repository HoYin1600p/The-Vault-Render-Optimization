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
    void exactInspectedCustomEmbeddiumBuildRetainsExistingGates() {
        String custom = "0.3.19-git.7b0cf676+mc1.18.2";
        assertStatus(RendererTransferStatus.APPLIED, RendererTransferFeature.ADJACENT_BLOCK_OCCLUSION,
                false, true, RendererFamily.EMBEDDIUM, custom, null);
        assertStatus(RendererTransferStatus.YIELDED, RendererTransferFeature.ADJACENT_BLOCK_OCCLUSION,
                false, false, RendererFamily.EMBEDDIUM, custom, null);
        assertStatus(RendererTransferStatus.YIELDED, RendererTransferFeature.CHUNK_REBUILD_DEDUPLICATION,
                true, true, RendererFamily.EMBEDDIUM, custom, null);
        assertStatus(RendererTransferStatus.BLOCKED, RendererTransferFeature.DIRECT_CCL_RENDERER_LOOKUP,
                false, true, RendererFamily.EMBEDDIUM, custom, "CodeChickenLib is not installed");
        assertStatus(RendererTransferStatus.BLOCKED, RendererTransferFeature.ADJACENT_BLOCK_OCCLUSION,
                false, true, RendererFamily.RUBIDIUM, custom, null);
    }

    @Test
    void otherGitBuildsRemainUnvalidated() {
        for (String version : new String[]{"0.3.19-git.deadbeef+mc1.18.2", "0.3.19-git.7b0cf676",
                "0.3.19-git.7b0cf676+mc1.20.1", "0.3.19-git.7b0cf6760+mc1.18.2"}) {
            assertStatus(RendererTransferStatus.BLOCKED, RendererTransferFeature.ADJACENT_BLOCK_OCCLUSION,
                    false, true, RendererFamily.EMBEDDIUM, version, null);
        }
    }

    @Test
    void postRemovalForkBuildRemainsOwnedByVro() {
        // The single-owner control (Embeddium 565016a2) ran on this build with every transfer APPLIED.
        assertStatus(RendererTransferStatus.APPLIED, RendererTransferFeature.ADJACENT_BLOCK_OCCLUSION,
                false, true, RendererFamily.EMBEDDIUM, "0.3.18-git.ced34c84+mc1.18.2", null);
    }

    @Test
    void preRemovalForkBuildsAndPrefixLookalikesFailClosed() {
        for (String version : new String[]{"0.3.18-git.a8cebc3a.dirty+mc1.18.2", "0.3.18-git.14ef7988+mc1.18.2",
                "0.3.18", "0.3.18+mc1.20.1", "0.3.180+mc1.18.2", "0.3.18-git.ced34c84"}) {
            assertStatus(RendererTransferStatus.BLOCKED, RendererTransferFeature.ADJACENT_BLOCK_OCCLUSION,
                    false, true, RendererFamily.EMBEDDIUM, version, null);
        }
        for (String version : new String[]{"0.5.60", "0.5.6+mc1.18.2", "0.5.6-git.1"}) {
            assertStatus(RendererTransferStatus.BLOCKED, RendererTransferFeature.ADJACENT_BLOCK_OCCLUSION,
                    false, true, RendererFamily.RUBIDIUM, version, null);
        }
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
