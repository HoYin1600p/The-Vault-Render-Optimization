package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Vector3f;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

class ReferenceExpanderTest {
    private static ModelPart randomPart(Random random) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        int count = 1 + random.nextInt(4);
        for (int i = 0; i < count; i++) {
            cubes.add(new ModelPart.Cube(random.nextInt(64), random.nextInt(64),
                    random.nextFloat() * 16 - 8, random.nextFloat() * 16 - 8, random.nextFloat() * 16 - 8,
                    1 + random.nextInt(12), 1 + random.nextInt(12), random.nextInt(4) == 0 ? 0 : 1 + random.nextInt(12),
                    random.nextInt(3) == 0 ? random.nextFloat() : 0, random.nextInt(3) == 0 ? random.nextFloat() : 0,
                    random.nextInt(3) == 0 ? random.nextFloat() : 0,
                    random.nextBoolean(), 64, 32 + 32 * random.nextInt(2)));
        }
        ModelPart part = new ModelPart(cubes, Map.of());
        part.x = random.nextFloat() * 20 - 10;
        part.y = random.nextFloat() * 20 - 10;
        part.z = random.nextFloat() * 20 - 10;
        part.xRot = random.nextFloat() * 6.3F - 3.15F;
        part.yRot = random.nextFloat() * 6.3F - 3.15F;
        part.zRot = random.nextInt(3) == 0 ? random.nextFloat() * 6.3F - 3.15F : 0;
        return part;
    }

    private static PoseStack randomPose(Random random) {
        PoseStack stack = new PoseStack();
        stack.translate(random.nextDouble() * 64 - 32, random.nextDouble() * 64 - 32, random.nextDouble() * 64 - 32);
        stack.mulPose(Vector3f.YP.rotationDegrees(random.nextFloat() * 360));
        stack.mulPose(Vector3f.XP.rotationDegrees(random.nextFloat() * 360));
        switch (random.nextInt(5)) {
            case 0 -> stack.scale(-1.0F, -1.0F, 1.0F);                    // LivingEntityRenderer's flip
            case 1 -> stack.scale(0.5F, 0.5F, 0.5F);                      // baby models
            case 2 -> stack.scale(random.nextFloat() + 0.2F, random.nextFloat() + 0.2F, random.nextFloat() + 0.2F);
            case 3 -> stack.scale(-0.9375F, -0.9375F, 0.9375F);
            default -> { }
        }
        return stack;
    }

    private static float[] store4(PoseStack.Pose pose) {
        FloatBuffer buffer = FloatBuffer.allocate(16);
        pose.pose().store(buffer);
        return buffer.array();
    }

    private static float[] store3(PoseStack.Pose pose) {
        FloatBuffer buffer = FloatBuffer.allocate(9);
        pose.normal().store(buffer);
        return buffer.array();
    }

    @Test
    void referenceMatchesVanillaModelPartOutputBitForBit() {
        Random random = new Random(1600);
        BufferBuilder builder = new BufferBuilder(1 << 16);
        int compared = 0;
        for (int trial = 0; trial < 400; trial++) {
            ModelPart part = randomPart(random);
            PoseStack stack = randomPose(random);
            float r = random.nextFloat(), g = random.nextFloat(), b = random.nextFloat(), a = random.nextFloat();
            int light = random.nextInt(0x00F000F1), overlay = random.nextInt(0x000F000F);

            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
            part.render(stack, builder, light, overlay, r, g, b, a);
            builder.end();
            ByteBuffer bytes = builder.popNextBuffer().getSecond().order(ByteOrder.LITTLE_ENDIAN);

            stack.pushPose();
            part.translateAndRotate(stack);
            ModelMesh mesh = ModelMeshCapture.capture(part);
            int[] expected = new int[mesh.vertexCount() * EntityVertexPacking.WORDS_PER_VERTEX];
            ReferenceExpander.expand(mesh, store4(stack.last()), store3(stack.last()),
                    EntityVertexPacking.color(r, g, b, a), overlay, light, expected, 0);
            stack.popPose();

            assertEquals(expected.length * 4, bytes.remaining(), "trial " + trial);
            for (int w = 0; w < expected.length; w++) {
                int actual = bytes.getInt(w * 4);
                // Word 8 carries three normal bytes and an unwritten padding byte.
                int mask = w % EntityVertexPacking.WORDS_PER_VERTEX == 8 ? 0x00FFFFFF : 0xFFFFFFFF;
                assertEquals(expected[w] & mask, actual & mask, "trial " + trial + " word " + w);
                compared++;
            }
        }
        assertTrue(compared > 100_000, "compared " + compared);
    }

    @Test
    void packingFollowsBufferBuilderTruncation() {
        assertEquals(0xFF7F0000, EntityVertexPacking.color(0, 0, 0.5F, 1));
        assertEquals(127, EntityVertexPacking.normalByte(1.5F));
        assertEquals((int) (-1.0F * 127.0F) & 255, EntityVertexPacking.normalByte(-3F));
        assertEquals(0x00F000F0, EntityVertexPacking.shorts(0x00F000F0));
    }

    @Test
    void nonQuadPartsAreNotCaptured() {
        assertSame(ModelMesh.EMPTY, ModelMeshCapture.capture(new ModelPart(List.of(), Map.of())));
    }
}
