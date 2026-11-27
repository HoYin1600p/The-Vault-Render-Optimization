package dev.hoyin1600p.vault_render_optimization.client.sound;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class SoundOptimizationTest {
    @Test
    void staticChannelCullingOnlySkipsNonStreamingWhenFull() {
        assertTrue(SoundOptimization.shouldCullSaturatedStaticChannel(false, 247, 247));
        assertTrue(SoundOptimization.shouldCullSaturatedStaticChannel(false, 250, 247));
        assertFalse(SoundOptimization.shouldCullSaturatedStaticChannel(false, 246, 247));
        assertFalse(SoundOptimization.shouldCullSaturatedStaticChannel(true, 247, 247));
        assertFalse(SoundOptimization.shouldCullSaturatedStaticChannel(false, 0, 0));
    }

    @Test
    void distanceCullingRespectsRangeSafetyMarginVolumeAndExceptions() {
        // Default range = max(1.0, 0.5) * 16 = 16, padded = 20 -> thresholdSq = 400.
        assertFalse(SoundOptimization.shouldCullOutOfRange(
                false, false, false, true, false, 0.5F, 16,
                SoundOptimization.distanceSquared(0, 0, 0, 20.0, 0, 0)));
        assertTrue(SoundOptimization.shouldCullOutOfRange(
                false, false, false, true, false, 0.5F, 16,
                SoundOptimization.distanceSquared(0, 0, 0, 20.1, 0, 0)));

        // High volume scales range: volume 2.0 * 16 = 32, padded = 36 -> thresholdSq = 1296.
        assertFalse(SoundOptimization.shouldCullOutOfRange(
                false, false, false, true, false, 2.0F, 16,
                SoundOptimization.distanceSquared(0, 0, 0, 35.5, 0, 0)));
        assertTrue(SoundOptimization.shouldCullOutOfRange(
                false, false, false, true, false, 2.0F, 16,
                SoundOptimization.distanceSquared(0, 0, 0, 36.5, 0, 0)));

        // Elevator/teleport regression: listener still at old floor (y=64), player already at
        // destination (y=90), teleport sound plays at destination (y=90). Minimum distance is 0.
        double staleListenerDistSq = SoundOptimization.distanceSquared(0, 64, 0, 0, 90, 0);
        double playerEyeDistSq = SoundOptimization.distanceSquared(0, 91.62, 0, 0, 90.5, 0);
        double minDistSq = Math.min(staleListenerDistSq, playerEyeDistSq);
        assertFalse(SoundOptimization.shouldCullOutOfRange(
                false, false, false, true, false, 1.0F, 16, minDistSq));

        // Relative, looping, tickable, non-linear attenuation, or canStartSilent sounds are never culled.
        double farSq = SoundOptimization.distanceSquared(0, 0, 0, 100.0, 0, 0);
        assertFalse(SoundOptimization.shouldCullOutOfRange(
                true, false, false, true, false, 1.0F, 16, farSq));
        assertFalse(SoundOptimization.shouldCullOutOfRange(
                false, true, false, true, false, 1.0F, 16, farSq));
        assertFalse(SoundOptimization.shouldCullOutOfRange(
                false, false, true, true, false, 1.0F, 16, farSq));
        assertFalse(SoundOptimization.shouldCullOutOfRange(
                false, false, false, false, false, 1.0F, 16, farSq));
        assertFalse(SoundOptimization.shouldCullOutOfRange(
                false, false, false, true, true, 1.0F, 16, farSq));
    }
}
