package dev.hoyin1600p.vault_render_optimization.client.chunk.sorting;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * On-demand measurement of the heap copies Embeddium keeps for translucent sorting
 * ({@code ChunkGraphicsState.translucencyData}: a full vertex and index copy per translucent pass
 * per section). This is the "measure first" step for a centroid-only representation; nothing is
 * changed and nothing runs per frame. The renderer registers a snapshot source and drops it on
 * destroy; the report runs on the client thread, which owns the section map.
 */
public final class TranslucentSortFootprint {
    /** Bytes per triangle when keeping one float3 centre plus its three int indices. */
    static final int CENTRE_AND_INDICES = 24;
    /** Bytes per triangle when keeping only the float3 centre and regenerating the index pattern. */
    static final int CENTRE_ONLY = 12;
    private static final int INDEX_BYTES_PER_TRIANGLE = 12;

    public record Totals(int sections, int passes, long vertexBytes, long indexBytes) {
        public long retainedBytes() { return vertexBytes + indexBytes; }
        public long triangles() { return indexBytes / INDEX_BYTES_PER_TRIANGLE; }
        public long centreAndIndexBytes() { return triangles() * CENTRE_AND_INDICES; }
        public long centreOnlyBytes() { return triangles() * CENTRE_ONLY; }
    }

    private static volatile WeakReference<Supplier<Totals>> source = new WeakReference<>(null);

    private TranslucentSortFootprint() {
    }

    public static void register(Supplier<Totals> totals) {
        source = new WeakReference<>(totals);
    }

    public static void unregister(Supplier<Totals> totals) {
        if (source.get() == totals) {
            source = new WeakReference<>(null);
        }
    }

    public static String report() {
        Supplier<Totals> supplier = source.get();
        if (supplier == null) {
            return "translucent sort data: no validated Embeddium renderer is active";
        }
        return describe(supplier.get());
    }

    static String describe(Totals totals) {
        long retained = totals.retainedBytes();
        return String.format(Locale.ROOT,
                "translucent sort data retained: %d section(s), %d pass(es), %.2f MiB (vertices %.2f MiB, indices %.2f MiB),"
                        + " %d triangle(s); centre+indices would be %.2f MiB (%s), centre-only %.2f MiB (%s)",
                totals.sections(), totals.passes(), mib(retained), mib(totals.vertexBytes()), mib(totals.indexBytes()),
                totals.triangles(), mib(totals.centreAndIndexBytes()), saving(retained, totals.centreAndIndexBytes()),
                mib(totals.centreOnlyBytes()), saving(retained, totals.centreOnlyBytes()));
    }

    private static double mib(long bytes) {
        return bytes / (1024.0 * 1024.0);
    }

    private static String saving(long retained, long alternative) {
        if (retained <= 0) {
            return "no data";
        }
        return String.format(Locale.ROOT, "-%.0f%%", 100.0 * (retained - alternative) / retained);
    }
}
