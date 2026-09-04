package dev.hoyin1600p.vault_render_optimization.client.chunk.budget;

import static org.junit.jupiter.api.Assertions.*;
import dev.hoyin1600p.vault_render_optimization.client.chunk.sorting.IndexSortCompatibility;
import java.util.List;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

class BudgetStructureTest {
    @Test void exactAdditionalContractsPassAndUnknownLayoutsFailClosed() throws Exception {
        try (var zip = new ZipFile(System.getProperty("vro.indexSort.rawRenderer"))) {
            assertNull(AdaptiveBudgetCompatibility.blocker(null, path -> {
                try (var stream = zip.getInputStream(zip.getEntry(path))) { return stream.readAllBytes(); }
                catch (Exception failure) { throw new IllegalStateException(failure); }
            }));
        }
        assertNotNull(AdaptiveBudgetCompatibility.blocker(null, path -> new byte[0]));
        assertNotNull(AdaptiveBudgetCompatibility.blocker(null, path -> null));
        assertNotNull(AdaptiveBudgetCompatibility.blocker(null, path -> { throw new IllegalStateException(); }));
        assertEquals("unsupported renderer", AdaptiveBudgetCompatibility.blocker("unsupported renderer", path -> {
            fail("must not inspect unsupported renderer"); return null;
        }));
    }

    private ClassNode raw(String suffix) throws Exception {
        try (var zip = new ZipFile(System.getProperty("vro.indexSort.rawRenderer"));
             var stream = zip.getInputStream(zip.getEntry(IndexSortCompatibility.PREFIX + suffix + ".class"))) {
            var node = new ClassNode();
            new ClassReader(stream).accept(node, 0);
            return node;
        }
    }

    @Test void budgetStoresAndSchedulingTargetsMatchExactNativeContract() throws Exception {
        var manager = raw("render/chunk/RenderSectionManager");
        var submit = manager.methods.stream().filter(m -> m.name.equals("submitRebuildTasks")).findFirst().orElseThrow();
        assertEquals("(Lme/jellysquid/mods/sodium/client/render/chunk/ChunkUpdateType;)Ljava/util/LinkedList;", submit.desc);
        int stores = 0, schedules = 0, decrements = 0;
        for (var instruction : submit.instructions) {
            if (instruction instanceof VarInsnNode var && var.getOpcode() == Opcodes.ISTORE && var.var == 2) stores++;
            if (instruction instanceof IincInsnNode inc && inc.var == 2 && inc.incr == -1) decrements++;
            if (instruction instanceof MethodInsnNode call && call.name.equals("scheduleDeferred")) schedules++;
        }
        assertEquals(2, stores); // initial budget + SORT minimum; mixin guards BOTH
        assertEquals(1, decrements);
        assertEquals(1, schedules);
        assertTrue(manager.methods.stream().anyMatch(m -> m.name.equals("performPendingUploads") && m.desc.equals("()Z")));
        assertTrue(manager.methods.stream().anyMatch(m -> m.name.equals("destroy") && m.desc.equals("()V")));
        var builder = raw("render/chunk/compile/ChunkBuilder");
        assertTrue(builder.fields.stream().anyMatch(f -> f.name.equals("deferredResultQueue") && f.desc.equals("Ljava/util/Queue;")));
        assertTrue(builder.fields.stream().anyMatch(f -> f.name.equals("limitThreads") && f.desc.equals("I")));
        assertTrue(builder.fields.stream().anyMatch(f -> f.name.equals("buildQueue") && f.desc.equals("Ljava/util/Deque;")));
    }

    @Test void nativeCleanupAndFutureSemanticsRemainAvailable() throws Exception {
        var builder = raw("render/chunk/compile/ChunkBuilder");
        var shutdown = builder.methods.stream().filter(m -> m.name.equals("stopWorkers")).findFirst().orElseThrow();
        boolean deletes = false, clears = false;
        for (var insn : shutdown.instructions) if (insn instanceof MethodInsnNode call) {
            deletes |= call.name.equals("delete");
            clears |= call.name.equals("clear");
        }
        assertTrue(deletes && clears);
        var type = raw("render/chunk/ChunkUpdateType");
        assertEquals(List.of("SORT", "IMPORTANT_SORT", "INITIAL_BUILD", "REBUILD", "IMPORTANT_REBUILD"),
                type.fields.stream().filter(f -> (f.access & Opcodes.ACC_ENUM) != 0).map(f -> f.name).toList());
    }
}
