package dev.hoyin1600p.vault_render_optimization.client.chunk.sorting;

import static org.junit.jupiter.api.Assertions.*;
import dev.hoyin1600p.vault_render_optimization.client.chunk.ChunkUpdateBackend;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.Test;

class IndexSortCompatibilityTest {
    private byte[] resource(String name) {
        try (var zip = new java.util.zip.ZipFile(System.getProperty("vro.indexSort.rawRenderer"))) {
            var entry = zip.getEntry(name);
            if (entry == null) return null;
            try (var stream = zip.getInputStream(entry)) { return stream.readAllBytes(); }
        } catch (IOException failure) { throw new UncheckedIOException(failure); }
    }

    @Test
    void actualCompileTargetMatchesInspectedRendererBytecode() {
        assertNull(IndexSortCompatibility.blocker(ChunkUpdateBackend.EMBEDDIUM, this::resource));
    }

    @Test
    void absentChangedAndUnreadableClassesFailClosed() {
        assertNotNull(IndexSortCompatibility.blocker(ChunkUpdateBackend.EMBEDDIUM, path -> null));
        assertNotNull(IndexSortCompatibility.blocker(ChunkUpdateBackend.EMBEDDIUM, path -> new byte[]{1}));
        assertNotNull(IndexSortCompatibility.blocker(ChunkUpdateBackend.EMBEDDIUM, path -> { throw new IllegalStateException(); }));
    }

    @Test
    void vanillaRubidiumAndAmbiguousBackendsDoNotLoadOptionalClasses() {
        for (var backend : new ChunkUpdateBackend[]{ChunkUpdateBackend.VANILLA, ChunkUpdateBackend.RUBIDIUM, ChunkUpdateBackend.BLOCKED}) {
            assertNotNull(IndexSortCompatibility.blocker(backend, path -> { fail("must not inspect renderer"); return null; }));
        }
    }
}
