package dev.hoyin1600p.vault_render_optimization.client.chunk.budget;

import java.util.Arrays;
import java.util.Locale;

/** Per-renderer feedback controller. No Minecraft, queues, GPU resources or global training data. */
public final class AdaptiveChunkBudget {
    public static final long MIB = 1024L * 1024;
    public static final long MAX_WAIT_NS = 250_000_000L;
    public static final int MAX_RESULTS = 128;
    private static final double MIN_BUDGET = 250_000, MAX_BUDGET = 1_500_000;
    private final long memoryWatermark;
    private final long[] waitingSince = new long[5];
    private final boolean[] wasWaiting = new boolean[5];
    private final double[] buildNanos = {6_000_000, 1_000_000};
    private final double[] outputBytes = {2 * MIB, 128 * 1024};
    private double nsPerByte = 2.0, uploadBudget = 750_000;
    private double previousUploadNanos, previousFrameNanos = 16_666_667;
    private long previousFrameAt, frameAt, queuedBytes, oldestWait;
    private int queuedResults, remaining, rescueType = -1, stableFrames;
    private double workerRemaining, outputRemaining, expectedUploadRemaining;
    private long submitted, uploaded, uploadNanosTotal, overshoots;
    private final long[] frameSamples = new long[256], uploadSamples = new long[256];
    private int frameSampleCount, uploadSampleCount, frameCursor, uploadCursor;
    private long peakWait;
    private boolean closed, hasFrame;

    public AdaptiveChunkBudget(long maxHeapBytes) {
        memoryWatermark = Math.max(16 * MIB, Math.min(64 * MIB, maxHeapBytes / 128));
    }

    /** All timing is monotonic; long pauses do not teach the controller to admit more work. */
    public synchronized void beginFrame(long now, int workers, int activeWorkers, int nativeAvailable,
            long pendingBytes, int pendingCount, long oldestWaitNanos, boolean[] waiting) {
        frameAt = now;
        submittedThisFrame = 0;
        long interval = !hasFrame ? 16_666_667 : now - previousFrameAt;
        hasFrame = true;
        previousFrameAt = now;
        boolean pause = interval <= 0 || interval > 250_000_000;
        double frame = pause ? 16_666_667 : Math.max(1_000_000, interval);
        if (!pause) {
            frameSamples[frameCursor++ & 255] = interval;
            frameSampleCount = Math.min(256, frameSampleCount + 1);
        }
        // Above 60Hz's frame interval, slower frames never increase the upload ceiling.
        double ceiling = Math.max(MIN_BUDGET, Math.min(MAX_BUDGET, frame * 0.10));
        boolean pressure = !pause && (previousUploadNanos > ceiling
                || frame > previousFrameNanos * 1.20 && previousUploadNanos > MIN_BUDGET);
        if (pressure) {
            uploadBudget = Math.max(MIN_BUDGET, uploadBudget * 0.80);
            stableFrames = 0;
        } else if (!pause && pendingCount > 0 && previousUploadNanos > 0
                && previousUploadNanos < ceiling * 0.80 && ++stableFrames >= 8) {
            uploadBudget *= 1.02;
            stableFrames = 0;
        }
        uploadBudget = Math.min(ceiling, uploadBudget);
        previousFrameNanos += (frame - previousFrameNanos) * 0.10;
        previousUploadNanos = 0;
        queuedBytes = pendingBytes;
        queuedResults = pendingCount;
        oldestWait = Math.max(0, oldestWaitNanos);
        peakWait = Math.max(peakWait, oldestWait);
        remaining = pendingBytes >= memoryWatermark || pendingCount >= MAX_RESULTS ? 0
                : Math.min(32, Math.max(0, nativeAvailable));
        // Bound new worker work and reserve estimated space for already admitted native tasks.
        int outstanding = Math.max(0, workers * 2 - nativeAvailable) + Math.max(0, activeWorkers);
        double estimate = Math.max(outputBytes[0], outputBytes[1]);
        outputRemaining = memoryWatermark - pendingBytes - outstanding * estimate;
        if (outputRemaining <= 0) remaining = 0;
        workerRemaining = Math.max(1, workers) * Math.min(8_000_000, frame * 0.5);
        expectedUploadRemaining = Math.max(0, uploadBudget * 2 - pendingBytes * nsPerByte);
        rescueType = -1;
        long oldest = now;
        for (int type = 0; type < waitingSince.length; type++) {
            if (waiting[type] && !wasWaiting[type]) waitingSince[type] = now;
            wasWaiting[type] = waiting[type];
            if (waiting[type] && now - waitingSince[type] >= MAX_WAIT_NS
                    && waitingSince[type] <= oldest) {
                oldest = waitingSince[type];
                rescueType = type;
            }
        }
    }

    /** Reserves one admission for an aged update class, without draining or dropping native requests. */
    public synchronized int limit(int nativeBudget, int type, boolean sort) {
        if (closed) return nativeBudget;
        int available = Math.min(nativeBudget, remaining);
        if (available <= 0) return 0;
        // Reserve cost as well as a slot: a cheap earlier sort must not spend an aged rebuild's allowance.
        if (rescueType >= 0 && rescueType != type) return 0;
        int kind = sort ? 1 : 0;
        int costLimit = (int) Math.min(workerRemaining / buildNanos[kind],
                Math.min(outputRemaining / outputBytes[kind],
                        expectedUploadRemaining / Math.max(1, outputBytes[kind] * nsPerByte)));
        // One indivisible job may exceed estimates. Permit progress only when the result queue is empty.
        if (queuedResults == 0 && submittedThisFrame == 0) costLimit = Math.max(1, costLimit);
        int allowance = Math.min(remaining, Math.max(0, costLimit));
        return Math.min(available, allowance);
    }

    private int submittedThisFrame;

    public synchronized void admitted(int type, boolean sort) {
        int kind = sort ? 1 : 0;
        remaining = Math.max(0, remaining - 1);
        workerRemaining -= buildNanos[kind];
        outputRemaining -= outputBytes[kind];
        expectedUploadRemaining -= outputBytes[kind] * nsPerByte;
        submittedThisFrame++;
        submitted++;
        waitingSince[type] = frameAt;
        if (rescueType == type) rescueType = -1;
    }

    public synchronized void observeBuild(boolean sort, long nanos, long bytes) {
        if (closed || nanos <= 0 || bytes < 0) return;
        int kind = sort ? 1 : 0;
        // Fast response to cost increases; slow recovery to prevent over-admitting after a cheap sample.
        buildNanos[kind] = learn(buildNanos[kind], Math.max(50_000, Math.min(1_000_000_000L, nanos)));
        outputBytes[kind] = learn(outputBytes[kind], Math.max(4096, bytes));
    }

    private static double learn(double prior, double sample) {
        return prior + (sample - prior) * (sample > prior ? 0.5 : 0.05);
    }

    public synchronized long uploadByteAllowance() {
        return Math.max(4096, Math.min(8 * MIB, (long) (uploadBudget / nsPerByte)));
    }

    public synchronized long uploadTimeAllowance() { return (long) uploadBudget; }

    public synchronized void observeUpload(long nanos, long bytes, int results) {
        if (closed || results <= 0) return;
        previousUploadNanos += Math.max(0, nanos);
        uploadNanosTotal += Math.max(0, nanos);
        uploaded += results;
        uploadSamples[uploadCursor++ & 255] = Math.max(0, nanos);
        uploadSampleCount = Math.min(256, uploadSampleCount + 1);
        if (nanos > uploadBudget) overshoots++;
        if (bytes > 0 && nanos > 0) nsPerByte = learn(nsPerByte, Math.max(0.01, (double) nanos / bytes));
    }

    public synchronized void close() { closed = true; }
    public long memoryWatermark() { return memoryWatermark; }
    public synchronized String snapshot() {
        return String.format(Locale.ROOT,
                "budget=%.3fms, byte allowance=%d, queued native bytes=%d/%d, queued results=%d, oldest observed wait=%.1fms (peak %.1fms), submitted=%d, uploaded=%d, upload CPU total=%.1fms, budget overshoots=%d, recent update-interval p95/p99=%.2f/%.2fms, upload CPU p95=%.2fms",
                uploadBudget / 1e6, uploadByteAllowance(), queuedBytes, memoryWatermark, queuedResults,
                oldestWait / 1e6, peakWait / 1e6, submitted, uploaded, uploadNanosTotal / 1e6, overshoots,
                percentile(frameSamples, frameSampleCount, 0.95) / 1e6,
                percentile(frameSamples, frameSampleCount, 0.99) / 1e6,
                percentile(uploadSamples, uploadSampleCount, 0.95) / 1e6);
    }

    private static long percentile(long[] samples, int count, double quantile) {
        if (count == 0) return 0;
        long[] sorted = Arrays.copyOf(samples, count);
        Arrays.sort(sorted);
        return sorted[Math.min(count - 1, (int) Math.ceil(count * quantile) - 1)];
    }
}
