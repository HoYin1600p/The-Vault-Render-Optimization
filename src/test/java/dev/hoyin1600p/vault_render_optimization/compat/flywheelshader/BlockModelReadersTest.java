package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.jozufozu.flywheel.core.vertex.BlockVertexListUnsafe;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.vertex.*;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.mixin.flw.vertex.MixinBlockModel;
import java.nio.ByteBuffer;
import net.coderbot.iris.vertices.IrisVertexFormats;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

class BlockModelReadersTest {
    @Test void nativeConstructorsCaptureFormatBeforeBothReaderSites() throws Exception {
        var node = new ClassNode();
        try (var stream = getClass().getResourceAsStream("/com/jozufozu/flywheel/core/model/BlockModel.class")) {
            new ClassReader(stream).accept(node, 0);
        }
        int ordinary = 0, shaded = 0;
        for (var method : node.methods) if (method.name.equals("<init>")) {
            boolean captured = false;
            for (var insn : method.instructions) if (insn instanceof MethodInsnNode call) {
                if (call.owner.endsWith("BufferBuilder$DrawState") && call.desc.equals("()Lcom/mojang/blaze3d/vertex/VertexFormat;"))
                    captured = true;
                if (call.owner.equals("com/jozufozu/flywheel/core/vertex/BlockVertex") && call.name.equals("createReader")) {
                    assertTrue(captured, "Input format must be captured before reader creation");
                    if (call.desc.equals("(Ljava/nio/ByteBuffer;I)Lcom/jozufozu/flywheel/core/vertex/BlockVertexListUnsafe;")) ordinary++;
                    else if (call.desc.equals("(Ljava/nio/ByteBuffer;II)Lcom/jozufozu/flywheel/core/vertex/BlockVertexListUnsafe$Shaded;")) shaded++;
                    else fail("Unknown native reader descriptor");
                }
            }
        }
        assertEquals(1, ordinary);
        assertEquals(1, shaded);
    }

    @Test void bothNativeTickTargetsExistWithExpectedStaticness() throws Exception {
        var flywheel = new ClassNode();
        try (var stream = getClass().getResourceAsStream("/com/jozufozu/flywheel/backend/instancing/InstancedRenderDispatcher.class")) {
            new ClassReader(stream).accept(flywheel, 0);
        }
        var tick = flywheel.methods.stream().filter(m -> m.name.equals("tick") && m.desc.equals("(Lnet/minecraftforge/event/TickEvent$ClientTickEvent;)V")).findFirst().orElseThrow();
        assertNotEquals(0, tick.access & org.objectweb.asm.Opcodes.ACC_STATIC);
        try (var jar = new java.util.zip.ZipFile(System.getProperty("vro.flywheel.rawCreate"))) {
            var create = new ClassNode();
            try (var stream = jar.getInputStream(jar.getEntry("com/simibubi/create/content/contraptions/render/FlwContraptionManager.class"))) {
                new ClassReader(stream).accept(create, 0);
            }
            tick = create.methods.stream().filter(m -> m.name.equals("tick") && m.desc.equals("()V")).findFirst().orElseThrow();
            assertEquals(0, tick.access & org.objectweb.asm.Opcodes.ACC_STATIC);
        }
    }

    @Test void pipelineGapDefersAndPipelineRecoveryResumes() {
        assertFalse(ModelBuildReadiness.defer(false, false));
        assertFalse(ModelBuildReadiness.defer(false, true));
        assertTrue(ModelBuildReadiness.defer(true, false)); // DH reload before login tick
        assertFalse(ModelBuildReadiness.defer(true, true));
        assertTrue(ModelBuildReadiness.defer(true, false)); // repeated reload
        assertFalse(ModelBuildReadiness.defer(false, false)); // shaders genuinely disabled
    }

    @Test void extendedBufferRetainsStrideAndShaderAttributesWithoutPipeline() {
        assertEquals(52, IrisVertexFormats.TERRAIN.getVertexSize());
        ByteBuffer data = MemoryUtil.memCalloc(104);
        data.putFloat(0, 1.25f).putFloat(52, 9.5f);
        data.putShort(84, (short) 42).putShort(86, (short) 7);
        data.putFloat(88, 0.25f).putFloat(92, 0.75f);
        data.putInt(96, 0x12345678).putInt(100, 0x10203040);
        var reader = BlockModelReaders.read(IrisVertexFormats.TERRAIN, data, 2);
        try {
            assertEquals(1.25f, reader.getX(0));
            assertEquals(9.5f, reader.getX(1));
            var extended = assertInstanceOf(IrisBlockVertexReader.class, reader);
            assertEquals(42, extended.getEntityX(1));
            assertEquals(7, extended.getEntityY(1));
            assertEquals(0.25f, extended.getMidTexU(1));
            assertEquals(0.75f, extended.getMidTexV(1));
            assertEquals(0x12345678, extended.getTangent(1));
            assertEquals(0x10203040, extended.getMidBlock(1));
            data.putFloat(52, 100); // native model owns a copy, not the producer's buffer
            assertEquals(9.5f, reader.getX(1));
        } finally { reader.delete(); MemoryUtil.memFree(data); }
    }

    @Test void ordinaryBufferAlwaysUsesOrdinaryStride() {
        ByteBuffer data = MemoryUtil.memCalloc(64);
        data.putFloat(32, 19.25f);
        var reader = BlockModelReaders.read(DefaultVertexFormat.BLOCK, data, 2);
        try {
            assertFalse(reader instanceof IrisBlockVertexReader);
            assertEquals(19.25f, reader.getX(1));
        } finally { reader.delete(); MemoryUtil.memFree(data); }
    }

    @Test void shadedReadersPreserveBoundaryForBothFormats() {
        for (var format : new com.mojang.blaze3d.vertex.VertexFormat[]{DefaultVertexFormat.BLOCK, IrisVertexFormats.TERRAIN}) {
            ByteBuffer data = MemoryUtil.memCalloc(2 * format.getVertexSize());
            data.putFloat(format.getVertexSize(), 3.5f);
            var reader = BlockModelReaders.read(format, data, 2, 1);
            try {
                assertTrue(reader.isShaded(0));
                assertFalse(reader.isShaded(1));
                assertEquals(3.5f, reader.getX(1));
            } finally { reader.delete(); MemoryUtil.memFree(data); }
        }
    }

    @Test void unsupportedAndTruncatedInputIsNotSilentlyReinterpreted() {
        var data = ByteBuffer.allocateDirect(52);
        assertSame(DefaultVertexFormat.POSITION, BlockModelReaders.acceptedFormat(DefaultVertexFormat.POSITION));
        assertThrows(IllegalArgumentException.class, () -> BlockModelReaders.read(DefaultVertexFormat.POSITION, data, 1));
        assertThrows(IllegalArgumentException.class, () -> BlockModelReaders.read(IrisVertexFormats.TERRAIN, data, 2));
        assertThrows(IllegalArgumentException.class, () -> BlockModelReaders.read(DefaultVertexFormat.BLOCK, data, -1));
        assertThrows(IllegalArgumentException.class, () -> BlockModelReaders.read(DefaultVertexFormat.BLOCK, data, 1, 2));
    }

    @Test void actualMixinCapturesFormatAndSelectsMatchingReader() throws Exception {
        var mixin = new MixinBlockModel();
        var state = mock(BufferBuilder.DrawState.class);
        when(state.format()).thenReturn(IrisVertexFormats.TERRAIN);
        var capture = MixinBlockModel.class.getDeclaredMethod("irisFlw$ReturnIrisFormat", BufferBuilder.DrawState.class);
        capture.setAccessible(true);
        assertSame(DefaultVertexFormat.BLOCK, capture.invoke(mixin, state));
        var read = MixinBlockModel.class.getDeclaredMethod("vro$readActualShadedFormat",
                com.jozufozu.flywheel.core.vertex.BlockVertex.class, ByteBuffer.class, int.class, int.class);
        read.setAccessible(true);
        ByteBuffer data = MemoryUtil.memCalloc(104);
        data.putFloat(52, 4);
        var reader = (BlockVertexListUnsafe.Shaded) read.invoke(mixin, null, data, 2, 1);
        try {
            assertInstanceOf(IrisBlockVertexReader.class, reader);
            assertEquals(4f, reader.getX(1));
        } finally { reader.delete(); MemoryUtil.memFree(data); }
    }

    @Test void bothExtendedReaderAllocationsHaveCleanup() throws Exception {
        for (var type : new Class<?>[]{IrisBlockVertexListUnsafe.class, IrisBlockVertexListUnsafe.Shaded.class}) {
            try (var stream = type.getResourceAsStream("/" + type.getName().replace('.', '/') + ".class")) {
                var node = new ClassNode();
                new ClassReader(stream).accept(node, 0);
                var method = node.methods.stream().filter(m -> m.name.equals("delete")).findFirst().orElseThrow();
                boolean delegate = false, parent = false;
                for (var insn : method.instructions) if (insn instanceof MethodInsnNode call && call.name.equals("delete")) {
                    delegate |= call.owner.endsWith("IrisBlockVertexReaderImpl");
                    parent |= call.owner.contains("BlockVertexListUnsafe");
                }
                assertTrue(delegate && parent); // delegate and inherited constructor each own a native copy
            }
        }
    }
}
