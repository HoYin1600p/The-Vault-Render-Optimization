package dev.hoyin1600p.vault_render_optimization.diagnostics;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public final class ChunkRebuildDiagnostics {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long SUMMARY_INTERVAL_NS = 5_000_000_000L;
    private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("vault_render_optimization.chunkDiagnostics", "true"));

    private static final AtomicLong windowStartNs = new AtomicLong(System.nanoTime());
    private static final AtomicLong scheduleSequence = new AtomicLong();

    private static final LongAdder schedules = new LongAdder();
    private static final LongAdder importantSchedules = new LongAdder();
    private static final LongAdder offThreadSchedules = new LongAdder();

    private static final LongAdder updateChunksCount = new LongAdder();
    private static final LongAdder updateChunksNs = new LongAdder();
    private static final AtomicLong updateChunksMaxNs = new AtomicLong();

    private static final LongAdder uploadCount = new LongAdder();
    private static final LongAdder uploadNs = new LongAdder();
    private static final AtomicLong uploadMaxNs = new AtomicLong();

    private static final LongAdder setupBatchesCount = new LongAdder();
    private static final LongAdder setupBatchesNs = new LongAdder();
    private static final AtomicLong setupBatchesMaxNs = new AtomicLong();

    private static final LongAdder rebuildCount = new LongAdder();
    private static final LongAdder rebuildNs = new LongAdder();
    private static final AtomicLong rebuildMaxNs = new AtomicLong();

    private static final LongAdder stealTaskCount = new LongAdder();
    private static final LongAdder stealTaskNs = new LongAdder();
    private static final AtomicLong stealTaskMaxNs = new AtomicLong();

    private static final Map<String, LongAdder> scheduleEntrypoints = new ConcurrentHashMap<>();
    private static final Map<String, LongAdder> scheduleCallers = new ConcurrentHashMap<>();

    private ChunkRebuildDiagnostics() {
    }

    public static boolean enabled() {
        return ENABLED;
    }

    public static void recordSchedule(int x, int y, int z, boolean important) {
        if (!ENABLED) {
            return;
        }

        schedules.increment();
        if (important) {
            importantSchedules.increment();
        }
        if (!"Render thread".equals(Thread.currentThread().getName())) {
            offThreadSchedules.increment();
        }

        long sequence = scheduleSequence.incrementAndGet();
        if ((sequence & 255L) == 0L) {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            scheduleEntrypoints.computeIfAbsent(findLevelRendererEntrypoint(stack), ignored -> new LongAdder()).increment();
            scheduleCallers.computeIfAbsent(findUsefulCaller(stack), ignored -> new LongAdder()).increment();
        }

        maybeLogSummary();
    }

    public static void recordUpdateChunks(long durationNs) {
        recordDuration(durationNs, updateChunksCount, updateChunksNs, updateChunksMaxNs);
        maybeLogSummary();
    }

    public static void recordUpload(long durationNs) {
        recordDuration(durationNs, uploadCount, uploadNs, uploadMaxNs);
        maybeLogSummary();
    }

    public static void recordSetupBatches(long durationNs) {
        recordDuration(durationNs, setupBatchesCount, setupBatchesNs, setupBatchesMaxNs);
    }

    public static void recordRebuild(long durationNs) {
        recordDuration(durationNs, rebuildCount, rebuildNs, rebuildMaxNs);
    }

    public static void recordStealTask(long durationNs) {
        recordDuration(durationNs, stealTaskCount, stealTaskNs, stealTaskMaxNs);
    }

    private static void recordDuration(long durationNs, LongAdder count, LongAdder totalNs, AtomicLong maxNs) {
        if (!ENABLED) {
            return;
        }

        count.increment();
        totalNs.add(durationNs);
        updateMax(maxNs, durationNs);
    }

    private static void updateMax(AtomicLong maxNs, long candidate) {
        long current;
        do {
            current = maxNs.get();
            if (candidate <= current) {
                return;
            }
        } while (!maxNs.compareAndSet(current, candidate));
    }

    private static void maybeLogSummary() {
        long now = System.nanoTime();
        long start = windowStartNs.get();
        if (now - start < SUMMARY_INTERVAL_NS || !windowStartNs.compareAndSet(start, now)) {
            return;
        }

        long elapsedNs = now - start;
        long scheduleCount = schedules.sumThenReset();
        long importantScheduleCount = importantSchedules.sumThenReset();
        long offThreadScheduleCount = offThreadSchedules.sumThenReset();

        long updateCount = updateChunksCount.sumThenReset();
        long updateTotalNs = updateChunksNs.sumThenReset();
        long updateMax = updateChunksMaxNs.getAndSet(0L);

        long uploadTotalCount = uploadCount.sumThenReset();
        long uploadTotalNs = uploadNs.sumThenReset();
        long uploadMax = uploadMaxNs.getAndSet(0L);

        long setupCount = setupBatchesCount.sumThenReset();
        long setupTotalNs = setupBatchesNs.sumThenReset();
        long setupMax = setupBatchesMaxNs.getAndSet(0L);

        long rebuildTotalCount = rebuildCount.sumThenReset();
        long rebuildTotalNs = rebuildNs.sumThenReset();
        long rebuildMax = rebuildMaxNs.getAndSet(0L);

        long stealCount = stealTaskCount.sumThenReset();
        long stealTotalNs = stealTaskNs.sumThenReset();
        long stealMax = stealTaskMaxNs.getAndSet(0L);

        String topEntrypoints = drainTopEntries(scheduleEntrypoints);
        String topCallers = drainTopEntries(scheduleCallers);
        LOGGER.info(
                "[VRO chunk diagnostics] {} schedules={} important={} offThread={} updateChunks={} total={} max={} uploads={} total={} max={} setupBatches={} total={} max={} rebuilds={} total={} max={} stealTasks={} total={} max={} entrypoints={} callers={}",
                formatSeconds(elapsedNs),
                scheduleCount,
                importantScheduleCount,
                offThreadScheduleCount,
                updateCount,
                formatNs(updateTotalNs),
                formatNs(updateMax),
                uploadTotalCount,
                formatNs(uploadTotalNs),
                formatNs(uploadMax),
                setupCount,
                formatNs(setupTotalNs),
                formatNs(setupMax),
                rebuildTotalCount,
                formatNs(rebuildTotalNs),
                formatNs(rebuildMax),
                stealCount,
                formatNs(stealTotalNs),
                formatNs(stealMax),
                topEntrypoints,
                topCallers
        );
    }

    private static String drainTopEntries(Map<String, LongAdder> counts) {
        List<Map.Entry<String, LongAdder>> entries = new ArrayList<>(counts.entrySet());
        counts.clear();

        if (entries.isEmpty()) {
            return "none";
        }

        entries.sort(Comparator.comparingLong((Map.Entry<String, LongAdder> entry) -> entry.getValue().sum()).reversed());

        StringBuilder builder = new StringBuilder();
        int limit = Math.min(entries.size(), 5);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                builder.append(" | ");
            }

            Map.Entry<String, LongAdder> entry = entries.get(i);
            builder.append(entry.getKey()).append(" x").append(entry.getValue().sum());
        }

        return builder.toString();
    }

    private static String findLevelRendererEntrypoint(StackTraceElement[] stack) {
        for (StackTraceElement frame : stack) {
            if (frame.getClassName().equals("net.minecraft.client.renderer.LevelRenderer")) {
                return frame.getMethodName() + ":" + frame.getLineNumber();
            }
        }

        return "unknown";
    }

    private static String findUsefulCaller(StackTraceElement[] stack) {
        for (StackTraceElement frame : stack) {
            String className = frame.getClassName();
            if (className.startsWith("dev.hoyin1600p.vault_render_optimization")) {
                continue;
            }
            if (className.equals(Thread.class.getName())) {
                continue;
            }
            if (className.startsWith("me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer")) {
                continue;
            }
            if (className.startsWith("me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager")) {
                continue;
            }
            if (className.equals("net.minecraft.client.renderer.LevelRenderer")) {
                continue;
            }

            return className + "#" + frame.getMethodName() + ":" + frame.getLineNumber();
        }

        return "unknown";
    }

    private static String formatNs(long ns) {
        return String.format("%.3fms", ns / 1_000_000.0D);
    }

    private static String formatSeconds(long ns) {
        return String.format("%.3fs", ns / 1_000_000_000.0D);
    }
}
