package dev.hoyin1600p.vault_render_optimization.client.benchmark;

/** Round is one-based for display. For CONFIRM, ON means the recommended combination. */
public record BenchmarkStep(String toggleId, String displayName, int round,
                            boolean enabled, boolean confirm) {
}
