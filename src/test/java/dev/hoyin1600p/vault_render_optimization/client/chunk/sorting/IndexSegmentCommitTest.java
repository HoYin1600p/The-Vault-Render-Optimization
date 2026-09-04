package dev.hoyin1600p.vault_render_optimization.client.chunk.sorting;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class IndexSegmentCommitTest {
    @Test
    void retainsVerticesAndReleasesOnlyOldIndicesAfterInstall() {
        List<String> events = new ArrayList<>();
        IndexSegmentCommit.replace("vertices", "old", () -> { events.add("upload"); return "new"; },
                (v, i) -> { assertEquals("vertices", v); assertEquals("new", i); events.add("install"); },
                i -> events.add("free " + i));
        assertEquals(List.of("upload", "install", "free old"), events);
    }

    @Test
    void failedInstallFreesNewIndicesButPreservesBothOriginalSegments() {
        List<String> freed = new ArrayList<>();
        assertThrows(IllegalStateException.class, () -> IndexSegmentCommit.replace("vertices", "old", () -> "new",
                (v, i) -> { throw new IllegalStateException("injected"); }, freed::add));
        assertEquals(List.of("new"), freed);
    }

    @Test
    void failedUploadNeverInstallsOrFreesExistingGeometry() {
        assertThrows(IllegalStateException.class, () -> IndexSegmentCommit.replace("vertices", "old",
                () -> { throw new IllegalStateException("injected"); },
                (v, i) -> fail("unexpected installation"), i -> fail("unexpected free")));
    }
}
