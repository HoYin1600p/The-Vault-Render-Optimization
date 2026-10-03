package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Vector3f;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.geckolib.GeckoMeshCapture;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import software.bernie.geckolib3.geo.raw.pojo.Cube;
import software.bernie.geckolib3.geo.raw.pojo.FaceUv;
import software.bernie.geckolib3.geo.raw.pojo.ModelProperties;
import software.bernie.geckolib3.geo.raw.pojo.UvFaces;
import software.bernie.geckolib3.geo.raw.pojo.UvUnion;
import software.bernie.geckolib3.geo.render.built.GeoCube;
import software.bernie.geckolib3.model.provider.GeoModelProvider;
import software.bernie.geckolib3.renderers.geo.IGeoRenderer;
import software.bernie.geckolib3.util.RenderUtils;

/**
 * GeckoLib 3's own {@code IGeoRenderer.renderCube} is the oracle: its bytes must equal
 * {@link GeckoMeshCapture} plus {@link ReferenceExpander} (which the startup self-test holds the shader to)
 * for random cubes, including flat cubes, face UVs with missing faces, mirroring, inflation and poses.
 */
class GeckoLibReferenceTest {
    private static final IGeoRenderer<Object> RENDERER = new IGeoRenderer<>() {
        @Override
        public MultiBufferSource getCurrentRTB() {
            return null;
        }

        @Override
        public GeoModelProvider getGeoModelProvider() {
            return null;
        }

        @Override
        public ResourceLocation getTextureLocation(Object animatable) {
            return null;
        }
    };

    private static double[] vec(Random random, double range) {
        return new double[] {random.nextDouble() * range * 2 - range, random.nextDouble() * range * 2 - range,
                random.nextDouble() * range * 2 - range};
    }

    private static FaceUv face(Random random) {
        if (random.nextInt(5) == 0) return null;
        FaceUv face = new FaceUv();
        face.setUv(new double[] {random.nextInt(64), random.nextInt(64)});
        face.setUvSize(new double[] {random.nextInt(17) - 4, random.nextInt(17) - 4});
        return face;
    }

    private static GeoCube randomCube(Random random) {
        Cube cube = new Cube();
        cube.setOrigin(vec(random, 12));
        double[] size = {1 + random.nextInt(12), 1 + random.nextInt(12), 1 + random.nextInt(12)};
        if (random.nextInt(3) == 0) size[random.nextInt(3)] = 0; // flat planes (wings, hair, cloth)
        cube.setSize(size);
        if (random.nextBoolean()) cube.setPivot(vec(random, 8));
        if (random.nextBoolean()) cube.setRotation(vec(random, 180));
        if (random.nextInt(4) == 0) cube.setInflate(random.nextDouble() * 0.5);
        if (random.nextInt(4) == 0) cube.setMirror(true);
        UvUnion uv = new UvUnion();
        if (random.nextBoolean()) {
            uv.isBoxUV = true;
            uv.boxUVCoords = new double[] {random.nextInt(64), random.nextInt(64)};
        } else {
            UvFaces faces = new UvFaces();
            faces.setNorth(face(random));
            faces.setSouth(face(random));
            faces.setEast(face(random));
            faces.setWest(face(random));
            faces.setUp(face(random));
            faces.setDown(face(random));
            uv.faceUV = faces;
        }
        cube.setUv(uv);
        ModelProperties properties = new ModelProperties();
        properties.setTextureWidth(64.0 * (1 + random.nextInt(2)));
        properties.setTextureHeight(64.0 * (1 + random.nextInt(2)));
        return GeoCube.createFromPojoCube(cube, properties, random.nextInt(4) == 0 ? 0.25 : null, random.nextBoolean());
    }

    private static PoseStack randomPose(Random random) {
        PoseStack stack = new PoseStack();
        stack.translate(random.nextDouble() * 64 - 32, random.nextDouble() * 64 - 32, random.nextDouble() * 64 - 32);
        stack.mulPose(Vector3f.YP.rotationDegrees(random.nextFloat() * 360));
        stack.mulPose(Vector3f.XP.rotationDegrees(random.nextFloat() * 360));
        switch (random.nextInt(5)) {
            case 0 -> stack.scale(-1.0F, -1.0F, 1.0F);
            case 1 -> stack.scale(0.5F, 0.5F, 0.5F);
            case 2 -> stack.scale(random.nextFloat() + 0.2F, random.nextFloat() + 0.2F, random.nextFloat() + 0.2F);
            case 3 -> stack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
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
    void referenceMatchesGeckoLibRenderCubeBitForBit() {
        Random random = new Random(1600);
        BufferBuilder builder = new BufferBuilder(1 << 16);
        int compared = 0, flatCubes = 0, flipsApplied = 0;
        for (int trial = 0; trial < 3000; trial++) {
            GeoCube cube = randomCube(random);
            ModelMesh mesh = GeckoMeshCapture.capture(cube);
            assertNotNull(mesh, "trial " + trial);
            int flips = GeckoMeshCapture.flips(cube);
            if (flips != 0) flatCubes++;
            PoseStack gecko = randomPose(random);
            PoseStack ours = new PoseStack();
            ours.last().pose().load(gecko.last().pose());
            ours.last().normal().load(gecko.last().normal());
            float r = random.nextFloat(), g = random.nextFloat(), b = random.nextFloat(), a = random.nextFloat();
            int light = random.nextInt(0x00F000F1), overlay = random.nextInt(0x000F000F);

            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
            RENDERER.renderCube(cube, gecko, builder, light, overlay, r, g, b, a);
            builder.end();
            ByteBuffer bytes = builder.popNextBuffer().getSecond().order(ByteOrder.LITTLE_ENDIAN);

            // GeckoLibGpuModels runs renderCube's pose steps itself, then reserves the cached mesh.
            RenderUtils.translateToPivotPoint(ours, cube);
            RenderUtils.rotateMatrixAroundCube(ours, cube);
            RenderUtils.translateAwayFromPivotPoint(ours, cube);
            int[] expected = new int[mesh.vertexCount() * EntityVertexPacking.WORDS_PER_VERTEX];
            ReferenceExpander.expand(mesh, store4(ours.last()), store3(ours.last()), EntityVertexPacking.color(r, g, b, a),
                    overlay, light, null, flips, expected, 0);
            int[] unflipped = new int[expected.length];
            ReferenceExpander.expand(mesh, store4(ours.last()), store3(ours.last()), EntityVertexPacking.color(r, g, b, a),
                    overlay, light, null, 0, unflipped, 0);

            assertEquals(expected.length * 4, bytes.remaining(), "trial " + trial);
            for (int w = 0; w < expected.length; w++) {
                int mask = w % EntityVertexPacking.WORDS_PER_VERTEX == 8 ? 0x00FFFFFF : 0xFFFFFFFF;
                assertEquals(expected[w] & mask, bytes.getInt(w * 4) & mask, "trial " + trial + " word " + w);
                if ((expected[w] & mask) != (unflipped[w] & mask)) flipsApplied++;
                compared++;
            }
        }
        assertTrue(compared > 500_000, "compared " + compared);
        assertTrue(flatCubes > 300 && flipsApplied > 100, "flat cubes " + flatCubes + ", flipped normals " + flipsApplied);
    }

    @Test
    void flipsFollowZeroSizeAxes() {
        Random random = new Random(7);
        for (int i = 0; i < 200; i++) {
            GeoCube cube = randomCube(random);
            float x = cube.size.x(), y = cube.size.y(), z = cube.size.z();
            int flips = GeckoMeshCapture.flips(cube);
            assertEquals(y == 0 || z == 0, (flips & InstanceRecord.FLIP_X) != 0);
            assertEquals(x == 0 || z == 0, (flips & InstanceRecord.FLIP_Y) != 0);
            assertEquals(x == 0 || y == 0, (flips & InstanceRecord.FLIP_Z) != 0);
        }
    }
}
