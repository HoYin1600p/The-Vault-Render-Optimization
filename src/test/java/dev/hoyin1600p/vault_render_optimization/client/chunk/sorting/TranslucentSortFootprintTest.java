package dev.hoyin1600p.vault_render_optimization.client.chunk.sorting;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TranslucentSortFootprintTest {
    @Test
    void estimatesFollowTheTriangleCount() {
        // 1,000 quads in the compact 20-byte format: 4,000 vertices and 2,000 triangles of int indices.
        var totals = new TranslucentSortFootprint.Totals(3, 4, 4_000L * 20, 2_000L * 12);
        assertEquals(2_000, totals.triangles());
        assertEquals(104_000, totals.retainedBytes());
        assertEquals(48_000, totals.centreAndIndexBytes());
        assertEquals(24_000, totals.centreOnlyBytes());
        String report = TranslucentSortFootprint.describe(totals);
        assertTrue(report.contains("3 section(s)"), report);
        assertTrue(report.contains("-54%"), report);
        assertTrue(report.contains("-77%"), report);
    }

    @Test
    void emptyRendererReportsNoData() {
        String report = TranslucentSortFootprint.describe(new TranslucentSortFootprint.Totals(0, 0, 0, 0));
        assertTrue(report.contains("no data"), report);
    }

    @Test
    void unregisteringOnlyDropsTheMatchingSource() {
        java.util.function.Supplier<TranslucentSortFootprint.Totals> first = () -> new TranslucentSortFootprint.Totals(1, 1, 20, 12);
        java.util.function.Supplier<TranslucentSortFootprint.Totals> second = () -> new TranslucentSortFootprint.Totals(2, 2, 40, 24);
        TranslucentSortFootprint.register(first);
        TranslucentSortFootprint.register(second);
        TranslucentSortFootprint.unregister(first);
        assertTrue(TranslucentSortFootprint.report().contains("2 section(s)"));
        TranslucentSortFootprint.unregister(second);
        assertTrue(TranslucentSortFootprint.report().contains("no validated Embeddium renderer"));
    }
}
