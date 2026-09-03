package dev.hoyin1600p.vault_render_optimization.client.chunk;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class ChunkUpdatePolicyTest {
    @Test
    void vroRequestsAsyncWhenEnabledAndNotComparing() {
        assertTrue(ChunkUpdatePolicy.effectiveDeferral(false, true, false));
        assertFalse(ChunkUpdatePolicy.effectiveDeferral(false, false, false));
        assertFalse(ChunkUpdatePolicy.effectiveDeferral(false, true, true));
    }

    @Test
    void neverOverridesAnExistingNativeDeferredPreference() {
        for (boolean enabled : new boolean[]{false, true}) {
            for (boolean compare : new boolean[]{false, true}) {
                assertTrue(ChunkUpdatePolicy.effectiveDeferral(true, enabled, compare));
            }
        }
    }

    @Test
    void togglingIsStatelessAndRestoresNativeBehavior() {
        assertFalse(ChunkUpdatePolicy.effectiveDeferral(false, false, false));
        assertTrue(ChunkUpdatePolicy.effectiveDeferral(false, true, false));
        assertFalse(ChunkUpdatePolicy.effectiveDeferral(false, true, true));
        assertTrue(ChunkUpdatePolicy.effectiveDeferral(false, true, false));
        assertFalse(ChunkUpdatePolicy.effectiveDeferral(false, false, false));
    }
}
