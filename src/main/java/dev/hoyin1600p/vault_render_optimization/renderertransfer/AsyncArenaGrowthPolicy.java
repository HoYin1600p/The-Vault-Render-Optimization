package dev.hoyin1600p.vault_render_optimization.renderertransfer;

public final class AsyncArenaGrowthPolicy {
    private AsyncArenaGrowthPolicy() {
    }

    public static int fixedGrowthIncrement(int initialCapacity, int divisor, int maxHeadroomMib) {
        long configuredHeadroom = Math.max(1L, initialCapacity / Math.max(1, divisor));
        long maxHeadroom = Math.max(1, maxHeadroomMib) * 1024L * 1024L;
        return (int) Math.min(configuredHeadroom, Math.min(maxHeadroom, Integer.MAX_VALUE));
    }

    public static int nextCapacity(int capacity, int used, int requestedElements, int growthIncrement) {
        long free = (long) capacity - used;
        long missing = Math.max(0L, (long) requestedElements - free);
        long required = (long) capacity + missing;
        long next = Math.max(required, (long) capacity + Math.max(1, growthIncrement));
        if (next > Integer.MAX_VALUE) {
            if (required > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Arena request exceeds the 32-bit renderer limit");
            }
            next = Integer.MAX_VALUE;
        }
        return (int) next;
    }
}
