package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Vector3f;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

class GpuEntityCoreTest {
    private static GpuEntityCapabilities.Snapshot snapshot(boolean gl43, boolean arb, String glsl, String renderer) {
        return new GpuEntityCapabilities.Snapshot(gl43, arb, true, gl43 ? "4.6.0" : "3.2.0", renderer, "vendor", glsl,
                16, 16, 1L << 30, 65535, 1024);
    }

    @Test
    void glslVersionParsing() {
        assertEquals(460, GpuEntityCapabilities.glslVersion("4.60 NVIDIA"));
        assertEquals(150, GpuEntityCapabilities.glslVersion("1.50 NVIDIA via Cg compiler"));
        assertEquals(430, GpuEntityCapabilities.glslVersion("4.30 - Build 31.0.101.2111"));
        assertEquals(450, GpuEntityCapabilities.glslVersion("4.5"));
        assertEquals(0, GpuEntityCapabilities.glslVersion("garbage"));
        assertEquals(0, GpuEntityCapabilities.glslVersion(null));
    }

    @Test
    void routeAndBlockerDecisions() {
        // NVIDIA inside Minecraft's 3.2 core context: extensions only.
        assertEquals(GpuEntityCapabilities.Route.ARB_EXTENSIONS, GpuEntityCapabilities.route(
                snapshot(false, true, "1.50 NVIDIA via Cg compiler", "NVIDIA GeForce RTX 5080/PCIe/SSE2")));
        assertEquals(GpuEntityCapabilities.Route.CORE_43, GpuEntityCapabilities.route(
                snapshot(true, true, "4.60", "AMD Radeon RX 7900")));
        assertNull(GpuEntityCapabilities.blocker(snapshot(true, false, "4.60", "AMD Radeon RX 7900")));
        // macOS-like 4.1 without the extensions.
        assertNotNull(GpuEntityCapabilities.blocker(snapshot(false, false, "4.10", "Apple M1")));
        // Software renderers are refused even though they could run the shader.
        assertNotNull(GpuEntityCapabilities.blocker(snapshot(true, true, "4.50", "llvmpipe (LLVM 15.0.7, 256 bits)")));
        GpuEntityCapabilities.Snapshot noFunctions = new GpuEntityCapabilities.Snapshot(true, true, false, "4.6", "x",
                "v", "4.60", 16, 16, 1L << 30, 65535, 1024);
        assertNotNull(GpuEntityCapabilities.blocker(noFunctions));
        GpuEntityCapabilities.Snapshot smallLimits = new GpuEntityCapabilities.Snapshot(true, true, true, "4.6", "x",
                "v", "4.60", 2, 16, 1L << 30, 65535, 1024);
        assertNotNull(GpuEntityCapabilities.blocker(smallLimits));
    }

    @Test
    void itemShaderLayoutAndBatchKinds() throws IOException {
        String source;
        try (InputStream in = GpuEntityBackend.class.getResourceAsStream(GpuEntityBackend.SHADER)) {
            assertNotNull(in);
            source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertEquals(16, InstanceRecord.FLAG_ITEM_EMBEDDIUM);
        assertTrue(source.contains("const uint ITEM_WORDS = " + ItemMesh.WORDS_PER_VERTEX + "u;"));
        assertTrue(source.contains("const uint FLAG_ITEM_EMBEDDIUM = " + InstanceRecord.FLAG_ITEM_EMBEDDIUM + "u;"));
        assertTrue(source.contains("meshWords"));
        assertTrue(source.contains("#ifdef ITEM_QUADS"));

        // The CPU expectation of the item cases is self-consistent and a batch of items keeps its kind.
        for (boolean iris : new boolean[] {false, true}) {
            GpuEntitySelfTest.ItemCase test = GpuEntitySelfTest.buildItems(9, 40, iris);
            assertTrue(test.count() >= 40);
            HoleBatch batch = GpuItemTestSupport.batch(test, null, iris);
            assertEquals(HoleBatch.Kind.ITEMS, batch.kind());
            assertTrue(batch.items());
            assertFalse(batch.canAddModel());
            ByteBuffer out = GpuItemTestSupport.sentinelBuffer(test);
            try {
                batch.fillOnCpu(out);
                assertNull(GpuItemTestSupport.compare(test, out, "CPU fill"));
            } finally {
                org.lwjgl.system.MemoryUtil.memFree(out);
            }
            batch.reset();
            assertEquals(HoleBatch.Kind.MODELS, batch.kind());
            assertFalse(batch.items());
        }
    }

    @Test
    void shaderLayoutMatchesJavaConstants() throws IOException {
        String source;
        try (InputStream in = GpuEntityBackend.class.getResourceAsStream(GpuEntityBackend.SHADER)) {
            assertNotNull(in);
            source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(source.lines().noneMatch(line -> line.startsWith("#version") || line.startsWith("#extension")),
                "the backend prepends the header");
        assertTrue(source.contains("const uint EXT_VERTEX_WORDS = " + IrisEntityExtension.WORDS_PER_VERTEX + "u;"));
        assertTrue(source.contains("uint ids = instances[r + " + InstanceRecord.IRIS_IDS + "u];"));
        assertTrue(source.contains("uint item = instances[r + " + InstanceRecord.IRIS_ITEM + "u] & 0xFFFFu;"));
        assertTrue(source.contains("const uint RECORD_WORDS = " + InstanceRecord.WORDS + "u;"));
        assertTrue(source.contains("const uint MESH_FLOATS = " + ModelMesh.FLOATS_PER_VERTEX + "u;"));
        assertTrue(source.contains("const uint VERTEX_WORDS = " + EntityVertexPacking.WORDS_PER_VERTEX + "u;"));
        assertTrue(source.contains("instances[r + " + InstanceRecord.MESH_FIRST + "u]"));
        assertTrue(source.contains("instances[r + " + InstanceRecord.VERTEX_COUNT + "u]"));
        assertTrue(source.contains("instances[r + " + InstanceRecord.OUTPUT + "u]"));
        assertTrue(source.contains("instances[r + " + InstanceRecord.COLOR + "u]"));
        assertTrue(source.contains("instances[r + " + InstanceRecord.OVERLAY + "u]"));
        assertTrue(source.contains("instances[r + " + InstanceRecord.LIGHT + "u]"));
        assertTrue(source.contains("layout(local_size_x = " + GpuEntityBackend.WORK_GROUP_SIZE + ") in;"));
        // The Oculus program's only approximation is javaRsqrt's starting candidate, which correct() then
        // moves to the correctly rounded value with exact integer comparisons.
        String code = source.replaceAll("//.*", "")
                .replace("correct(scaledBits(inversesqrt(float(m2)), -(e2 / 2)), 2, m, e, 0u, 0)", "");
        for (String forbidden : List.of("fma", "normalize", "packSnorm", "packUnorm", "inversesqrt")) {
            assertFalse(code.contains(forbidden), forbidden);
        }
        for (String axis : List.of("px", "py", "pz", "tx", "ty", "tz")) {
            assertTrue(source.contains("precise float " + axis + " = "), axis);
        }
    }

    @Test
    void selfTestCaseComparesEveryWord() {
        GpuEntitySelfTest.Case test = GpuEntitySelfTest.build(3, 40);
        assertNull(GpuEntitySelfTest.compare(test, IntBuffer.wrap(test.expected().clone())));
        int[] wrong = test.expected().clone();
        int hole = 0;
        while (!test.written()[hole]) hole++;
        wrong[hole] ^= 1;
        assertNotNull(GpuEntitySelfTest.compare(test, IntBuffer.wrap(wrong)));
        int[] gap = test.expected().clone();
        gap[0] = 0;
        assertTrue(GpuEntitySelfTest.compare(test, IntBuffer.wrap(gap)).startsWith("gap word"));
    }

    /**
     * The CPU fallback: records made the way the builder mixin makes them (absolute offsets in the
     * builder, a non-zero popped base) must reproduce vanilla's bytes for the same parts.
     */
    @Test
    void cpuFillReproducesVanillaBytes() {
        Random random = new Random(77);
        BufferBuilder builder = new BufferBuilder(1 << 16);
        // A first draw state so the one under test starts at a non-zero offset, as in a real builder.
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
        builder.vertex(0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0);
        builder.vertex(0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0);
        builder.vertex(0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0);
        builder.vertex(0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0);
        builder.end();
        int base = 4 * 36;

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
        HoleBatch batch = new HoleBatch();
        int offset = base;
        for (int i = 0; i < 60; i++) {
            List<ModelPart.Cube> cubes = new ArrayList<>();
            cubes.add(new ModelPart.Cube(random.nextInt(64), random.nextInt(64), -4, -4, -4, 8, 8, 8,
                    random.nextFloat(), 0, 0, random.nextBoolean(), 64, 64));
            ModelPart part = new ModelPart(cubes, Map.of());
            part.xRot = random.nextFloat() * 3;
            part.y = random.nextFloat() * 10;
            PoseStack stack = new PoseStack();
            stack.translate(random.nextFloat() * 10, 2, -3);
            stack.mulPose(Vector3f.YP.rotationDegrees(random.nextFloat() * 360));
            if (random.nextBoolean()) stack.scale(-1, -1, 1);
            float r = random.nextFloat(), g = random.nextFloat(), b = random.nextFloat(), a = random.nextFloat();
            int light = random.nextInt(0x00F000F1), overlay = random.nextInt(0x000F000F);
            part.render(stack, builder, light, overlay, r, g, b, a);

            stack.pushPose();
            part.translateAndRotate(stack);
            FloatBuffer pose = FloatBuffer.allocate(16);
            FloatBuffer normal = FloatBuffer.allocate(9);
            stack.last().pose().store(pose);
            stack.last().normal().store(normal);
            ModelMesh mesh = ModelMeshCapture.capture(part);
            batch.add(mesh, 0, pose.array(), normal.array(), offset, EntityVertexPacking.color(r, g, b, a), overlay, light);
            offset += mesh.vertexCount() * 36;
        }
        builder.end();
        builder.popNextBuffer();
        Pair<BufferBuilder.DrawState, ByteBuffer> popped = builder.popNextBuffer();
        ByteBuffer vanilla = popped.getSecond();
        assertEquals(offset - base, popped.getFirst().vertexCount() * 36);

        ByteBuffer filled = ByteBuffer.allocateDirect(vanilla.remaining()).order(ByteOrder.nativeOrder());
        for (int i = 0; i < filled.capacity(); i++) filled.put(i, (byte) 0x6B);
        batch.setBase(base);
        batch.fillOnCpu(filled);
        for (int i = 0; i < filled.capacity(); i++) {
            if (i % 36 == 35) continue; // padding byte: not an attribute, never written by vanilla
            assertEquals(vanilla.get(i), filled.get(i), "byte " + i);
        }
    }
}
