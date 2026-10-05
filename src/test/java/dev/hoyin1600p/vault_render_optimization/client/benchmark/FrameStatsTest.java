package dev.hoyin1600p.vault_render_optimization.client.benchmark;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class FrameStatsTest {
    @Test
    void computesTimeWeightedFpsAndEvenMedianWithoutMutatingInput() {
        long[] frames = {30_000_000, 10_000_000};
        FrameStats stats = FrameStats.from(frames);
        assertEquals(2, stats.frameCount());
        assertEquals(50, stats.averageFps(), 1e-9);
        assertEquals(20, stats.medianMs(), 1e-9);
        assertEquals(30, stats.p99Ms(), 1e-9);
        assertEquals(1000.0 / 30, stats.onePercentLowFps(), 1e-9);
        assertArrayEquals(new long[] {30_000_000, 10_000_000}, frames);
    }

    @Test
    void slowestOnePercentUsesCeilingAndP99UsesNearestRank() {
        long[] frames = new long[101];
        Arrays.fill(frames, 10_000_000);
        frames[99] = 20_000_000;
        frames[100] = 40_000_000;
        FrameStats stats = FrameStats.from(frames);
        assertEquals(10, stats.medianMs());
        assertEquals(20, stats.p99Ms());
        assertEquals(1000.0 / 30, stats.onePercentLowFps(), 1e-9);
    }

    @Test
    void handlesSingleFrameEmptyListsAndRejectsInvalidIntervals() {
        assertEquals(new FrameStats(0, 0, 0, 0, 0), FrameStats.from(List.of()));
        assertEquals(new FrameStats(1, 100, 10, 10, 100), FrameStats.from(List.of(10_000_000L)));
        assertThrows(IllegalArgumentException.class, () -> FrameStats.from(new long[] {0}));
        assertThrows(IllegalArgumentException.class, () -> FrameStats.from(new long[] {-1}));
    }
}
