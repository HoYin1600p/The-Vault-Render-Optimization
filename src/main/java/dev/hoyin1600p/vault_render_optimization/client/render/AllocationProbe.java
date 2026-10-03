package dev.hoyin1600p.vault_render_optimization.client.render;

import java.lang.management.ManagementFactory;

/**
 * Measures heap bytes allocated by the render thread per level frame, for A/B checks of allocation
 * removals (for example the frustum test). Measurement only: it reads the JVM's per-thread allocation counter
 * at the start and end of a window and never changes rendering.
 */
public final class AllocationProbe {
    private static final com.sun.management.ThreadMXBean THREADS = threads();
    private static volatile long frames;
    private static long renderThreadId = -1L;
    private static long startBytes;
    private static long startFrames;
    private static long startNanos;
    private static boolean running;

    private AllocationProbe() {
    }

    private static com.sun.management.ThreadMXBean threads() {
        java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        if (bean instanceof com.sun.management.ThreadMXBean sun && sun.isThreadAllocatedMemorySupported()) {
            try {
                sun.setThreadAllocatedMemoryEnabled(true);
                return sun;
            } catch (UnsupportedOperationException | SecurityException ignored) {
                return null;
            }
        }
        return null;
    }

    /** Called at the start of every level frame on the render thread. */
    public static void frame() {
        frames++;
    }

    /** Starts a window. Must run on the render thread (client commands do). */
    public static String start() {
        if (THREADS == null) return "Allocation probe unavailable: this JVM does not report per-thread allocation.";
        renderThreadId = Thread.currentThread().getId();
        startBytes = THREADS.getThreadAllocatedBytes(renderThreadId);
        startFrames = frames;
        startNanos = System.nanoTime();
        running = true;
        return "Allocation probe started on thread '" + Thread.currentThread().getName() + "'.";
    }

    /** Reports the window since {@link #start()}; the window keeps running. */
    public static String report() {
        if (THREADS == null) return "Allocation probe unavailable: this JVM does not report per-thread allocation.";
        if (!running) return "Allocation probe is not running; use start first.";
        long bytes = THREADS.getThreadAllocatedBytes(renderThreadId) - startBytes;
        long frameCount = frames - startFrames;
        double seconds = (System.nanoTime() - startNanos) / 1.0e9;
        return String.format(java.util.Locale.ROOT,
                "Render-thread allocation: %.1f KiB/frame, %.1f MiB/s over %d frames in %.1f s.",
                frameCount == 0 ? 0.0 : bytes / 1024.0 / frameCount,
                seconds <= 0.0 ? 0.0 : bytes / 1048576.0 / seconds, frameCount, seconds);
    }
}
