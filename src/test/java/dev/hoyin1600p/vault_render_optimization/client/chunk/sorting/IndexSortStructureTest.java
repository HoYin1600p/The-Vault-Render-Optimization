package dev.hoyin1600p.vault_render_optimization.client.chunk.sorting;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

class IndexSortStructureTest {
    private ClassNode raw(String suffix) throws Exception {
        try (var zip = new ZipFile(System.getProperty("vro.indexSort.rawRenderer"))) {
            var entry = zip.getEntry(IndexSortCompatibility.PREFIX + suffix + ".class");
            try (var stream = zip.getInputStream(entry)) {
                ClassNode node = new ClassNode();
                new ClassReader(stream).accept(node, 0);
                return node;
            }
        }
    }

    @Test
    void nativeUploadCallsExactlyOneBatchAndOneCompletionHook() throws Exception {
        var node = raw("render/chunk/region/RenderRegionManager");
        var method = node.methods.stream().filter(m -> m.name.equals("upload") && m.desc.contains("Ljava/util/Iterator;")).findFirst().orElseThrow();
        int upload = 0, finish = 0;
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                if (call.owner.equals(node.name) && call.name.equals("upload") && call.desc.contains("Ljava/util/List;")) upload++;
                if (call.name.equals("onBuildFinished")) finish++;
            }
        }
        assertEquals(1, upload);
        assertEquals(1, finish);
        assertTrue(node.methods.stream().anyMatch(m -> m.name.equals("upload") && m.desc.contains("Ljava/util/List;") && (m.access & Opcodes.ACC_PRIVATE) != 0));
    }

    @Test
    void graphicsConstructorUsesOnlyMeshPartsAndTaskFieldsMatch() throws Exception {
        var state = raw("render/chunk/ChunkGraphicsState");
        var constructor = state.methods.stream().filter(m -> m.name.equals("<init>")).findFirst().orElseThrow();
        for (var instruction : constructor.instructions) {
            if (instruction instanceof MethodInsnNode call) assertNotEquals("getVertexData", call.name);
        }
        var manager = raw("render/chunk/RenderSectionManager");
        assertTrue(manager.methods.stream().anyMatch(m -> m.name.equals("createSortTask")));
        for (String field : new String[]{"cameraX", "cameraY", "cameraZ"}) {
            assertTrue(manager.fields.stream().anyMatch(f -> f.name.equals(field) && f.desc.equals("F")));
        }
        assertTrue(manager.fields.stream().anyMatch(f -> f.name.equals("currentFrame") && f.desc.equals("I")));
    }

    @Test
    void testRenderLayerTokensMatchNativeEnumAndUploaderNeverAccessesVertexArena() throws Exception {
        var layers = raw("render/chunk/passes/BlockRenderPass");
        var tokens = layers.fields.stream().filter(f -> (f.access & Opcodes.ACC_ENUM) != 0).map(f -> f.name).collect(Collectors.toSet());
        assertEquals(Set.of("SOLID", "CUTOUT", "CUTOUT_MIPPED", "TRANSLUCENT", "TRIPWIRE"), tokens);
        try (var stream = IndexOnlyUploads.class.getResourceAsStream("IndexOnlyUploads.class")) {
            ClassNode adapter = new ClassNode();
            new ClassReader(stream).accept(adapter, 0);
            for (var method : adapter.methods) for (var instruction : method.instructions) {
                if (instruction instanceof FieldInsnNode field) assertNotEquals("vertexBuffers", field.name);
            }
        }
    }
}
