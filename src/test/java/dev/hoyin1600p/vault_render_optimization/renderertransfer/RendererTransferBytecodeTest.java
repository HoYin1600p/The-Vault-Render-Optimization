package dev.hoyin1600p.vault_render_optimization.renderertransfer;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

class RendererTransferBytecodeTest {
    private static Function<String, byte[]> jar(String path) {
        return name -> {
            try (var zip = new ZipFile(path)) {
                var entry = zip.getEntry(name);
                if (entry == null) return null;
                try (var stream = zip.getInputStream(entry)) { return stream.readAllBytes(); }
            } catch (IOException failure) { throw new UncheckedIOException(failure); }
        };
    }

    @Test
    void everyTransferHasABytecodeContract() {
        assertEquals(EnumSet.allOf(RendererTransferFeature.class),
                EnumSet.copyOf(RendererTransferBytecode.contractedFeatures()));
    }

    @Test
    void compileTargetRendererPassesEveryTransferContract() {
        var resources = jar(System.getProperty("vro.indexSort.rawRenderer"));
        for (var feature : RendererTransferFeature.values()) {
            assertNull(RendererTransferBytecode.blocker(feature, resources), feature.id());
        }
    }

    @Test
    void customRendererPassesEveryTransferContract() {
        String custom = System.getProperty("vro.indexSort.customRenderer", System.getenv("VRO_CUSTOM_EMBEDDIUM_JAR"));
        assumeTrue(custom != null && !custom.isBlank(), "custom Embeddium JAR not supplied");
        var resources = jar(custom);
        for (var feature : RendererTransferFeature.values()) {
            assertNull(RendererTransferBytecode.blocker(feature, resources), feature.id());
        }
    }

    @Test
    void changedOrMissingClassesFailClosed() {
        var stock = jar(System.getProperty("vro.indexSort.rawRenderer"));
        Function<String, byte[]> tampered = name -> {
            byte[] bytes = stock.apply(name);
            if (bytes == null || !name.endsWith("BlockOcclusionCache.class")) return bytes;
            bytes = bytes.clone();
            bytes[bytes.length - 1] ^= 1;
            return bytes;
        };
        assertNotNull(RendererTransferBytecode.blocker(RendererTransferFeature.ADJACENT_BLOCK_OCCLUSION, tampered));
        assertNull(RendererTransferBytecode.blocker(RendererTransferFeature.ASYNC_ARENA_GROWTH, tampered));
        assertNotNull(RendererTransferBytecode.blocker(RendererTransferFeature.CHUNK_REBUILD_DEDUPLICATION, name -> null));
        assertNotNull(RendererTransferBytecode.blocker(RendererTransferFeature.SMOOTH_FLUID_LIGHTING, name -> {
            throw new UncheckedIOException(new IOException("unreadable"));
        }));
    }
}
