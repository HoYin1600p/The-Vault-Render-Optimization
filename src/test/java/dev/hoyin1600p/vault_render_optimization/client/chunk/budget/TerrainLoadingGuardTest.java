package dev.hoyin1600p.vault_render_optimization.client.chunk.budget;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class TerrainLoadingGuardTest {
    private static final long MS = 1_000_000;
    private final TerrainLoadingGuard guard = new TerrainLoadingGuard(64 * AdaptiveChunkBudget.MIB);
    private void update(long ms, int[] requests, int queued, int active, int ready, long bytes, long wait, boolean initial) {
        guard.begin(ms * MS, requests, queued, active, ready, bytes, wait * MS, initial);
    }
    private void idle(long ms) { update(ms, new int[5], 0, 0, 0, 0, 0, false); }
    private void pacing() { idle(0); idle(500); assertTrue(guard.pacing()); }

    @Test void startupRequiresNativeDrainThenRecoveryRatherThanThrottlingUnknownJobs() {
        update(0, new int[5], 12, 4, 0, 0, 0, false);
        update(1000, new int[5], 0, 1, 0, 0, 0, false);
        assertFalse(guard.pacing());
        idle(1100);
        idle(1599);
        assertFalse(guard.pacing());
        idle(1600);
        assertTrue(guard.pacing());
    }

    @Test void cachedTerrainRequestBurstBypassesEvenWithEmptyCompletedQueue() {
        pacing();
        update(516, new int[]{0, 0, 128, 0, 0}, 0, 0, 0, 0, 0, false);
        assertFalse(guard.pacing());
        for (int type = 0; type < 5; type++) assertFalse(guard.mayLimit(type));
        assertTrue(guard.status().contains("128"));
    }

    @Test void initialSchedulingIsNeverLimitedEvenBeforeTheNextGuardSample() {
        pacing();
        assertFalse(guard.mayLimit(TerrainLoadingGuard.INITIAL_BUILD));
        assertTrue(guard.mayLimit(3));
        assertTrue(guard.mayLimit(0));
    }

    @Test void initialResultsAndTheirWorkersKeepTheNativePathUntilFullyDrained() {
        pacing();
        update(516, new int[5], 0, 2, 1, 64, 0, true);
        assertFalse(guard.pacing());
        update(532, new int[5], 0, 1, 0, 0, 0, false);
        assertFalse(guard.pacing());
        update(548, new int[5], 0, 0, 1, 0, 0, true); // empty geometry still changes built state
        assertFalse(guard.pacing());
        idle(564); idle(1064);
        assertTrue(guard.pacing());
    }

    @Test void resultArrivingAfterFrameStartImmediatelyDisablesPacing() {
        pacing();
        guard.protectReadyTerrain();
        assertFalse(guard.pacing());
        assertTrue(guard.status().contains("completed during update"));
    }

    @Test void waitingRequestsTriggerRecoveryDespiteTinyCompletedQueue() {
        pacing();
        update(516, new int[]{0, 0, 0, 12, 0}, 0, 0, 0, 0, 0, false);
        assertTrue(guard.pacing());
        update(766, new int[]{0, 0, 0, 10, 0}, 0, 0, 0, 0, 0, false);
        assertFalse(guard.pacing());
        assertTrue(guard.status().contains("safety window"));
    }

    @Test void resultAgeRequestPressureAndMemoryPressureIndependentlyYield() {
        pacing();
        update(516, new int[5], 0, 0, 1, 64, 250, false);
        assertFalse(guard.pacing());
        idle(532); idle(1032);
        update(1048, new int[]{0, 0, 0, 128, 0}, 0, 0, 0, 0, 0, false);
        assertFalse(guard.pacing());
        idle(1064); idle(1564);
        update(1580, new int[5], 0, 0, 1, 64 * AdaptiveChunkBudget.MIB, 0, false);
        assertFalse(guard.pacing());
    }

    @Test void cooldownPreventsRepeatedThrottleCatchupOscillation() {
        pacing();
        for (int ms = 516; ms < 2000; ms += 200) {
            update(ms, new int[]{0, 0, 10, 0, 0}, 0, 0, 0, 0, 0, false);
            idle(ms + 16);
            assertFalse(guard.pacing());
        }
        idle(2200); idle(2700);
        assertTrue(guard.pacing());
    }

    @Test void stableAlreadyBuiltUpdatesRemainEligibleAndNegativeClockOriginsWork() {
        idle(-1000); idle(-500);
        for (int ms = -484; ms < 2000; ms += 32) {
            update(ms, new int[]{0, 0, 0, 3, 0}, 0, 1, 1, 128, 16, false);
            assertTrue(guard.mayLimit(3));
            idle(ms + 16);
        }
    }
}
