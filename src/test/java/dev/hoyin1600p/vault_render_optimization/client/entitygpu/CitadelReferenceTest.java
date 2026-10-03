package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import static org.junit.jupiter.api.Assertions.*;

import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Vector3f;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.citadel.CitadelMeshCapture;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Random;
import net.minecraft.world.entity.Entity;
import org.junit.jupiter.api.Test;

/**
 * Citadel's own {@code AdvancedModelBox.render} (private {@code doRender}) is the oracle: for a part without
 * children its bytes must equal {@link CitadelMeshCapture} plus {@link ReferenceExpander} for the pose after
 * {@code translateAndRotate}, which is exactly what the GPU hook reserves.
 */
class CitadelReferenceTest {
    private static final AdvancedEntityModel<Entity> MODEL = new AdvancedEntityModel<>() {
        @Override
        public Iterable<AdvancedModelBox> getAllParts() {
            return List.of();
        }

        @Override
        public Iterable<BasicModelPart> parts() {
            return List.of();
        }

        @Override
        public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                              float headPitch) {
        }
    };

    private static AdvancedModelBox randomBox(Random random) {
        MODEL.texWidth = 64 * (1 + random.nextInt(2));
        MODEL.texHeight = 32 * (1 + random.nextInt(3));
        AdvancedModelBox box = new AdvancedModelBox(MODEL, "part");
        int cubes = 1 + random.nextInt(3);
        for (int i = 0; i < cubes; i++) {
            box.setTextureOffset(random.nextInt(64), random.nextInt(32));
            box.addBox(random.nextFloat() * 16 - 8, random.nextFloat() * 16 - 8, random.nextFloat() * 16 - 8,
                    random.nextInt(4) == 0 ? 0 : 1 + random.nextInt(12), 1 + random.nextInt(12), 1 + random.nextInt(12),
                    random.nextInt(3) == 0 ? random.nextFloat() * 0.5F : 0.0F);
        }
        box.setPos(random.nextFloat() * 20 - 10, random.nextFloat() * 20 - 10, random.nextFloat() * 20 - 10);
        box.rotateAngleX = random.nextFloat() * 6.3F - 3.15F;
        box.rotateAngleY = random.nextFloat() * 6.3F - 3.15F;
        box.rotateAngleZ = random.nextInt(3) == 0 ? random.nextFloat() * 6.3F - 3.15F : 0;
        if (random.nextInt(3) == 0) {
            box.setScaleX(random.nextFloat() + 0.3F);
            box.setScaleY(random.nextFloat() + 0.3F);
            box.setScaleZ(random.nextFloat() + 0.3F);
        }
        if (random.nextInt(4) == 0) box.mirror = true;
        return box;
    }

    private static PoseStack randomPose(Random random) {
        PoseStack stack = new PoseStack();
        stack.translate(random.nextDouble() * 64 - 32, random.nextDouble() * 64 - 32, random.nextDouble() * 64 - 32);
        stack.mulPose(Vector3f.YP.rotationDegrees(random.nextFloat() * 360));
        stack.mulPose(Vector3f.XP.rotationDegrees(random.nextFloat() * 360));
        if (random.nextInt(3) == 0) stack.scale(-1.0F, -1.0F, 1.0F);
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
    void referenceMatchesCitadelDoRenderBitForBit() {
        Random random = new Random(1600);
        BufferBuilder builder = new BufferBuilder(1 << 16);
        int compared = 0;
        for (int trial = 0; trial < 800; trial++) {
            AdvancedModelBox box = randomBox(random);
            ModelMesh mesh = CitadelMeshCapture.capture(box);
            assertNotNull(mesh, "trial " + trial);
            PoseStack citadel = randomPose(random);
            PoseStack ours = new PoseStack();
            ours.last().pose().load(citadel.last().pose());
            ours.last().normal().load(citadel.last().normal());
            float r = random.nextFloat(), g = random.nextFloat(), b = random.nextFloat(), a = random.nextFloat();
            int light = random.nextInt(0x00F000F1), overlay = random.nextInt(0x000F000F);

            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
            box.render(citadel, builder, light, overlay, r, g, b, a);
            builder.end();
            ByteBuffer bytes = builder.popNextBuffer().getSecond().order(ByteOrder.LITTLE_ENDIAN);

            ours.pushPose();
            box.translateAndRotate(ours);
            int[] expected = new int[mesh.vertexCount() * EntityVertexPacking.WORDS_PER_VERTEX];
            ReferenceExpander.expand(mesh, store4(ours.last()), store3(ours.last()), EntityVertexPacking.color(r, g, b, a),
                    overlay, light, null, 0, expected, 0);
            ours.popPose();

            assertEquals(expected.length * 4, bytes.remaining(), "trial " + trial);
            for (int w = 0; w < expected.length; w++) {
                int mask = w % EntityVertexPacking.WORDS_PER_VERTEX == 8 ? 0x00FFFFFF : 0xFFFFFFFF;
                assertEquals(expected[w] & mask, bytes.getInt(w * 4) & mask, "trial " + trial + " word " + w);
                compared++;
            }
        }
        assertTrue(compared > 100_000, "compared " + compared);
    }
}
