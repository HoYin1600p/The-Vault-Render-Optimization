package dev.hoyin1600p.vault_render_optimization.renderertransfer;

import java.util.WeakHashMap;

/**
 * Aggregate excess CPU-native capacity, not VRAM or live mesh results. Keys are
 * buffer-owned history tokens, never buffers themselves. Pressure is advisory:
 * only the owning worker may trim, at its next safe start().
 */
public final class RetainedBufferPressure {
    private static final WeakHashMap<BufferTrimHistory, Integer> EXCESS = new WeakHashMap<>();
    private static long bytes;
    private static long lastRecount;
    private static boolean recounted;
    private static long trims, trimmedBytes;

    private RetainedBufferPressure() { }

    public static synchronized boolean observe(BufferTrimHistory key, int capacity, int initial,
                                                long budget, long now) {
        Integer previous = EXCESS.put(key, Math.max(0, capacity - initial));
        bytes += Math.max(0, capacity - initial) - (previous == null ? 0L : previous);
        if (!recounted || now - lastRecount >= 1_000_000_000L) {
            // Weak-key collection can only overestimate pressure between recounts.
            bytes = 0;
            for (int value : EXCESS.values()) bytes += value;
            lastRecount = now;
            recounted = true;
        }
        return bytes > budget;
    }

    public static synchronized void remove(BufferTrimHistory key) {
        Integer previous = EXCESS.remove(key);
        if (previous != null) bytes -= previous;
    }

    public static synchronized void recordTrim(int released) {
        trims++;
        trimmedBytes += released;
    }

    public static synchronized String status() {
        return "tracked buffers=" + EXCESS.size() + ", estimated excess bytes=" + bytes
                + ", adaptive trims=" + trims + ", cumulative released bytes=" + trimmedBytes;
    }
}
