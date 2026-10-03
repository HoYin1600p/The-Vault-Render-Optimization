package dev.hoyin1600p.vault_render_optimization.client.particle;

/** Render-thread sampling gate. No world or particle references are retained. */
final class CensusInterval {
    static final long INTERVAL_NANOS = 250_000_000L;
    private boolean sampled;
    private long last;

    boolean due(long now) {
        if (sampled && now - last < INTERVAL_NANOS) return false;
        sampled = true;
        last = now;
        return true;
    }

    void reset() { sampled = false; }
}
