package dev.hoyin1600p.vault_render_optimization.client.chunk.sorting;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import java.util.concurrent.atomic.LongAdder;

/** No optional renderer types: safe during startup selection and on vanilla. */
public final class IndexSortState {
    private static volatile boolean supported;
    private static volatile String reason = "selection pending";
    public static final LongAdder scheduled = new LongAdder();
    public static final LongAdder applied = new LongAdder();
    public static final LongAdder stale = new LongAdder();
    public static final LongAdder fallback = new LongAdder();
    public static final LongAdder vertexCopyBytesAvoided = new LongAdder();
    public static final LongAdder vertexUploadBytesAvoided = new LongAdder();

    private IndexSortState() { }

    public static void configure(boolean available, String detail) {
        supported = available;
        reason = detail;
    }

    public static boolean enabled() {
        return supported && ClientOptimizationConfig.indexOnlySorting
                && !ClientOptimizationConfig.compareModeEnabled();
    }

    public static String status() {
        return (supported ? enabled() ? "APPLIED" : "YIELDED" : "BLOCKED") + ": " + reason
                + "; scheduled=" + scheduled.sum() + ", applied=" + applied.sum()
                + ", stale=" + stale.sum() + ", fallback=" + fallback.sum()
                + ", vertex copy bytes avoided=" + vertexCopyBytesAvoided.sum()
                + ", vertex upload bytes avoided=" + vertexUploadBytesAvoided.sum();
    }
}
