package dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import java.util.concurrent.atomic.LongAdder;

public final class SophisticatedStorageDiagnostics {
    private static final LongAdder FRONT_FACE_RENDERED = new LongAdder();
    private static final LongAdder BACK_FACE_SKIPPED = new LongAdder();
    private static final LongAdder COVERED_FACE_SKIPPED = new LongAdder();
    private static final LongAdder COUNT_LABELS_RENDERED = new LongAdder();
    private static final LongAdder COUNT_CACHE_HITS = new LongAdder();
    private static final LongAdder COUNT_CACHE_MISSES = new LongAdder();
    private static final LongAdder FILL_BARS_RENDERED = new LongAdder();
    private static final LongAdder MODEL_REBUILDS_ALLOWED = new LongAdder();
    private static final LongAdder COUNT_ONLY_REBUILDS_SKIPPED = new LongAdder();

    private SophisticatedStorageDiagnostics() {
    }

    public static boolean enabled() {
        return ClientOptimizationConfig.sophisticatedStorageDiagnostics;
    }

    public static void recordFrontFaceRendered() {
        if (enabled()) FRONT_FACE_RENDERED.increment();
    }

    public static void recordBackFaceSkipped() {
        if (enabled()) BACK_FACE_SKIPPED.increment();
    }

    public static void recordCoveredFaceSkipped() {
        if (enabled()) COVERED_FACE_SKIPPED.increment();
    }

    public static void recordCountLabelRendered() {
        if (enabled()) COUNT_LABELS_RENDERED.increment();
    }

    public static void recordCountCacheHit() {
        if (enabled()) COUNT_CACHE_HITS.increment();
    }

    public static void recordCountCacheMiss() {
        if (enabled()) COUNT_CACHE_MISSES.increment();
    }

    public static void recordFillBarRendered() {
        if (enabled()) FILL_BARS_RENDERED.increment();
    }

    public static void recordModelRebuildAllowed() {
        if (enabled()) MODEL_REBUILDS_ALLOWED.increment();
    }

    public static void recordCountOnlyRebuildSkipped() {
        if (enabled()) COUNT_ONLY_REBUILDS_SKIPPED.increment();
    }

    public static Snapshot snapshot() {
        return new Snapshot(
                enabled(),
                FRONT_FACE_RENDERED.sum(),
                BACK_FACE_SKIPPED.sum(),
                COVERED_FACE_SKIPPED.sum(),
                COUNT_LABELS_RENDERED.sum(),
                COUNT_CACHE_HITS.sum(),
                COUNT_CACHE_MISSES.sum(),
                FILL_BARS_RENDERED.sum(),
                MODEL_REBUILDS_ALLOWED.sum(),
                COUNT_ONLY_REBUILDS_SKIPPED.sum()
        );
    }

    public static void reset() {
        FRONT_FACE_RENDERED.reset();
        BACK_FACE_SKIPPED.reset();
        COVERED_FACE_SKIPPED.reset();
        COUNT_LABELS_RENDERED.reset();
        COUNT_CACHE_HITS.reset();
        COUNT_CACHE_MISSES.reset();
        FILL_BARS_RENDERED.reset();
        MODEL_REBUILDS_ALLOWED.reset();
        COUNT_ONLY_REBUILDS_SKIPPED.reset();
    }

    public record Snapshot(
            boolean enabled,
            long frontFacesRendered,
            long backFacesSkipped,
            long coveredFacesSkipped,
            long countLabelsRendered,
            long countCacheHits,
            long countCacheMisses,
            long fillBarsRendered,
            long modelRebuildsAllowed,
            long countOnlyRebuildsSkipped
    ) {
    }
}
