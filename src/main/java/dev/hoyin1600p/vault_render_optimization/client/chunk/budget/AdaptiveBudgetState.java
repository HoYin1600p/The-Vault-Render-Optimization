package dev.hoyin1600p.vault_render_optimization.client.chunk.budget;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;

/** Only text is published globally; no world, controller, queue or worker references. */
public final class AdaptiveBudgetState {
    private static volatile boolean supported;
    private static volatile String reason = "selection pending";
    private static volatile String observation = "no active renderer observed";
    private AdaptiveBudgetState() { }
    public static void configure(boolean available, String detail) { supported = available; reason = detail; }
    public static boolean enabled() {
        return supported && ClientOptimizationConfig.adaptiveChunkBudget && !ClientOptimizationConfig.compareModeEnabled();
    }
    public static void observe(String message) { observation = message; }
    public static String status() {
        return (supported ? enabled() ? "ENABLED" : "YIELDED" : "BLOCKED") + ": " + reason + "; " + observation;
    }
}
