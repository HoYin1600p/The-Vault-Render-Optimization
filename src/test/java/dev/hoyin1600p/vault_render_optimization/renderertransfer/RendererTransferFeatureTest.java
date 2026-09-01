package dev.hoyin1600p.vault_render_optimization.renderertransfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RendererTransferFeatureTest {
    @Test
    void mapsOwnedMixinNamesToTheirTransferIds() {
        assertEquals(
                RendererTransferFeature.ADJACENT_BLOCK_OCCLUSION,
                RendererTransferFeature.forMixin(
                        "dev.hoyin1600p.vault_render_optimization.mixin.backport.embeddium.occlusion.BlockOcclusionCacheMixin"
                )
        );
        assertEquals(
                RendererTransferFeature.CHUNK_REBUILD_DEDUPLICATION,
                RendererTransferFeature.forMixin(
                        "dev.hoyin1600p.vault_render_optimization.mixin.backport.embeddium.chunk.EmbeddiumRenderSectionMixin"
                )
        );
        assertNull(RendererTransferFeature.forMixin("example.UnrelatedMixin"));
    }
}
