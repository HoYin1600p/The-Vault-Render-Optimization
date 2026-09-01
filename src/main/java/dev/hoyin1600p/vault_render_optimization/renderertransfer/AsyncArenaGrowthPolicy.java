package dev.hoyin1600p.vault_render_optimization.renderertransfer;

public final class AsyncArenaGrowthPolicy {
    private AsyncArenaGrowthPolicy() {
    }

    public static int nextCapacity(
            int capacity,
            int used,
            int requestedElements,
            int divisor,
            int maxHeadroomMib
    ) {
        long free = (long) capacity - used;
        long missing = Math.max(0L, (long) requestedElements - free);
        long required = (long) capacity + missing;
        long configuredHeadroom = Math.max(1L, capacity / Math.max(1, divisor));
        long maxHeadroom = Math.max(1, maxHeadroomMib) * 1024L * 1024L;
        long next = Math.max(required, (long) capacity + Math.min(configuredHeadroom, maxHeadroom));
        if (next > Integer.MAX_VALUE) {
            if (required > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Arena request exceeds the 32-bit renderer limit");
            }
            next = Integer.MAX_VALUE;
        }
        return (int) next;
    }
}
