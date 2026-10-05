package dev.hoyin1600p.vault_render_optimization.client.benchmark;

import java.util.List;

/** Delta, noise and recommendations use FPS derived from each round's median frame time.
 * Mean average FPS remains descriptive; it does not drive the recommendation. */
public record BenchmarkVerdict(double meanFpsOn, double meanFpsOff,
                               double medianFpsOn, double medianFpsOff, double deltaPercent,
                               double noisePercent, Recommendation recommendation, boolean noisy) {
    public enum Recommendation { ON, OFF, NO_DIFFERENCE }

    /** Arguments follow ABBA: ON, OFF, OFF, ON; supplied FPS is the decision metric. */
    public static BenchmarkVerdict from(double on1, double off1, double off2, double on2) {
        for (double fps : new double[] {on1, off1, off2, on2}) {
            if (!Double.isFinite(fps) || fps <= 0) {
                throw new IllegalArgumentException("Round FPS must be finite and positive");
            }
        }
        double on = on1 / 2 + on2 / 2;
        double off = off1 / 2 + off2 / 2;
        double delta = (on / off - 1) * 100;
        double noise = Math.max(Math.abs(on1 - on2) / on, Math.abs(off1 - off2) / off) * 100;
        double threshold = Math.max(2.0, 2 * noise);
        Recommendation recommendation = delta >= threshold ? Recommendation.ON
                : delta <= -threshold ? Recommendation.OFF : Recommendation.NO_DIFFERENCE;
        return new BenchmarkVerdict(on, off, on, off, delta, noise, recommendation, noise > 10);
    }

    public static BenchmarkVerdict from(List<FrameStats> rounds, boolean hitchNoisy) {
        if (rounds.size() != 4) throw new IllegalArgumentException("Four ABBA rounds required");
        BenchmarkVerdict metric = from(rounds.get(0).medianFps(), rounds.get(1).medianFps(),
                rounds.get(2).medianFps(), rounds.get(3).medianFps());
        return new BenchmarkVerdict((rounds.get(0).averageFps() + rounds.get(3).averageFps()) / 2,
                (rounds.get(1).averageFps() + rounds.get(2).averageFps()) / 2,
                metric.medianFpsOn(), metric.medianFpsOff(), metric.deltaPercent(), metric.noisePercent(),
                metric.recommendation(), hitchNoisy || metric.noisy());
    }
}
