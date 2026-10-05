package dev.hoyin1600p.vault_render_optimization.client.benchmark;

/** A session-only switch: implementations must never persist changes. */
public interface BenchmarkToggle {
    String id();
    String displayName();
    boolean current();
    void applySession(boolean enabled);
    boolean available();

    default void beginMeasure() { }
    default void endMeasure() { }

    default String unavailableReason() {
        return "unavailable in the current rendering environment";
    }
}
