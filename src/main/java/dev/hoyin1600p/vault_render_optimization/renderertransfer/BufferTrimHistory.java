package dev.hoyin1600p.vault_render_optimization.renderertransfer;

/** Owner-thread policy only. Never frees memory or touches an active writer. */
public final class BufferTrimHistory {
    public static final long WINDOW_NANOS = 30_000_000_000L;
    private long lastStart;
    private long windowStart;
    private int peak;
    private int lowDemandStarts;
    private boolean initialized;

    public int target(int capacity, int initial, int previousUsed, long now, boolean pressure) {
        boolean idle = initialized && now - lastStart >= WINDOW_NANOS;
        if (!initialized || now - windowStart >= WINDOW_NANOS) {
            peak = 0;
            windowStart = now;
        }
        peak = Math.max(peak, Math.max(0, previousUsed));
        initialized = true;
        lastStart = now;
        lowDemandStarts = previousUsed <= capacity / 4 ? Math.min(3, lowDemandStarts + 1) : 0;
        int demand = idle ? initial : pressure && lowDemandStarts >= 3
                ? Math.max(initial, previousUsed) : Math.max(initial, peak);
        // Power-of-two headroom avoids shrinking to each tiny fluctuation in demand.
        long rounded = Math.max(initial, demand <= 1 ? 1L : (long) Integer.highestOneBit(demand - 1) * 2L);
        int target = (int) Math.min(capacity, Math.min(Integer.MAX_VALUE, rounded));
        // Don't reallocate for small savings. Baseline hard-cap trimming is separate.
        return target <= capacity / 2 ? target : capacity;
    }
}
