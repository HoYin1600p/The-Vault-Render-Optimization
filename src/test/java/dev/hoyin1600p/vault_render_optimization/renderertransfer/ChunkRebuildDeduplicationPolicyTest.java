package dev.hoyin1600p.vault_render_optimization.renderertransfer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChunkRebuildDeduplicationPolicyTest {
    @Test
    void importantRequestsAreNeverSatisfiedByRegularPendingWork() {
        assertFalse(ChunkRebuildDeduplicationPolicy.hasEquivalentPending(true, true, false));
        assertTrue(ChunkRebuildDeduplicationPolicy.hasEquivalentPending(true, false, true));
    }

    @Test
    void ordinaryRequestsCoalesceIntoEitherPendingRebuildStrength() {
        assertTrue(ChunkRebuildDeduplicationPolicy.hasEquivalentPending(false, true, false));
        assertTrue(ChunkRebuildDeduplicationPolicy.hasEquivalentPending(false, false, true));
    }

    @Test
    void onlyUnfinishedUncancelledTasksCountAsActive() {
        assertTrue(ChunkRebuildDeduplicationPolicy.hasActiveTask(true, false, false));
        assertFalse(ChunkRebuildDeduplicationPolicy.hasActiveTask(false, false, false));
        assertFalse(ChunkRebuildDeduplicationPolicy.hasActiveTask(true, true, false));
        assertFalse(ChunkRebuildDeduplicationPolicy.hasActiveTask(true, false, true));
    }
}
