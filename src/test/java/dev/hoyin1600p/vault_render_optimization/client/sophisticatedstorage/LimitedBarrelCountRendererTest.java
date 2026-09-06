package dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class LimitedBarrelCountRendererTest {
    @Test
    void cacheKeyPreservesSignedCountBitsAndLayoutWidth() {
        assertEquals(LimitedBarrelCountRenderer.cacheKey(42, 5),
                LimitedBarrelCountRenderer.cacheKey(42, 5));
        assertNotEquals(LimitedBarrelCountRenderer.cacheKey(42, 5),
                LimitedBarrelCountRenderer.cacheKey(43, 5));
        assertNotEquals(LimitedBarrelCountRenderer.cacheKey(42, 5),
                LimitedBarrelCountRenderer.cacheKey(42, 6));
        assertNotEquals(LimitedBarrelCountRenderer.cacheKey(-1, 5),
                LimitedBarrelCountRenderer.cacheKey(Integer.MAX_VALUE, 5));
    }
}
