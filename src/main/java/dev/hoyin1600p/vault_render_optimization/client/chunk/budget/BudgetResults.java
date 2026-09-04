package dev.hoyin1600p.vault_render_optimization.client.chunk.budget;

import dev.hoyin1600p.vault_render_optimization.client.chunk.sorting.IndexOnlySortResult;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Queue;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;

/** Bounded render-thread ledger. Native queue remains the sole owner of all result payloads. */
public final class BudgetResults {
    private record Seen(long at, long bytes) { }
    private Map<ChunkBuildResult, Seen> seen = new IdentityHashMap<>();
    public record Snapshot(long bytes, int count, long oldestWait, boolean unbuiltTerrain) { }

    public Snapshot inspect(Queue<ChunkBuildResult> queue, long now) {
        Map<ChunkBuildResult, Seen> live = new IdentityHashMap<>();
        long bytes = 0, oldest = now;
        boolean unbuilt = false;
        for (ChunkBuildResult result : queue) {
            if (live.size() == 4096) {
                // Saturated native queue: report overload, bound observation overhead, keep native ownership.
                seen = live;
                return new Snapshot(Long.MAX_VALUE / 4, 4097, Math.max(0, now - oldest), true);
            }
            Seen entry = seen.get(result);
            if (entry == null) entry = new Seen(now, bytes(result));
            live.put(result, entry);
            unbuilt |= isUnbuiltTerrain(result);
            bytes += entry.bytes;
            oldest = Math.min(oldest, entry.at);
        }
        seen = live;
        return new Snapshot(bytes, live.size(), Math.max(0, now - oldest), unbuilt);
    }

    public static boolean isUnbuiltTerrain(ChunkBuildResult result) {
        return result.render != null && !result.render.isBuilt();
    }

    /** If a bounded inspection cannot rule out first-time geometry, prefer the native drain. */
    public static boolean needsNativeDrain(Queue<ChunkBuildResult> queue) {
        int count = 0;
        for (var result : queue) if (++count > 4096 || isUnbuiltTerrain(result)) return true;
        return false;
    }

    public void forget(ChunkBuildResult result) { seen.remove(result); }
    public void clear() { seen.clear(); }

    /** Native payload bytes only; cached heap geometry and driver allocations are not included. */
    public static long bytes(ChunkBuildResult result) {
        long bytes = 0;
        if (result instanceof IndexOnlySortResult sort) {
            for (var output : sort.indices.values()) bytes += output.buffer().getLength();
        } else {
            for (var mesh : result.meshes.values()) {
                var data = mesh.getVertexData();
                bytes += (long) data.vertexBuffer().getLength() + data.indexBuffer().getLength();
            }
        }
        return bytes;
    }
}
