package dev.hoyin1600p.vault_render_optimization.client.chunk.residency;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FarsightChunkBoundTest {
    @Test
    void boundIsTheLargerOfServerAndRenderDistancePlusOne() {
        assertEquals(13, FarsightChunkBound.bound(12, 8));
        assertEquals(33, FarsightChunkBound.bound(10, 32));
        assertEquals(1, FarsightChunkBound.bound(0, 0));
    }

    @Test
    void onlyChunksStrictlyOutsideTheSquareAreForgotten() {
        int bound = FarsightChunkBound.bound(10, 12);
        assertFalse(FarsightChunkBound.exceedsBound(13, -13, 0, 0, bound));
        assertTrue(FarsightChunkBound.exceedsBound(14, 0, 0, 0, bound));
        assertTrue(FarsightChunkBound.exceedsBound(0, -14, 0, 0, bound));
        assertFalse(FarsightChunkBound.exceedsBound(113, 100, 100, 100, bound));
    }

    @Test
    void vhAcceleratorHandOverMarkerShipsInTheJar() {
        // VH Accelerator leaves the bound to VRO only when this resource exists.
        assertNotNull(getClass().getClassLoader().getResource("META-INF/vro-features/farsight-chunk-bound"));
    }
}
