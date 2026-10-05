package dev.hoyin1600p.vault_render_optimization.client.benchmark;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Render-thread state machine. No game types, disk writes or background work. */
public final class BenchmarkRunner {
    public enum Phase { IDLE, WARMUP, MEASURING, PAUSED, COMPLETE, CANCELLED }
    public record ToggleResult(String id, String displayName, List<FrameStats> rounds,
                               BenchmarkVerdict verdict) { }
    public record SkippedToggle(String id, String displayName, String reason) { }
    private record Entry(BenchmarkToggle toggle, boolean original) { }

    private final List<BenchmarkToggle> toggles;
    private final BenchmarkPlan plan;
    private final LongSupplier clock;
    private final List<Entry> entries = new ArrayList<>();
    private final List<ToggleResult> results = new ArrayList<>();
    private final List<SkippedToggle> skipped = new ArrayList<>();
    private final List<FrameStats> rounds = new ArrayList<>();
    private final List<Long> samples = new ArrayList<>();
    private Phase phase = Phase.IDLE;
    private int toggleIndex;
    private int round;
    private int completedRounds;
    private boolean confirm;
    private boolean all = true;
    private int plannedRounds;
    private int roundRedos;
    private boolean pairRetried;
    private boolean hitchNoisy;
    private double progressHighWater;
    private static final long HITCH_NANOS = 250_000_000L;
    private static final int MAX_ROUND_REDOS = 2;
    private BenchmarkToggle measuringToggle;
    private BenchmarkRecommendations.Resolved recommendations;
    private long windowStart;
    private long previousFrame;
    private double windowProgress;
    private BenchmarkVerdict confirmation;

    public BenchmarkRunner(List<BenchmarkToggle> toggles, LongSupplier clock) {
        this(toggles, clock, BenchmarkPlan.DEFAULT);
    }

    public BenchmarkRunner(List<BenchmarkToggle> toggles, LongSupplier clock, BenchmarkPlan plan) {
        this.toggles = List.copyOf(toggles);
        this.clock = Objects.requireNonNull(clock);
        this.plan = Objects.requireNonNull(plan);
        HashSet<String> ids = new HashSet<>();
        for (BenchmarkToggle toggle : toggles) {
            if (!ids.add(toggle.id())) throw new IllegalArgumentException("Duplicate toggle id: " + toggle.id());
        }
    }

    /** A runner represents one run; create a fresh runner for the next run. */
    public void start() {
        if (phase != Phase.IDLE) throw new IllegalStateException("Benchmark already started");
        for (BenchmarkToggle toggle : toggles) {
            boolean original = toggle.current();
            if (toggle.available()) entries.add(new Entry(toggle, original));
            else skipped.add(new SkippedToggle(toggle.id(), toggle.displayName(), toggle.unavailableReason()));
        }
        if (entries.isEmpty()) {
            phase = Phase.COMPLETE;
            return;
        }
        plannedRounds = 4 * (entries.size() + 2); // ALL, each switch, optional CONFIRM.
        beginRound(clock.getAsLong());
    }

    private void beginRound(long now) {
        samples.clear();
        windowProgress = 0;
        windowStart = previousFrame = now;
        phase = Phase.WARMUP;
        boolean enabled = BenchmarkPlan.enabled(round);
        if (all || confirm) {
            for (Entry entry : entries) {
                entry.toggle().applySession(all ? enabled
                        : enabled ? recommendations.states().get(entry.toggle().id()) : entry.original());
            }
        } else {
            BenchmarkToggle toggle = entries.get(toggleIndex).toggle();
            if (measuringToggle == null) {
                measuringToggle = toggle;
                toggle.beginMeasure();
            }
            toggle.applySession(enabled);
        }
    }

    public void onFrame(long nowNanos, boolean focused, boolean screenBlocking) {
        if (!active()) return;
        progressPercent();
        if (!focused || screenBlocking) {
            samples.clear();
            windowProgress = 0;
            phase = Phase.PAUSED;
            return;
        }
        if (phase == Phase.PAUSED) {
            beginRound(nowNanos);
            return;
        }
        long elapsed = nowNanos - windowStart;
        if (elapsed < 0) { // A bad clock must not produce negative frame durations.
            beginRound(nowNanos);
            return;
        }
        if (phase == Phase.WARMUP) {
            windowProgress = Math.min(elapsed, plan.warmupNanos());
            if (elapsed >= plan.warmupNanos()) {
                phase = Phase.MEASURING;
                windowStart = previousFrame = nowNanos;
            }
            return;
        }
        if (nowNanos > previousFrame) samples.add(nowNanos - previousFrame);
        previousFrame = nowNanos;
        windowProgress = plan.warmupNanos() + Math.min(elapsed, plan.measureNanos());
        if (elapsed < plan.measureNanos()) return;
        FrameStats stats = FrameStats.from(samples);
        boolean hitched = samples.stream().anyMatch(nanos -> nanos > HITCH_NANOS)
                || stats.p99Ms() > 4 * stats.medianMs();
        completedRounds++;
        // A discarded round counts as extra work, including its full warmup on the redo.
        if (hitched && roundRedos < MAX_ROUND_REDOS) {
            roundRedos++;
            plannedRounds++;
            beginRound(nowNanos);
            return;
        }
        hitchNoisy |= hitched;
        roundRedos = 0;
        rounds.add(stats);
        if (++round == 4) {
            BenchmarkVerdict verdict = BenchmarkVerdict.from(rounds, hitchNoisy);
            if (verdict.noisePercent() > 10 && !pairRetried) {
                pairRetried = true;
                plannedRounds += 4;
                round = 0;
                rounds.clear();
                hitchNoisy = false;
                beginRound(nowNanos);
                return;
            }
            if (confirm) {
                confirmation = verdict;
                finish();
                return;
            }
            if (all) {
                results.add(new ToggleResult("ALL", "All GPU paths", List.copyOf(rounds), verdict));
                restore();
                all = false;
            } else {
                Entry entry = entries.get(toggleIndex);
                results.add(new ToggleResult(entry.toggle().id(), entry.toggle().displayName(),
                        List.copyOf(rounds), verdict));
                entry.toggle().applySession(entry.original());
                endMeasure();
                if (++toggleIndex == entries.size()) {
                    Map<String, Boolean> originals = new LinkedHashMap<>();
                    Map<String, BenchmarkVerdict.Recommendation> verdicts = new LinkedHashMap<>();
                    entries.forEach(e -> originals.put(e.toggle().id(), e.original()));
                    results.stream().filter(result -> !result.id().equals("ALL"))
                            .forEach(result -> verdicts.put(result.id(), result.verdict().recommendation()));
                    recommendations = BenchmarkRecommendations.resolve(originals, verdicts);
                    confirm = !recommendations.states().equals(originals);
                    if (!confirm) {
                        plannedRounds -= 4;
                        finish();
                        return;
                    }
                }
            }
            round = 0;
            rounds.clear();
            pairRetried = false;
            hitchNoisy = false;
        }
        beginRound(nowNanos);
    }

    private void finish() {
        restore();
        phase = Phase.COMPLETE;
        windowProgress = 0;
    }

    public boolean noChangeRecommended() {
        return recommendations != null && !confirm;
    }

    /** Planned windows, including scheduled redos; CONFIRM is reserved until resolution. */
    public double estimatedTotalSeconds() {
        return (plan.warmupNanos() + (double) plan.measureNanos()) / 1_000_000_000.0 * plannedRounds;
    }

    private void endMeasure() {
        if (measuringToggle != null) {
            BenchmarkToggle toggle = measuringToggle;
            measuringToggle = null;
            toggle.endMeasure();
        }
    }

    private void restore() {
        endMeasure();
        for (Entry entry : entries) entry.toggle().applySession(entry.original());
    }

    public void cancel() {
        if (!active()) return;
        restore();
        samples.clear();
        phase = Phase.CANCELLED;
    }

    public boolean active() {
        return phase == Phase.WARMUP || phase == Phase.MEASURING || phase == Phase.PAUSED;
    }

    public Phase phase() { return phase; }
    public List<ToggleResult> results() { return List.copyOf(results); }
    public List<SkippedToggle> skipped() { return List.copyOf(skipped); }
    public BenchmarkVerdict confirmation() { return confirmation; }
    public BenchmarkRecommendations.Resolved recommendations() { return recommendations; }

    public BenchmarkStep step() {
        if (entries.isEmpty() || !active()) return null;
        Entry entry = entries.get(Math.min(toggleIndex, entries.size() - 1));
        return new BenchmarkStep(all ? "ALL" : confirm ? "CONFIRM" : entry.toggle().id(),
                all ? "All GPU paths" : confirm ? "CONFIRM" : entry.toggle().displayName(),
                round + 1, BenchmarkPlan.enabled(round), confirm);
    }

    public double progressPercent() {
        if (phase == Phase.COMPLETE) return 100;
        if (entries.isEmpty()) return 0;
        double fraction = windowProgress / (plan.warmupNanos() + (double) plan.measureNanos());
        // New work or a pause must never make the displayed progress go backwards.
        progressHighWater = Math.max(progressHighWater,
                Math.min(99.9, 100 * (completedRounds + fraction) / plannedRounds));
        return progressHighWater;
    }
}
