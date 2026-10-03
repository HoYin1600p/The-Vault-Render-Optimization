package dev.hoyin1600p.vault_render_optimization.client.chunk.sorting;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import dev.hoyin1600p.vault_render_optimization.client.chunk.ChunkUpdateBackend;
import dev.hoyin1600p.vault_render_optimization.client.chunk.budget.AdaptiveBudgetCompatibility;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class IndexSortCompatibilityTest {
    private static final String CUSTOM_JAR_SHA = "9B68D17A96314365E72A9A0DE966B6352C9BA6AB4DE5363C670861E5C655D520";
    private static final String CHUNK_BUILDER = IndexSortCompatibility.PREFIX + "render/chunk/compile/ChunkBuilder.class";

    private byte[] resource(String name) {
        return entry(System.getProperty("vro.indexSort.rawRenderer"), name);
    }

    private static byte[] entry(String jar, String name) {
        try (var zip = new java.util.zip.ZipFile(jar)) {
            var entry = zip.getEntry(name);
            if (entry == null) return null;
            try (var stream = zip.getInputStream(entry)) { return stream.readAllBytes(); }
        } catch (IOException failure) { throw new UncheckedIOException(failure); }
    }

    /** Optional: the installed custom 7b0cf676 JAR, supplied by independent validation. */
    private static String customJar() {
        String jar = System.getProperty("vro.indexSort.customRenderer", System.getenv("VRO_CUSTOM_EMBEDDIUM_JAR"));
        assumeTrue(jar != null && !jar.isBlank(), "custom Embeddium 7b0cf676 JAR not supplied");
        return jar;
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    @Test
    void actualCompileTargetMatchesInspectedRendererBytecode() {
        assertNull(IndexSortCompatibility.blocker(ChunkUpdateBackend.EMBEDDIUM, this::resource));
    }

    @Test
    void exactCustomBuildPassesEveryExistingHashContract() throws Exception {
        String jar = customJar();
        assertEquals(CUSTOM_JAR_SHA, sha256(Files.readAllBytes(Path.of(jar))), "not the inspected custom JAR");
        assertEquals("7F0AFD51E4D9A54CBFABF69553579640ADCFD7AC96CF71FCC917719E306B0A11",
                sha256(entry(jar, CHUNK_BUILDER)));
        String shared = IndexSortCompatibility.blocker(ChunkUpdateBackend.EMBEDDIUM, path -> entry(jar, path));
        assertNull(shared);
        assertNull(AdaptiveBudgetCompatibility.blocker(shared, path -> entry(jar, path)));
    }

    @Test
    void customChunkBuilderDoesNotRelaxOtherClassContracts() {
        String jar = customJar();
        byte[] customBuilder = entry(jar, CHUNK_BUILDER);
        // Custom ChunkBuilder alongside the stock classes is accepted; any other altered class is not.
        assertNull(IndexSortCompatibility.blocker(ChunkUpdateBackend.EMBEDDIUM,
                path -> path.equals(CHUNK_BUILDER) ? customBuilder : resource(path)));
        assertNotNull(IndexSortCompatibility.blocker(ChunkUpdateBackend.EMBEDDIUM,
                path -> path.endsWith("RenderSection.class") ? customBuilder : entry(jar, path)));
        assertNotNull(IndexSortCompatibility.blocker(ChunkUpdateBackend.RUBIDIUM, path -> entry(jar, path)));
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
