package dev.hoyin1600p.vault_render_optimization.client.chunk;

import static org.junit.jupiter.api.Assertions.*;
import dev.hoyin1600p.vault_render_optimization.renderertransfer.RendererFamily;
import org.junit.jupiter.api.Test;

final class ChunkUpdateBackendTest {
    @Test
    void vanillaWorksWithoutAnyOptionalRenderer() {
        assertEquals(ChunkUpdateBackend.VANILLA, select(RendererFamily.NONE, null));
        assertFalse(ChunkUpdateBackend.VANILLA.usesSodiumScheduler());
    }

    @Test
    void validatedRenderersSelectOnlyTheirOwnScheduler() {
        assertEquals(ChunkUpdateBackend.EMBEDDIUM, select(RendererFamily.EMBEDDIUM, "0.3.18+mc1.18.2"));
        assertEquals(ChunkUpdateBackend.EMBEDDIUM, select(RendererFamily.EMBEDDIUM, "0.3.19+mc1.18.2"));
        assertEquals(ChunkUpdateBackend.RUBIDIUM, select(RendererFamily.RUBIDIUM, "0.5.6"));
        assertTrue(ChunkUpdateBackend.EMBEDDIUM.usesSodiumScheduler());
        assertTrue(ChunkUpdateBackend.RUBIDIUM.usesSodiumScheduler());
        assertFalse(ChunkUpdateBackend.BLOCKED.usesSodiumScheduler());
    }

    @Test
    void unknownAndAmbiguousRenderersNeverFallBackToVanillaInjection() {
        assertEquals(ChunkUpdateBackend.BLOCKED, select(RendererFamily.AMBIGUOUS, "0.5.6"));
        assertEquals(ChunkUpdateBackend.BLOCKED, select(RendererFamily.EMBEDDIUM, null));
        assertEquals(ChunkUpdateBackend.BLOCKED, select(RendererFamily.EMBEDDIUM, "0.3.19+mc1.20.1"));
        assertEquals(ChunkUpdateBackend.BLOCKED, select(RendererFamily.EMBEDDIUM, "0.3.180+mc1.18.2"));
        assertEquals(ChunkUpdateBackend.BLOCKED, select(RendererFamily.RUBIDIUM, "0.6.0"));
    }

    @Test
    void serverFailedDiscoveryAndUnsupportedRendererFailClosed() {
        assertEquals(ChunkUpdateBackend.BLOCKED, ChunkUpdateBackend.select(false, false, false, RendererFamily.NONE, null));
        assertEquals(ChunkUpdateBackend.BLOCKED, ChunkUpdateBackend.select(true, true, false, RendererFamily.NONE, null));
        assertEquals(ChunkUpdateBackend.BLOCKED, ChunkUpdateBackend.select(true, false, true, RendererFamily.NONE, null));
        assertEquals(ChunkUpdateBackend.BLOCKED, ChunkUpdateBackend.select(true, false, true,
                RendererFamily.EMBEDDIUM, "0.3.18+mc1.18.2"));
    }

    private static ChunkUpdateBackend select(RendererFamily family, String version) {
        return ChunkUpdateBackend.select(true, false, false, family, version);
    }
}
