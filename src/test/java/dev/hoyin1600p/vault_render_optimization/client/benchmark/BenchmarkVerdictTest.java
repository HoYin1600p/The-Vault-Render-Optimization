package dev.hoyin1600p.vault_render_optimization.client.benchmark;

import static org.junit.jupiter.api.Assertions.*;
import static dev.hoyin1600p.vault_render_optimization.client.benchmark.BenchmarkVerdict.Recommendation.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Arrays;

class BenchmarkVerdictTest {
    @Test
    void recommendsOnOffOrNoDifference() {
        BenchmarkVerdict on = BenchmarkVerdict.from(110, 100, 100, 110);
        assertEquals(ON, on.recommendation());
        assertEquals(110, on.meanFpsOn());
        assertEquals(100, on.meanFpsOff());
        assertEquals(10, on.deltaPercent(), 1e-9);
        assertEquals(0, on.noisePercent());
        assertEquals(OFF, BenchmarkVerdict.from(90, 100, 100, 90).recommendation());
        assertEquals(NO_DIFFERENCE, BenchmarkVerdict.from(101, 100, 100, 101).recommendation());
    }

    @Test
    void largerRoundNoiseSuppressesAnApparentImprovement() {
        BenchmarkVerdict noisyOn = BenchmarkVerdict.from(100, 100, 100, 120);
        assertEquals(10, noisyOn.deltaPercent(), 1e-9);
        assertEquals(2000.0 / 110, noisyOn.noisePercent(), 1e-9);
        assertEquals(NO_DIFFERENCE, noisyOn.recommendation());
        BenchmarkVerdict noisyOff = BenchmarkVerdict.from(110, 90, 110, 110);
        assertEquals(20, noisyOff.noisePercent(), 1e-9);
        assertEquals(NO_DIFFERENCE, noisyOff.recommendation());
        assertEquals(ON, BenchmarkVerdict.from(160, 90, 110, 160).recommendation());
    }

    @Test
    void medianMetricKeepsBackgroundStallsOutOfDecisionButExposesAverageFps() {
        long[] on = new long[100];
        Arrays.fill(on, 8_000_000L);
        on[99] = 240_000_000L;
        FrameStats fastButStalled = FrameStats.from(on);
        FrameStats off = FrameStats.from(new long[] {10_000_000L, 10_000_000L});
        var verdict = BenchmarkVerdict.from(List.of(fastButStalled, off, off, fastButStalled), false);
        assertEquals(125, verdict.medianFpsOn());
        assertEquals(100, verdict.medianFpsOff());
        assertEquals(25, verdict.deltaPercent());
        assertEquals(fastButStalled.averageFps(), verdict.meanFpsOn());
        assertTrue(verdict.meanFpsOn() < verdict.meanFpsOff());
        assertEquals(ON, verdict.recommendation());
        assertEquals(0, verdict.noisePercent());
        assertTrue(BenchmarkVerdict.from(List.of(off, off, off, off), true).noisy());
    }

    @Test
    void rejectsUnmeasuredOrNonFiniteRounds() {
        assertThrows(IllegalArgumentException.class, () -> BenchmarkVerdict.from(0, 100, 100, 100));
        assertThrows(IllegalArgumentException.class, () -> BenchmarkVerdict.from(Double.NaN, 100, 100, 100));
        assertThrows(IllegalArgumentException.class, () -> BenchmarkVerdict.from(100, 100, Double.POSITIVE_INFINITY, 100));
    }
}
