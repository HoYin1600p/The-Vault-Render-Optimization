package dev.hoyin1600p.vault_render_optimization.client.benchmark;

/** Durations are monotonic nanoseconds, independent of game ticks and frame rate. */
public record BenchmarkPlan(long warmupNanos, long measureNanos) {
    public static final BenchmarkPlan DEFAULT = new BenchmarkPlan(3_000_000_000L, 12_000_000_000L);
    /** ABBA: ON, OFF, OFF, ON. */
    public static final int ROUNDS = 4;

    public BenchmarkPlan {
        if (warmupNanos < 0 || measureNanos <= 0
                || warmupNanos > Long.MAX_VALUE - measureNanos) {
            throw new IllegalArgumentException("Invalid benchmark windows");
        }
    }

    public static boolean enabled(int round) {
        if (round < 0 || round >= ROUNDS) throw new IllegalArgumentException("Round must be 0..3");
        return round == 0 || round == ROUNDS - 1;
    }

    public double warmupSeconds() {
        return warmupNanos / 1e9;
    }

    public double measureSeconds() {
        return measureNanos / 1e9;
    }
}
