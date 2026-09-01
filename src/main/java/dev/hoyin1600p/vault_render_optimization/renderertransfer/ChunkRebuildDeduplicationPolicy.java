package dev.hoyin1600p.vault_render_optimization.renderertransfer;

public final class ChunkRebuildDeduplicationPolicy {
    private ChunkRebuildDeduplicationPolicy() {
    }

    public static boolean hasEquivalentPending(
            boolean importantRequest,
            boolean regularPending,
            boolean importantPending
    ) {
        return importantRequest ? importantPending : regularPending || importantPending;
    }

    public static boolean hasActiveTask(boolean taskPresent, boolean cancelled, boolean done) {
        return taskPresent && !cancelled && !done;
    }
}
