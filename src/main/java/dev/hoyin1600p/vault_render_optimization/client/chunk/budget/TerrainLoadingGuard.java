package dev.hoyin1600p.vault_render_optimization.client.chunk.budget;

import java.util.Arrays;
import java.util.Locale;

/** Safety gate, not a replacement scheduler. Loading or delayed work gets native throughput. */
public final class TerrainLoadingGuard {
    public static final int INITIAL_BUILD = 2; // Exact enum contract is fingerprinted and tested.
    public static final long WAIT_LIMIT = 250_000_000L;
    public static final long RECOVERY_DELAY = 500_000_000L;
    private final long memoryWatermark;
    private final long[] busySince = new long[5];
    private final boolean[] busy = new boolean[5];
    private final int[] requests = new int[5];
    private boolean initialBarrier = true, pacing, recovering;
    private long recoveryStarted, longestBusy;
    private int queuedWorkers, activeWorkers, completed;
    private String reason = "startup/loading safety";
    private long nativeCycles, pacedCycles;

    public TerrainLoadingGuard(long memoryWatermark) { this.memoryWatermark = memoryWatermark; }

    public void begin(long now, int[] requestCounts, int queuedWorkers, int activeWorkers,
            int completed, long nativeBytes, long oldestResultWait, boolean unbuiltResult) {
        System.arraycopy(requestCounts, 0, requests, 0, requests.length);
        this.queuedWorkers = queuedWorkers;
        this.activeWorkers = activeWorkers;
        this.completed = completed;
        longestBusy = 0;
        long total = 0;
        for (int type = 0; type < requests.length; type++) {
            total += requests[type];
            if (requests[type] > 0) {
                if (!busy[type]) busySince[type] = now;
                busy[type] = true;
                longestBusy = Math.max(longestBusy, now - busySince[type]);
            } else busy[type] = false;
        }
        if (requests[INITIAL_BUILD] > 0 || unbuiltResult) {
            initialBarrier = true;
            fallback("initial terrain pending");
        } else if (initialBarrier && (queuedWorkers > 0 || activeWorkers > 0 || completed > 0)) {
            // At enable/reload, already-running jobs have unknown origins. Don't throttle their completion.
            fallback("waiting for loading jobs to drain");
        } else {
            initialBarrier = false;
            if (total >= 128 || completed >= AdaptiveChunkBudget.MAX_RESULTS || nativeBytes >= memoryWatermark) {
                fallback("backlog pressure");
            } else if (longestBusy >= WAIT_LIMIT || oldestResultWait >= WAIT_LIMIT) {
                fallback("waiting work exceeded safety window");
            } else if (!pacing) {
                if (!recovering) { recovering = true; recoveryStarted = now; }
                if (now - recoveryStarted >= RECOVERY_DELAY) {
                    pacing = true;
                    recovering = false;
                    reason = "already-built terrain updates";
                } else reason = "native recovery cooldown";
            }
        }
        if (pacing) pacedCycles++; else nativeCycles++;
    }

    /** Rechecked immediately before upload, because workers may finish after begin(). */
    public void protectReadyTerrain() {
        initialBarrier = true;
        fallback("initial terrain completed during update");
    }

    private void fallback(String detail) { pacing = false; recovering = false; reason = detail; }
    public boolean pacing() { return pacing; }
    public boolean mayLimit(int type) { return pacing && type != INITIAL_BUILD; }
    public String status() {
        return String.format(Locale.ROOT,
                "%s (%s); pending requests [sort,important-sort,initial,rebuild,important-rebuild]=%s, worker queue=%d, active workers=%d, completed results=%d, longest pending-class busy=%.1fms, paced/native cycles=%d/%d",
                pacing ? "PACING" : "NATIVE FALLBACK", reason, Arrays.toString(requests), queuedWorkers,
                activeWorkers, completed, longestBusy / 1e6, pacedCycles, nativeCycles);
    }
}
