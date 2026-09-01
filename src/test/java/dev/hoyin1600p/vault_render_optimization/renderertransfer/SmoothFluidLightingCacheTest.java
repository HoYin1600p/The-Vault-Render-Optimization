package dev.hoyin1600p.vault_render_optimization.renderertransfer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SmoothFluidLightingCacheTest {
    @Test
    void smoothLightingRequiresAmbientOcclusionAndZeroLuminosity() {
        assertTrue(SmoothFluidLightingCache.usesSmoothLighting(true, 0));
        assertFalse(SmoothFluidLightingCache.usesSmoothLighting(false, 0));
        assertFalse(SmoothFluidLightingCache.usesSmoothLighting(true, 1));
    }
}
