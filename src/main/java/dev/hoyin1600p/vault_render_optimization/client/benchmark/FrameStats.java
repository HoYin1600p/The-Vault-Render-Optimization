package dev.hoyin1600p.vault_render_optimization.client.benchmark;

import java.util.Arrays;
import java.util.List;

/** Statistics of completed frame intervals. p99 uses the nearest-rank percentile. */
public record FrameStats(int frameCount, double averageFps, double medianMs,
                         double p99Ms, double onePercentLowFps) {
    /** Robust typical FPS, used for benchmark recommendations. */
    public double medianFps() { return medianMs > 0 ? 1000.0 / medianMs : 0; }

    public static FrameStats from(List<Long> intervalsNanos) {
        return from(intervalsNanos.stream().mapToLong(Long::longValue).toArray());
    }

    public static FrameStats from(long[] intervalsNanos) {
        if (intervalsNanos.length == 0) return new FrameStats(0, 0, 0, 0, 0);
        long[] sorted = intervalsNanos.clone();
        double total = 0;
        for (long interval : sorted) {
            if (interval <= 0) throw new IllegalArgumentException("Frame intervals must be positive");
            total += interval;
        }
        Arrays.sort(sorted);
        int count = sorted.length;
        double median = count % 2 == 0
                ? (sorted[count / 2 - 1] / 2.0 + sorted[count / 2] / 2.0)
                : sorted[count / 2];
        int slowCount = Math.max(1, (int) Math.ceil(count * 0.01));
        double slowTotal = 0;
        for (int i = count - slowCount; i < count; i++) slowTotal += sorted[i];
        return new FrameStats(count, count * 1_000_000_000.0 / total, median / 1_000_000.0,
                sorted[(int) Math.ceil(count * 0.99) - 1] / 1_000_000.0,
                slowCount * 1_000_000_000.0 / slowTotal);
    }
}
