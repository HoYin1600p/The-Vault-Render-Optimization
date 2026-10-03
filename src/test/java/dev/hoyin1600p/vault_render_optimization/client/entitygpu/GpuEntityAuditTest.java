package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class GpuEntityAuditTest {
    private static final String RENDER_DESC = GpuEntityAudit.MODEL_PART_RENDER.descriptor();
    private static final String COMPILE_DESC = GpuEntityAudit.MODEL_PART_COMPILE.descriptor();
    private static final String HANDLER_DESC = "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V";

    private static ClassNode vanilla(String internalName) throws IOException {
        try (InputStream in = GpuEntityAuditTest.class.getClassLoader().getResourceAsStream(internalName + ".class")) {
            assertNotNull(in, internalName);
            ClassNode node = new ClassNode();
            new ClassReader(in.readAllBytes()).accept(node, 0);
            return node;
        }
    }

    private static MethodNode merged(ClassNode node, String name, String desc, String mixin) {
        MethodNode method = new MethodNode(Opcodes.ACC_PRIVATE, name, desc, null, null);
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        AnnotationNode annotation = new AnnotationNode(GpuEntityAudit.MIXIN_MERGED);
        annotation.values = new ArrayList<>(List.of("mixin", mixin, "priority", 1000));
        method.visibleAnnotations = new ArrayList<>(List.of(annotation));
        node.methods.add(method);
        return method;
    }

    private static MethodNode method(ClassNode node, GpuEntityAudit.Target target) {
        return node.methods.stream().filter(target::matches).findFirst().orElseThrow();
    }

    private static void callFromHead(ClassNode node, MethodNode target, MethodNode handler) {
        target.instructions.insert(new MethodInsnNode(Opcodes.INVOKESPECIAL, node.name, handler.name, handler.desc));
    }

    @Test
    void unmodifiedVanillaModelPartPasses() throws IOException {
        assertNull(GpuEntityAudit.skippedBodiesUntouched(vanilla(GpuEntityAudit.MODEL_PART),
                List.of(GpuEntityAudit.MODEL_PART_RENDER, GpuEntityAudit.MODEL_PART_COMPILE)));
        assertNull(GpuEntityAudit.skippedBodiesUntouched(vanilla(GpuEntityAudit.MODEL_PART_CUBE),
                List.of(GpuEntityAudit.CUBE_COMPILE)));
    }

    @Test
    void vroHookAndEmbeddiumCompileOverwriteAreAllowed() throws IOException {
        ClassNode node = vanilla(GpuEntityAudit.MODEL_PART);
        MethodNode render = method(node, GpuEntityAudit.MODEL_PART_RENDER);
        MethodNode handler = merged(node, "handler$vro", HANDLER_DESC,
                "dev.hoyin1600p.vault_render_optimization.mixin.entitygpu.ModelPartGpuMixin");
        callFromHead(node, render, handler);
        MethodNode compile = method(node, GpuEntityAudit.MODEL_PART_COMPILE);
        AnnotationNode overwrite = new AnnotationNode(GpuEntityAudit.MIXIN_MERGED);
        overwrite.values = new ArrayList<>(List.of("mixin",
                "me.jellysquid.mods.sodium.mixin.features.entity.fast_render.MixinModelPart"));
        compile.visibleAnnotations = new ArrayList<>(List.of(overwrite));
        assertNull(GpuEntityAudit.skippedBodiesUntouched(node,
                List.of(GpuEntityAudit.MODEL_PART_RENDER, GpuEntityAudit.MODEL_PART_COMPILE)));
    }

    @Test
    void unknownInjectionIntoRenderOrCompileBlocks() throws IOException {
        ClassNode node = vanilla(GpuEntityAudit.MODEL_PART);
        MethodNode handler = merged(node, "handler$other$render", HANDLER_DESC, "some.other.mod.ModelPartMixin");
        callFromHead(node, method(node, GpuEntityAudit.MODEL_PART_RENDER), handler);
        String blocker = GpuEntityAudit.skippedBodiesUntouched(node,
                List.of(GpuEntityAudit.MODEL_PART_RENDER, GpuEntityAudit.MODEL_PART_COMPILE));
        assertNotNull(blocker);
        assertTrue(blocker.contains("some.other.mod"), blocker);

        ClassNode second = vanilla(GpuEntityAudit.MODEL_PART);
        MethodNode xaero = merged(second, "handler$other", HANDLER_DESC, "another.mod.CompileMixin");
        callFromHead(second, method(second, GpuEntityAudit.MODEL_PART_COMPILE), xaero);
        assertNotNull(GpuEntityAudit.skippedBodiesUntouched(second,
                List.of(GpuEntityAudit.MODEL_PART_RENDER, GpuEntityAudit.MODEL_PART_COMPILE)));
    }

    @Test
    void reviewedCancelOrFallThroughHooksMustRunBeforeVro() throws IOException {
        for (String mod : GpuEntityAudit.PRECEDING_RENDER_HOOKS) {
            // Mixin places later-applied HEAD callbacks after earlier ones: foreign first, then VRO.
            ClassNode before = vanilla(GpuEntityAudit.MODEL_PART);
            MethodNode render = method(before, GpuEntityAudit.MODEL_PART_RENDER);
            callFromHead(before, render, merged(before, "handler$vro", HANDLER_DESC,
                    "dev.hoyin1600p.vault_render_optimization.mixin.entitygpu.ModelPartGpuMixin"));
            callFromHead(before, render, merged(before, "handler$foreign", HANDLER_DESC, mod));
            assertNull(GpuEntityAudit.skippedBodiesUntouched(before, List.of(GpuEntityAudit.MODEL_PART_RENDER)), mod);

            ClassNode after = vanilla(GpuEntityAudit.MODEL_PART);
            MethodNode render2 = method(after, GpuEntityAudit.MODEL_PART_RENDER);
            callFromHead(after, render2, merged(after, "handler$foreign", HANDLER_DESC, mod));
            callFromHead(after, render2, merged(after, "handler$vro", HANDLER_DESC,
                    "dev.hoyin1600p.vault_render_optimization.mixin.entitygpu.ModelPartGpuMixin"));
            String blocker = GpuEntityAudit.skippedBodiesUntouched(after, List.of(GpuEntityAudit.MODEL_PART_RENDER));
            assertNotNull(blocker, mod);
            assertTrue(blocker.contains("after VRO"), blocker);

            // Only render is covered: the same mod in compile still blocks.
            ClassNode compile = vanilla(GpuEntityAudit.MODEL_PART);
            callFromHead(compile, method(compile, GpuEntityAudit.MODEL_PART_COMPILE),
                    merged(compile, "handler$foreign", HANDLER_DESC, mod));
            assertNotNull(GpuEntityAudit.skippedBodiesUntouched(compile, List.of(GpuEntityAudit.MODEL_PART_COMPILE)), mod);
        }
    }

    @Test
    void xaeroIconTraceObserverIsAllowedOnlyInModelPartCompile() throws IOException {
        String xaero = GpuEntityAudit.CONDITIONAL_COMPILE_OBSERVERS.get(0);
        ClassNode node = vanilla(GpuEntityAudit.MODEL_PART);
        callFromHead(node, method(node, GpuEntityAudit.MODEL_PART_COMPILE), merged(node, "handler$xaero", HANDLER_DESC, xaero));
        assertNull(GpuEntityAudit.skippedBodiesUntouched(node,
                List.of(GpuEntityAudit.MODEL_PART_RENDER, GpuEntityAudit.MODEL_PART_COMPILE)));
        assertTrue(GpuEntityAudit.xaeroTraceHookPresent());

        ClassNode render = vanilla(GpuEntityAudit.MODEL_PART);
        callFromHead(render, method(render, GpuEntityAudit.MODEL_PART_RENDER), merged(render, "handler$xaero", HANDLER_DESC, xaero));
        assertNotNull(GpuEntityAudit.skippedBodiesUntouched(render, List.of(GpuEntityAudit.MODEL_PART_RENDER)));

        ClassNode cube = vanilla(GpuEntityAudit.MODEL_PART_CUBE);
        callFromHead(cube, method(cube, GpuEntityAudit.CUBE_COMPILE), merged(cube, "handler$xaero", HANDLER_DESC, xaero));
        assertNotNull(GpuEntityAudit.skippedBodiesUntouched(cube, List.of(GpuEntityAudit.CUBE_COMPILE)));
    }

    @Test
    void foreignOverwriteOfRenderBlocks() throws IOException {
        ClassNode node = vanilla(GpuEntityAudit.MODEL_PART);
        AnnotationNode overwrite = new AnnotationNode(GpuEntityAudit.MIXIN_MERGED);
        overwrite.values = new ArrayList<>(List.of("mixin", "some.other.mod.ModelPartMixin"));
        method(node, GpuEntityAudit.MODEL_PART_RENDER).visibleAnnotations = new ArrayList<>(List.of(overwrite));
        assertNotNull(GpuEntityAudit.skippedBodiesUntouched(node, List.of(GpuEntityAudit.MODEL_PART_RENDER)));
    }

    @Test
    void injectionsElsewhereInModelPartAreFine() throws IOException {
        // The Vault, otyacraftengine and wildbackport also inject translateAndRotate, which the GPU path calls.
        ClassNode node = vanilla(GpuEntityAudit.MODEL_PART);
        MethodNode translate = node.methods.stream()
                .filter(m -> m.name.equals("translateAndRotate") || m.name.equals("m_104299_")).findFirst().orElseThrow();
        callFromHead(node, translate, merged(node, "handler$vault$scale", HANDLER_DESC, "iskallia.vault.mixin.MixinModelPart"));
        assertNull(GpuEntityAudit.skippedBodiesUntouched(node,
                List.of(GpuEntityAudit.MODEL_PART_RENDER, GpuEntityAudit.MODEL_PART_COMPILE)));
    }

    @Test
    void bufferHooksMustAllBePresent() throws IOException {
        String mixin = "dev.hoyin1600p.vault_render_optimization.mixin.entitygpu.BufferBuilderGpuMixin";
        List<GpuEntityAudit.Target> targets = List.of(GpuEntityAudit.BUILDER_POP, GpuEntityAudit.BUILDER_END,
                GpuEntityAudit.BUILDER_DISCARD, GpuEntityAudit.BUILDER_SORT, GpuEntityAudit.BUILDER_BEGIN);
        ClassNode node = vanilla(GpuEntityAudit.BUFFER_BUILDER);
        assertNotNull(GpuEntityAudit.hooksPresent(node, mixin, targets), "no hooks at all");
        for (GpuEntityAudit.Target target : targets) {
            MethodNode handler = merged(node, "vro$" + target.names().get(0), HANDLER_DESC, mixin);
            callFromHead(node, method(node, target), handler);
        }
        assertNull(GpuEntityAudit.hooksPresent(node, mixin, targets));
        // A hook contributed by another mod's mixin does not count as VRO's.
        ClassNode other = vanilla(GpuEntityAudit.BUFFER_BUILDER);
        for (GpuEntityAudit.Target target : targets) {
            callFromHead(other, method(other, target), merged(other, "h$" + target.names().get(0), HANDLER_DESC,
                    "some.other.mod.BufferBuilderMixin"));
        }
        assertNotNull(GpuEntityAudit.hooksPresent(other, mixin, targets));
    }

    @Test
    void uploaderHooksResolveAgainstVanilla() throws IOException {
        String mixin = "dev.hoyin1600p.vault_render_optimization.mixin.entitygpu.BufferUploaderGpuMixin";
        List<GpuEntityAudit.Target> targets = List.of(GpuEntityAudit.UPLOADER_END, GpuEntityAudit.UPLOADER_DRAW);
        ClassNode node = vanilla(GpuEntityAudit.BUFFER_UPLOADER);
        String missing = GpuEntityAudit.hooksPresent(node, mixin, targets);
        assertNotNull(missing);
        assertTrue(missing.contains("does not contain"), "targets must resolve before hooks are checked: " + missing);
    }

    @Test
    void compileDescriptorsMatchVanilla() throws IOException {
        assertTrue(vanilla(GpuEntityAudit.MODEL_PART).methods.stream().anyMatch(GpuEntityAudit.MODEL_PART_COMPILE::matches));
        assertTrue(vanilla(GpuEntityAudit.MODEL_PART_CUBE).methods.stream().anyMatch(GpuEntityAudit.CUBE_COMPILE::matches));
        assertEquals(RENDER_DESC, GpuEntityAudit.MODEL_PART_RENDER.descriptor());
        assertEquals(COMPILE_DESC, GpuEntityAudit.CUBE_COMPILE.descriptor());
    }
}
