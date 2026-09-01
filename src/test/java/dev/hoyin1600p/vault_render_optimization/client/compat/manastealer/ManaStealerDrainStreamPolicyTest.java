package dev.hoyin1600p.vault_render_optimization.client.compat.manastealer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ManaStealerDrainStreamPolicyTest {
    @Test
    void sphericalEligibilityMatchesTheTrapDistanceBoundary() {
        assertTrue(ManaStealerDrainStreamPolicy.insideSphere(6.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 6.0D));
        assertTrue(ManaStealerDrainStreamPolicy.insideSphere(3.0D, 4.0D, 0.0D, 0.0D, 0.0D, 0.0D, 6.0D));
        assertFalse(ManaStealerDrainStreamPolicy.insideSphere(6.001D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 6.0D));
        assertFalse(ManaStealerDrainStreamPolicy.insideSphere(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D));
    }

    @Test
    void fullRadiusUsesApproximatelyHalfTheOriginalArcaneLivePopulation() {
        assertEquals(45, ManaStealerDrainStreamPolicy.visibleOrbCount(
                6.0D, 6.5D, 6, 48, ManaStealerDrainStreamPolicy.ALL));
        assertEquals(29, ManaStealerDrainStreamPolicy.visibleOrbCount(
                6.0D, 6.5D, 6, 48, ManaStealerDrainStreamPolicy.DECREASED));
        assertEquals(11, ManaStealerDrainStreamPolicy.visibleOrbCount(
                6.0D, 6.5D, 6, 48, ManaStealerDrainStreamPolicy.MINIMAL));
    }

    @Test
    void populationIsBoundedAndDegeneratePathsRenderNothing() {
        assertEquals(48, ManaStealerDrainStreamPolicy.visibleOrbCount(
                100.0D, 6.5D, 6, 48, ManaStealerDrainStreamPolicy.ALL));
        assertEquals(0, ManaStealerDrainStreamPolicy.visibleOrbCount(
                0.01D, 6.5D, 6, 48, ManaStealerDrainStreamPolicy.ALL));
        assertEquals(0, ManaStealerDrainStreamPolicy.visibleOrbCount(
                6.0D, 0.0D, 6, 48, ManaStealerDrainStreamPolicy.ALL));
    }

    @Test
    void progressWrapsDeterministicallyAcrossSlots() {
        assertEquals(0.25D, ManaStealerDrainStreamPolicy.progress(0.25D, 0, 4), 1.0E-9D);
        assertEquals(0.50D, ManaStealerDrainStreamPolicy.progress(0.25D, 1, 4), 1.0E-9D);
        assertEquals(0.0D, ManaStealerDrainStreamPolicy.progress(0.25D, 3, 4), 1.0E-9D);
        assertEquals(0.0D, ManaStealerDrainStreamPolicy.progress(3.0D, 0, 0), 1.0E-9D);
    }
}
