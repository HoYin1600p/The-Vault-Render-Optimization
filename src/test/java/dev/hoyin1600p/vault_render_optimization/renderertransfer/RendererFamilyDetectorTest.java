package dev.hoyin1600p.vault_render_optimization.renderertransfer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RendererFamilyDetectorTest {
    @Test
    void stockEmbeddiumRubidiumAliasResolvesToEmbeddium() {
        assertEquals(RendererFamily.EMBEDDIUM, RendererFamilyDetector.resolve(true, true, true));
    }

    @Test
    void separateRendererFilesRemainAmbiguous() {
        assertEquals(RendererFamily.AMBIGUOUS, RendererFamilyDetector.resolve(true, true, false));
    }

    @Test
    void singleRenderersAndNoRendererResolveNormally() {
        assertEquals(RendererFamily.EMBEDDIUM, RendererFamilyDetector.resolve(true, false, false));
        assertEquals(RendererFamily.RUBIDIUM, RendererFamilyDetector.resolve(false, true, false));
        assertEquals(RendererFamily.NONE, RendererFamilyDetector.resolve(false, false, false));
    }
}
