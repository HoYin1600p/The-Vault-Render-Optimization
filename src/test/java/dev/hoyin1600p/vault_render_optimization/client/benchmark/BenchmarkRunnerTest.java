package dev.hoyin1600p.vault_render_optimization.client.benchmark;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class BenchmarkRunnerTest {
    private static final long MS = 1_000_000;
    private final AtomicLong clock = new AtomicLong();
    private final List<String> calls = new ArrayList<>();

    private final class Toggle implements BenchmarkToggle {
        final String id;
        boolean value;
        boolean available = true;
        int begins, ends;
        Toggle(String id, boolean value) { this.id = id; this.value = value; }
        public String id() { return id; }
        public String displayName() { return id; }
        public boolean current() { return value; }
        public boolean available() { return available; }
        public String unavailableReason() { return "test unavailable"; }
        public void applySession(boolean enabled) { value = enabled; calls.add(id + ":" + enabled); }
        public void beginMeasure() { begins++; }
        public void endMeasure() { ends++; }
    }

    private BenchmarkRunner runner(Toggle... toggles) {
        return new BenchmarkRunner(List.of(toggles), clock::get, new BenchmarkPlan(20 * MS, 40 * MS));
    }
    private void frame(BenchmarkRunner runner, long ms, boolean focused, boolean blocking) {
        clock.addAndGet(ms * MS);
        runner.onFrame(clock.get(), focused, blocking);
    }
    private void frame(BenchmarkRunner runner, long ms) { frame(runner, ms, true, false); }
    private void finishRound(BenchmarkRunner runner, int interval) {
        frame(runner, 20);
        for (int ms = 0; ms < 40; ms += interval) frame(runner, interval);
    }
    private void finish(BenchmarkRunner runner) {
        for (int i = 0; runner.active() && i < 100; i++) finishRound(runner, 10);
        assertFalse(runner.active(), "Runner must terminate within retry limits");
    }

    @Test void allAbbaFirstThenIsolatedSwitchesWithoutUnnecessaryConfirmation() {
        Toggle a = new Toggle("a", false), b = new Toggle("b", true);
        BenchmarkRunner runner = runner(a, b);
        runner.start();
        assertThrows(IllegalStateException.class, runner::start);
        assertEquals(0.96, runner.estimatedTotalSeconds(), 1e-9);
        for (int round = 0; round < 4; round++) {
            assertEquals("ALL", runner.step().toggleId());
            assertEquals("All GPU paths", runner.step().displayName());
            assertEquals(BenchmarkPlan.enabled(round), a.value);
            assertEquals(a.value, b.value);
            assertEquals(0, a.begins); // Composite uses raw switches, never dependency hooks.
            finishRound(runner, 10);
        }
        assertEquals("a", runner.step().toggleId());
        for (int round = 0; round < 4; round++) {
            assertEquals(BenchmarkPlan.enabled(round), a.value);
            assertTrue(b.value);
            finishRound(runner, 10);
        }
        for (int round = 0; round < 4; round++) {
            assertEquals("b", runner.step().toggleId());
            assertFalse(a.value);
            assertEquals(BenchmarkPlan.enabled(round), b.value);
            finishRound(runner, 10);
        }
        assertEquals(BenchmarkRunner.Phase.COMPLETE, runner.phase());
        assertEquals(100, runner.progressPercent());
        assertEquals(0.72, runner.estimatedTotalSeconds(), 1e-9);
        assertEquals(3, runner.results().size());
        assertEquals("ALL", runner.results().get(0).id());
        assertTrue(runner.noChangeRecommended());
        assertNull(runner.confirmation());
        assertFalse(a.value);
        assertTrue(b.value);
        for (var toggle : List.of(a, b)) { assertEquals(1, toggle.begins); assertEquals(1, toggle.ends); }
        for (var result : runner.results()) {
            assertEquals(4, result.rounds().size());
            assertEquals(BenchmarkVerdict.Recommendation.NO_DIFFERENCE, result.verdict().recommendation());
            assertFalse(result.verdict().noisy());
        }
        int writes = calls.size();
        runner.cancel(); frame(runner, 100);
        assertEquals(writes, calls.size());
    }

    @Test void warmupIsExcludedAndPauseRestartsRoundWithoutRepeatingHooksOrDecreasingProgress() {
        Toggle a = new Toggle("a", false);
        BenchmarkRunner runner = runner(a);
        runner.start();
        for (int i = 0; i < 4; i++) finishRound(runner, 10);
        frame(runner, 19);
        assertEquals(BenchmarkRunner.Phase.WARMUP, runner.phase());
        frame(runner, 1);
        assertEquals(BenchmarkRunner.Phase.MEASURING, runner.phase());
        frame(runner, 30);
        double progress = runner.progressPercent();
        frame(runner, 5, false, false);
        assertEquals(BenchmarkRunner.Phase.PAUSED, runner.phase());
        assertTrue(runner.progressPercent() >= progress);
        frame(runner, 5000, false, true);
        frame(runner, 100);
        assertEquals(BenchmarkRunner.Phase.WARMUP, runner.phase());
        assertEquals(1, runner.step().round());
        assertEquals(1, a.begins);
        finish(runner);
        var stats = runner.results().get(1).rounds().get(0);
        assertEquals(4, stats.frameCount());
        assertEquals(100, stats.averageFps(), 1e-9);
        assertFalse(a.value);
    }

    @Test void maxFrameHitchesRedoAtMostTwicePerRoundThenFlagNoisy() {
        BenchmarkRunner runner = runner(new Toggle("a", false));
        runner.start();
        double progress = 0;
        for (int round = 1; round <= 4; round++) {
            for (int attempt = 0; attempt < 3; attempt++) {
                assertEquals(round, runner.step().round());
                frame(runner, 20); frame(runner, 300);
                assertTrue(runner.progressPercent() >= progress);
                progress = runner.progressPercent();
            }
        }
        assertEquals("a", runner.step().toggleId());
        assertTrue(runner.results().get(0).verdict().noisy());
        assertEquals(4, runner.results().get(0).rounds().size());
        assertEquals(300, runner.results().get(0).rounds().get(0).medianMs());
        assertEquals(1.2, runner.estimatedTotalSeconds(), 1e-9); // 12 reserved + 8 retries.
        finish(runner);
    }

    @Test void p99HitchWithout250MsFrameIsDiscardedAndGoodRedoIsUsed() {
        BenchmarkRunner runner = runner(new Toggle("a", false));
        runner.start();
        frame(runner, 20);
        for (int i = 0; i < 5; i++) frame(runner, 1);
        frame(runner, 35); // Median 1 ms, p99 35 ms, maximum < 250 ms.
        assertEquals(1, runner.step().round());
        assertEquals(BenchmarkRunner.Phase.WARMUP, runner.phase());
        finish(runner);
        assertFalse(runner.results().get(0).verdict().noisy());
        assertEquals(10, runner.results().get(0).rounds().get(0).medianMs());
        assertEquals(0.54, runner.estimatedTotalSeconds(), 1e-9); // 8 accepted + one redo.
    }

    @Test void inconsistentPairRerunsOnceAndUsesRecoveredMeasurements() {
        for (boolean recover : new boolean[] {true, false}) {
            BenchmarkRunner runner = runner(new Toggle("a", false));
            runner.start();
            for (int round = 0; round < 4; round++) finishRound(runner, round == 3 ? 20 : 10);
            assertEquals("ALL", runner.step().toggleId());
            assertEquals(1, runner.step().round());
            for (int round = 0; round < 4; round++) finishRound(runner, !recover && round == 3 ? 20 : 10);
            assertEquals("a", runner.step().toggleId());
            assertEquals(!recover, runner.results().get(0).verdict().noisy());
            assertEquals(recover ? 0 : 200.0 / 3, runner.results().get(0).verdict().noisePercent(), 1e-9);
            finish(runner);
            assertEquals(0.72, runner.estimatedTotalSeconds(), 1e-9); // 8 accepted + four repeated.
        }
    }

    @Test void changedRecommendationConfirmsResolvedCombinationThenRestoresOriginals() {
        Toggle a = new Toggle("a", false), b = new Toggle("b", true);
        BenchmarkRunner runner = runner(a, b);
        runner.start();
        int confirmations = 0;
        for (int i = 0; runner.active() && i < 100; i++) {
            var step = runner.step();
            int interval;
            if (step.confirm()) {
                confirmations++;
                assertEquals(step.enabled(), a.value);
                assertEquals(!step.enabled(), b.value);
                interval = step.enabled() ? 5 : 10;
            } else interval = step.toggleId().equals("ALL") ? 10
                    : step.toggleId().equals("a") == step.enabled() ? 5 : 10;
            finishRound(runner, interval);
        }
        assertEquals(4, confirmations);
        assertEquals(BenchmarkVerdict.Recommendation.ON, runner.results().get(1).verdict().recommendation());
        assertEquals(BenchmarkVerdict.Recommendation.OFF, runner.results().get(2).verdict().recommendation());
        assertEquals(100, runner.confirmation().deltaPercent(), 1e-9);
        assertFalse(runner.noChangeRecommended());
        assertFalse(a.value); assertTrue(b.value);
    }

    @Test void confirmationChecksResolvedDependenciesEvenWhenBaseLoses() {
        Toggle base = new Toggle("gpu_entity_models", false), items = new Toggle("gpu_items", false);
        BenchmarkRunner runner = runner(base, items);
        runner.start();
        for (int i = 0; i < 12; i++) {
            var step = runner.step();
            finishRound(runner, step.toggleId().equals("ALL") ? 10
                    : step.toggleId().equals("gpu_items") == step.enabled() ? 5 : 10);
        }
        assertTrue(runner.step().confirm());
        assertTrue(base.value); assertTrue(items.value);
        assertTrue(runner.recommendations().required().contains("gpu_entity_models"));
        runner.cancel();
        assertFalse(base.value); assertFalse(items.value);
    }

    @Test void alreadyRecommendedOriginalStateSkipsConfirmation() {
        Toggle a = new Toggle("a", true);
        BenchmarkRunner runner = runner(a);
        runner.start();
        for (int i = 0; i < 8; i++) finishRound(runner, runner.step().enabled() ? 5 : 10);
        assertTrue(runner.noChangeRecommended());
        assertNull(runner.confirmation());
        assertTrue(a.value);
    }

    @Test void cancelRestoresDuringCompositePerSwitchPauseAndConfirmation() {
        for (int completed : new int[] {0, 4, 8}) {
            Toggle a = new Toggle("a", false);
            BenchmarkRunner runner = runner(a);
            runner.start();
            for (int i = 0; i < completed; i++) finishRound(runner, runner.step().enabled() ? 5 : 10);
            if (completed == 8) assertTrue(runner.step().confirm());
            frame(runner, 20); frame(runner, 1, false, true);
            runner.cancel();
            assertFalse(a.value);
            assertEquals(a.begins, a.ends);
            assertEquals(BenchmarkRunner.Phase.CANCELLED, runner.phase());
        }
    }

    @Test void compositeIncludesEveryShaderVariantWithoutInvokingDependencyOverrides() {
        Toggle base = new Toggle("gpu_entity_models", false);
        Toggle entities = new Toggle("gpu_entity_models_with_shaders", false);
        Toggle items = new Toggle("gpu_items", true);
        Toggle particles = new Toggle("gpu_particles", false);
        Toggle shaderParticles = new Toggle("gpu_particles_with_shaders", true);
        var switches = List.of(base, entities, items, particles, shaderParticles);
        BenchmarkRunner runner = runner(base, entities, items, particles, shaderParticles);
        runner.start();
        for (int round = 0; round < 4; round++) {
            for (var toggle : switches) {
                assertEquals(BenchmarkPlan.enabled(round), toggle.value);
                assertEquals(0, toggle.begins);
            }
            finishRound(runner, 10);
        }
        runner.cancel();
        assertFalse(base.value); assertFalse(entities.value); assertTrue(items.value);
        assertFalse(particles.value); assertTrue(shaderParticles.value);
    }

    @Test void inconsistentPerSwitchPairAlsoRerunsExactlyOnceAndFlagsPersistentNoise() {
        BenchmarkRunner runner = runner(new Toggle("a", false));
        runner.start();
        for (int i = 0; i < 4; i++) finishRound(runner, 10);
        for (int attempt = 0; attempt < 2; attempt++) {
            for (int i = 0; i < 4; i++) {
                assertEquals("a", runner.step().toggleId());
                assertEquals(i + 1, runner.step().round());
                finishRound(runner, i == 3 ? 20 : 10);
            }
        }
        assertFalse(runner.active());
        assertTrue(runner.results().get(1).verdict().noisy());
        assertTrue(runner.noChangeRecommended());
        assertEquals(0.72, runner.estimatedTotalSeconds(), 1e-9);
    }

    @Test void unavailableTogglesNeverAppliedIncludingComposite() {
        Toggle skip = new Toggle("skip", true); skip.available = false;
        BenchmarkRunner runner = runner(skip, new Toggle("a", false));
        runner.start(); finish(runner);
        assertEquals(List.of(new BenchmarkRunner.SkippedToggle("skip", "skip", "test unavailable")), runner.skipped());
        assertTrue(calls.stream().noneMatch(call -> call.startsWith("skip:")));
        BenchmarkRunner empty = runner(skip); empty.start();
        assertEquals(BenchmarkRunner.Phase.COMPLETE, empty.phase());
        assertEquals(0, empty.estimatedTotalSeconds());
    }

    @Test void defaultsAndValidation() {
        assertEquals(3_000_000_000L, BenchmarkPlan.DEFAULT.warmupNanos());
        assertEquals(12_000_000_000L, BenchmarkPlan.DEFAULT.measureNanos());
        assertThrows(IllegalArgumentException.class, () -> new BenchmarkPlan(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> new BenchmarkPlan(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new BenchmarkPlan(Long.MAX_VALUE, 1));
        assertThrows(IllegalArgumentException.class, () -> BenchmarkPlan.enabled(4));
        assertThrows(IllegalArgumentException.class, () -> runner(new Toggle("a", false), new Toggle("a", true)));
    }
}
