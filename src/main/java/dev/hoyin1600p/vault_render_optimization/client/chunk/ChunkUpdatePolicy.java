package dev.hoyin1600p.vault_render_optimization.client.chunk;

/** Chooses native asynchronous scheduling without changing the user's renderer settings. */
public final class ChunkUpdatePolicy {
    private ChunkUpdatePolicy() {
    }

    public static boolean vroRequestsDeferral(boolean enabled, boolean compareMode) {
        return enabled && !compareMode;
    }

    public static boolean effectiveDeferral(boolean nativeDeferred, boolean enabled, boolean compareMode) {
        return nativeDeferred || vroRequestsDeferral(enabled, compareMode);
    }
}
