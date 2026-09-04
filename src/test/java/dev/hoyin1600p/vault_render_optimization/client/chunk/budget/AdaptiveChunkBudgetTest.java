package dev.hoyin1600p.vault_render_optimization.client.chunk.budget;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayDeque;
import org.junit.jupiter.api.Test;

class AdaptiveChunkBudgetTest {
    private static final long MS = 1_000_000;
    private static final long MIB = AdaptiveChunkBudget.MIB;
    private final boolean[] allWaiting = {true, true, true, true, true};
    private AdaptiveChunkBudget controller() { return new AdaptiveChunkBudget(8L * 1024 * MIB); }
    private void frame(AdaptiveChunkBudget budget, long now, long bytes, int count) {
        budget.beginFrame(now, 10, 0, 20, bytes, count, 0, allWaiting);
    }

    @Test void startsConservativelyButAllowsOneLargeIndivisibleJob() {
        var budget = controller();
        frame(budget, MS, 0, 0);
        assertEquals(750_000, budget.uploadTimeAllowance());
        assertEquals(375_000, budget.uploadByteAllowance());
        assertEquals(1, budget.limit(Integer.MAX_VALUE, 4, false));
        budget.admitted(4, false);
        assertEquals(0, budget.limit(Integer.MAX_VALUE, 4, false));
    }

    @Test void expensiveUploadsReduceAllowanceWithoutMachineSpecificCalibration() {
        var fast = controller();
        var slow = controller();
        for (int i = 1; i <= 120; i++) {
            frame(fast, i * 16 * MS, MIB, 4);
            frame(slow, i * 16 * MS, MIB, 4);
            fast.observeUpload(100_000, MIB, 4);
            slow.observeUpload(15 * MS, MIB, 4);
        }
        assertTrue(fast.uploadByteAllowance() > slow.uploadByteAllowance() * 10);
        assertTrue(slow.uploadTimeAllowance() <= 750_000);
    }

    @Test void lowFpsCannotIncreaseBudgetWithoutEvidenceOfCheapUploads() {
        var budget = controller();
        for (int i = 1; i <= 500; i++) {
            frame(budget, i * 100 * MS, 4 * MIB, 16);
            budget.observeUpload(20 * MS, 4 * MIB, 16);
        }
        assertEquals(250_000, budget.uploadTimeAllowance());
        assertTrue(budget.uploadByteAllowance() < MIB);
    }

    @Test void stableRecoveryIsGradualAndCapped() {
        var budget = controller();
        for (int i = 1; i <= 10000; i++) {
            frame(budget, i * 16 * MS, MIB, 4);
            long before = budget.uploadTimeAllowance();
            budget.observeUpload(100_000, MIB, 4);
            assertTrue(before >= 250_000 && before <= 1_500_000);
        }
        assertEquals(1_500_000, budget.uploadTimeAllowance());
        assertTrue(budget.uploadByteAllowance() <= 8 * MIB);
    }

    @Test void highRefreshHasSmallerCeilingAndPauseDoesNotTeachMoreWork() {
        var budget = controller();
        frame(budget, MS, 0, 0);
        frame(budget, 5 * MS, 0, 0);
        assertTrue(budget.uploadTimeAllowance() <= 400_000);
        long before = budget.uploadTimeAllowance();
        frame(budget, 5000 * MS, MIB, 4);
        assertEquals(before, budget.uploadTimeAllowance());
    }

    @Test void backlogWatermarksStopAdmissionsButDoNotStopUploadProgress() {
        var budget = controller();
        frame(budget, MS, budget.memoryWatermark(), 1);
        assertEquals(0, budget.limit(Integer.MAX_VALUE, 4, false));
        assertTrue(budget.uploadByteAllowance() > 0);
        frame(budget, 17 * MS, 4096, 128);
        assertEquals(0, budget.limit(Integer.MAX_VALUE, 4, false));
        frame(budget, 33 * MS, 0, 0);
        assertTrue(budget.limit(Integer.MAX_VALUE, 4, false) > 0);
    }

    @Test void memoryWatermarkScalesWithinConservativeBounds() {
        assertEquals(16 * MIB, new AdaptiveChunkBudget(512 * MIB).memoryWatermark());
        assertEquals(32 * MIB, new AdaptiveChunkBudget(4096 * MIB).memoryWatermark());
        assertEquals(64 * MIB, new AdaptiveChunkBudget(65536 * MIB).memoryWatermark());
    }

    @Test void activeAndQueuedWorkersReserveEstimatedOutputMemory() {
        var budget = new AdaptiveChunkBudget(512 * MIB);
        budget.beginFrame(MS, 10, 10, 20, 0, 0, 0, allWaiting);
        assertEquals(0, budget.limit(Integer.MAX_VALUE, 4, false));
        budget.beginFrame(17 * MS, 10, 0, 12, 0, 0, 0, allWaiting);
        assertEquals(0, budget.limit(Integer.MAX_VALUE, 4, false));
    }

    @Test void reservationsAreIdempotentAndAgedClassesProgressDuringImportantStorm() {
        var budget = controller();
        int[] progress = new int[5];
        // Only one admission is possible per cycle: force the starvation safeguard to do the work.
        for (int i = 1; i <= 200; i++) {
            budget.beginFrame(i * 16 * MS, 1, 0, 2, 0, 0, 0, allWaiting);
            for (int type : new int[]{4, 1, 2, 3, 0}) {
                int allowed = budget.limit(1, type, type <= 1);
                assertEquals(allowed, budget.limit(allowed, type, type <= 1));
                if (allowed > 0) { budget.admitted(type, type <= 1); progress[type]++; }
            }
        }
        for (int count : progress) assertTrue(count > 0);
    }

    @Test void closedControllerIgnoresLateWorkersAndReturnsNativeAdmission() {
        var budget = controller();
        frame(budget, MS, 0, 0);
        budget.close();
        String before = budget.snapshot();
        budget.observeBuild(false, 500 * MS, 16 * MIB);
        budget.observeUpload(300 * MS, 16 * MIB, 2);
        assertEquals(before, budget.snapshot());
        assertEquals(57, budget.limit(57, 4, false));
    }

    @Test void slowerWorkersAdmitLessWorkAndBuildSortEstimatesAreIndependent() {
        var fast = controller();
        var slow = controller();
        for (int i = 0; i < 300; i++) {
            fast.observeBuild(false, 100_000, 65536);
            slow.observeBuild(false, 100 * MS, 65536);
            fast.observeBuild(true, 100_000, 65536);
            slow.observeBuild(true, 100_000, 65536);
        }
        frame(fast, MS, 0, 0);
        frame(slow, MS, 0, 0);
        assertTrue(fast.limit(20, 4, false) > slow.limit(20, 4, false));
        assertEquals(fast.limit(20, 0, true), slow.limit(20, 0, true));
    }

    @Test void monotonicClockMayHaveNegativeOrigin() {
        var budget = controller();
        budget.beginFrame(-1000 * MS, 1, 0, 2, 0, 0, 0, allWaiting);
        budget.admitted(4, false);
        budget.beginFrame(-700 * MS, 1, 0, 2, 0, 0, 0, allWaiting);
        // All classes aged; one is selected instead of losing timestamps merely because they're negative.
        boolean someProgress = false;
        for (int type = 0; type < 5; type++) someProgress |= budget.limit(1, type, type <= 1) > 0;
        assertTrue(someProgress);
    }

    @Test void closedLoopAdaptsWhenWorkerAndUploadLoadChangeWithoutUnboundedBacklog() {
        record Job(long ready, long duration) { }
        var jobs = new ArrayDeque<Job>();
        var results = new ArrayDeque<Long>();
        var budget = controller();
        boolean[] waiting = {false, false, false, false, true};
        long now = MS;
        long bytesPerResult = 64 * 1024;
        int fastAdmissions = 0, slowAdmissions = 0;
        for (int frame = 0; frame < 2000; frame++) {
            boolean slow = frame >= 1000;
            while (!jobs.isEmpty() && jobs.peek().ready <= now) {
                var finished = jobs.remove();
                results.add(bytesPerResult);
                budget.observeBuild(false, finished.duration, bytesPerResult);
            }
            int active = Math.min(4, jobs.size());
            int queued = Math.max(0, jobs.size() - active);
            budget.beginFrame(now, 4, active, Math.max(0, 8 - queued),
                    results.size() * bytesPerResult, results.size(), 0, waiting);
            int allowed = budget.limit(Integer.MAX_VALUE, 4, false);
            for (int j = 0; j < allowed; j++) {
                long cost = slow ? 40 * MS : MS;
                long ready = now + cost * (1 + jobs.size() / 4);
                // FIFO completion is sufficient for this deterministic, equal-size worker simulation.
                if (!jobs.isEmpty()) ready = Math.max(ready, jobs.peekLast().ready);
                jobs.add(new Job(ready, cost));
                budget.admitted(4, false);
                if (frame >= 500 && frame < 1000) fastAdmissions++;
                if (frame >= 1500) slowAdmissions++;
            }
            var drain = new BudgetedDrain<>(results, Long::longValue, ignored -> {},
                    budget.uploadByteAllowance(), Math.max(1, results.size()));
            while (drain.hasNext()) drain.next();
            long uploadTime = (long) (drain.bytes() * (slow ? 40.0 : 0.5));
            budget.observeUpload(uploadTime, drain.bytes(), drain.count());
            now += Math.max(16 * MS, 10 * MS + uploadTime);
            assertTrue(results.size() <= 128 + 12);
            assertTrue(results.size() * bytesPerResult < budget.memoryWatermark());
        }
        assertTrue(fastAdmissions > slowAdmissions, fastAdmissions + " versus " + slowAdmissions);
        assertTrue(slowAdmissions > 0, "overload must not stop progress permanently");
    }
}
