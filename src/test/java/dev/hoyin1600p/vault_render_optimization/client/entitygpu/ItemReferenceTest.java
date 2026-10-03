package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.Random;
import net.coderbot.iris.vertices.NormalHelper;
import net.coderbot.iris.vertices.QuadView;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

/**
 * {@link ItemReference} against the real Forge {@code putBulkData} through a real {@code BufferBuilder}, against an
 * independent transcription of Embeddium's item arithmetic, and (Iris) against Oculus' own {@code NormalHelper}.
 */
class ItemReferenceTest {
    private static final Direction[] DIRECTIONS = Direction.values();

    // ---------------------------------------------------------------- generators

    private static int floatBits(Random random, float lo, float hi) {
        return Float.floatToRawIntBits(lo + random.nextFloat() * (hi - lo));
    }

    /** Baked normal bytes: 0, per-vertex random, FaceBakery-style (face direction), or partly zero. */
    private static int[] bakedNormals(Random random, Direction direction) {
        int[] normals = new int[4];
        int style = random.nextInt(5);
        int face = ((direction.getStepX() * 127) & 255) | (((direction.getStepY() * 127) & 255) << 8)
                | (((direction.getStepZ() * 127) & 255) << 16);
        for (int v = 0; v < 4; v++) {
            int pad = random.nextInt(3) == 0 ? random.nextInt(256) << 24 : 0;
            switch (style) {
                case 0 -> normals[v] = pad;
                case 1 -> normals[v] = random.nextInt();
                case 2 -> normals[v] = face | pad;
                case 3 -> normals[v] = (v == 0 || random.nextBoolean() ? 0 : random.nextInt() & 0xFFFFFF) | pad;
                default -> {
                    int wobble = (random.nextInt(9) - 4) & 255;
                    normals[v] = (face ^ (random.nextBoolean() ? wobble : 0)) | pad;
                }
            }
        }
        return normals;
    }

    private static BakedQuad randomQuad(Random random, int tintIndex) {
        Direction direction = DIRECTIONS[random.nextInt(6)];
        int[] data = new int[32];
        int[] normals = bakedNormals(random, direction);
        boolean sameColor = random.nextBoolean();
        int quadColor = randomColor(random);
        boolean bakedLight = random.nextInt(3) == 0;
        int quadLight = random.nextInt(4) == 0 ? random.nextInt() : LightTexture.pack(random.nextInt(16), random.nextInt(16));
        for (int v = 0; v < 4; v++) {
            int d = v * 8;
            data[d] = floatBits(random, -2.0F, 18.0F) ;
            data[d + 1] = floatBits(random, -2.0F, 18.0F);
            data[d + 2] = floatBits(random, -2.0F, 18.0F);
            data[d + 3] = sameColor ? quadColor : randomColor(random);
            data[d + 4] = floatBits(random, 0.0F, 1.0F);
            data[d + 5] = floatBits(random, 0.0F, 1.0F);
            data[d + 6] = bakedLight ? (random.nextBoolean() ? quadLight : random.nextInt()) : 0;
            data[d + 7] = normals[v];
        }
        return new BakedQuad(data, tintIndex, direction, null, random.nextBoolean());
    }

    private static int randomColor(Random random) {
        return switch (random.nextInt(4)) {
            case 0 -> 0xFFFFFFFF;
            case 1 -> 0xFF000000 | random.nextInt(0x1000000);
            case 2 -> random.nextInt(4) == 0 ? 0 : (0xFF000000 | (random.nextInt(4) * 85) * 0x010101);
            default -> random.nextInt();
        };
    }

    private static List<BakedQuad> randomQuads(Random random, int[] tintIndices) {
        int count = 1 + random.nextInt(6);
        List<BakedQuad> quads = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            quads.add(randomQuad(random, tintIndices[random.nextInt(tintIndices.length)]));
        }
        return quads;
    }

    private static PoseStack randomPose(Random random) {
        PoseStack stack = new PoseStack();
        stack.translate(random.nextDouble() * 64 - 32, random.nextDouble() * 64 - 32, random.nextDouble() * 64 - 32);
        if (random.nextInt(4) != 0) stack.mulPose(Vector3f.YP.rotationDegrees(random.nextFloat() * 360));
        if (random.nextInt(4) != 0) stack.mulPose(Vector3f.XP.rotationDegrees(random.nextFloat() * 360));
        if (random.nextInt(4) == 0) stack.mulPose(Vector3f.ZP.rotationDegrees(random.nextFloat() * 360));
        switch (random.nextInt(7)) {
            case 0 -> stack.scale(-1.0F, -1.0F, 1.0F);
            case 1 -> stack.scale(0.5F, 0.5F, 0.5F);
            case 2 -> stack.scale(random.nextFloat() + 0.2F, random.nextFloat() + 0.2F, random.nextFloat() + 0.2F);
            case 3 -> stack.scale(0.5F - random.nextFloat(), 0.5F - random.nextFloat(), 0.5F - random.nextFloat());
            case 4 -> stack.scale(-0.375F, 0.375F, -0.375F);
            case 5 -> stack.scale(16.0F, 16.0F, 16.0F);
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

    private static int randomLight(Random random) {
        return switch (random.nextInt(4)) {
            case 0 -> 0xF000F0;
            case 1 -> LightTexture.pack(random.nextInt(16), random.nextInt(16));
            case 2 -> 0;
            default -> random.nextInt(0x00F000F1);
        };
    }

    private static int randomOverlay(Random random) {
        return random.nextBoolean() ? 0x000A0000 | random.nextInt(16) : random.nextInt();
    }

    /** {@code ItemColors} result (ARGB, alpha noise) or -1 when untinted. */
    private static int randomTintArgb(Random random) {
        return random.nextInt(3) == 0 ? -1 : random.nextInt();
    }

    private static int abgr(int argb) {
        return (argb & 0xFF) << 16 | (argb & 0xFF00) | (argb >> 16 & 0xFF) | 0xFF000000;
    }

    // ---------------------------------------------------------------- FORGE writer

    @Test
    void forgeWriterMatchesRealBufferBuilder() {
        Random random = new Random(1600);
        BufferBuilder builder = new BufferBuilder(1 << 16);
        int compared = 0;
        for (int trial = 0; trial < 6000; trial++) {
            int[] tintArgb = {randomTintArgb(random), randomTintArgb(random), randomTintArgb(random)};
            List<BakedQuad> quads = randomQuads(random, new int[] {-1, 0, 1, 2});
            PoseStack stack = randomPose(random);
            int light = randomLight(random), overlay = randomOverlay(random);

            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
            for (BakedQuad quad : quads) {
                int i = quad.isTinted() ? tintArgb[quad.getTintIndex()] : -1;
                float r = (float) (i >> 16 & 255) / 255.0F;
                float g = (float) (i >> 8 & 255) / 255.0F;
                float b = (float) (i & 255) / 255.0F;
                builder.putBulkData(stack.last(), quad, r, g, b, light, overlay, true);
            }
            builder.end();
            ByteBuffer bytes = builder.popNextBuffer().getSecond().order(ByteOrder.LITTLE_ENDIAN);

            ItemMesh mesh = ItemMeshCapture.capture(quads, ItemWriter.FORGE);
            assertNotNull(mesh);
            int[] expected = new int[mesh.vertexCount() * EntityVertexPacking.WORDS_PER_VERTEX];
            float[] pose = store4(stack.last()), normal = store3(stack.last());
            for (int q = 0; q < quads.size(); q++) {
                int tint = mesh.quadTint()[q] < 0 ? -1 : abgr(tintArgb[mesh.quadTint()[q]]);
                ItemReference.expand(mesh, q * 4, 4, pose, normal, tint, overlay, light, false, expected,
                        q * 4 * EntityVertexPacking.WORDS_PER_VERTEX);
            }
            compareToBuffer(bytes, expected, "forge trial " + trial);
            compared += expected.length;
        }
        assertTrue(compared > 100_000, "compared " + compared);
    }

    private static void compareToBuffer(ByteBuffer bytes, int[] expected, String message) {
        assertEquals(expected.length * 4, bytes.remaining(), message);
        for (int w = 0; w < expected.length; w++) {
            int actual = bytes.getInt(w * 4);
            // Word 8 carries three normal bytes and a padding byte that vanilla never writes.
            int mask = w % EntityVertexPacking.WORDS_PER_VERTEX == 8 ? 0x00FFFFFF : 0xFFFFFFFF;
            assertEquals(expected[w] & mask, actual & mask, message + " word " + w);
        }
    }

    // ---------------------------------------------------------------- EMBEDDIUM writer (transcription)

    private static final float NORM3B = 0.007874016F;

    /** Embeddium {@code ColorABGR.mix}/{@code mixARGBColors}. */
    private static int embeddiumMix(int a, int b) {
        if (a == -1) return b;
        if (b == -1) return a;
        int result = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            float x = (float) (a >>> shift & 255) / 255.0F;
            float y = (float) (b >>> shift & 255) / 255.0F;
            result |= ((int) (x * y * 255.0F) & 255) << shift;
        }
        return result;
    }

    private static int embeddiumLight(int baked, int light) {
        if (baked == 0) return light;
        int sl = Math.max(baked >> 16 & 255, light >> 16 & 255);
        int bl = Math.max(baked & 255, light & 255);
        return sl << 16 | bl;
    }

    private static int embeddiumFacing(Direction direction, int baked) {
        if ((baked & 0xFFFFFF) == 0) {
            return ((int) (direction.getStepX() * 127.0F) & 255) | (((int) (direction.getStepY() * 127.0F) & 255) << 8)
                    | (((int) (direction.getStepZ() * 127.0F) & 255) << 16);
        }
        return baked;
    }

    private static int embeddiumTransformNormal(int packed, float[] n) {
        float x = (byte) (packed & 255) * NORM3B;
        float y = (byte) (packed >> 8 & 255) * NORM3B;
        float z = (byte) (packed >> 16 & 255) * NORM3B;
        float x2 = n[0] * x + n[3] * y + n[6] * z;
        float y2 = n[1] * x + n[4] * y + n[7] * z;
        float z2 = n[2] * x + n[5] * y + n[8] * z;
        return packSigned(x2) | packSigned(y2) << 8 | packSigned(z2) << 16;
    }

    private static int packSigned(float v) {
        return (int) (Math.max(-1.0F, Math.min(1.0F, v)) * 127.0F) & 255;
    }

    private static int[] embeddiumExpected(List<BakedQuad> quads, int[] tintAbgrByQuad, float[] m, float[] n, int light,
                                           int overlay) {
        int[] out = new int[quads.size() * 4 * 9];
        int o = 0;
        for (int q = 0; q < quads.size(); q++) {
            BakedQuad quad = quads.get(q);
            int[] d = quad.getVertices();
            for (int v = 0; v < 4; v++) {
                int b = v * 8;
                float x = Float.intBitsToFloat(d[b]), y = Float.intBitsToFloat(d[b + 1]), z = Float.intBitsToFloat(d[b + 2]);
                out[o++] = Float.floatToRawIntBits(((m[0] * x + m[4] * y) + m[8] * z) + m[12] * 1.0F);
                out[o++] = Float.floatToRawIntBits(((m[1] * x + m[5] * y) + m[9] * z) + m[13] * 1.0F);
                out[o++] = Float.floatToRawIntBits(((m[2] * x + m[6] * y) + m[10] * z) + m[14] * 1.0F);
                out[o++] = embeddiumMix(d[b + 3], tintAbgrByQuad[q]);
                out[o++] = d[b + 4];
                out[o++] = d[b + 5];
                out[o++] = overlay;
                out[o++] = embeddiumLight(d[b + 6], light);
                out[o++] = embeddiumTransformNormal(embeddiumFacing(quad.getDirection(), d[b + 7]), n);
            }
        }
        return out;
    }

    private static int[] expandAll(ItemMesh mesh, int[] tintAbgrByQuad, float[] pose, float[] normal, int overlay,
                                   int light, boolean embeddium) {
        int[] out = new int[mesh.vertexCount() * 9];
        for (int q = 0; q < mesh.vertexCount() / 4; q++) {
            ItemReference.expand(mesh, q * 4, 4, pose, normal, tintAbgrByQuad[q], overlay, light, embeddium, out, q * 36);
        }
        return out;
    }

    private static int[] tintWords(ItemMesh mesh, int[] tintArgb) {
        int[] tints = new int[mesh.quadTint().length];
        for (int q = 0; q < tints.length; q++) {
            tints[q] = mesh.quadTint()[q] < 0 ? -1 : abgr(tintArgb[mesh.quadTint()[q]]);
        }
        return tints;
    }

    @Test
    void embeddiumWriterMatchesTranscription() {
        Random random = new Random(3200);
        for (int trial = 0; trial < 20_000; trial++) {
            int[] tintArgb = {randomTintArgb(random), randomTintArgb(random), randomTintArgb(random)};
            List<BakedQuad> quads = randomQuads(random, new int[] {-1, 0, 1, 2});
            PoseStack stack = randomPose(random);
            int light = randomLight(random), overlay = randomOverlay(random);
            float[] pose = store4(stack.last()), normal = store3(stack.last());
            ItemMesh mesh = ItemMeshCapture.capture(quads, ItemWriter.EMBEDDIUM);
            assertNotNull(mesh);
            int[] tints = tintWords(mesh, tintArgb);
            int[] actual = expandAll(mesh, tints, pose, normal, overlay, light, true);
            int[] expected = embeddiumExpected(quads, tints, pose, normal, light, overlay);
            assertArrayEquals(expected, actual, "embeddium trial " + trial);
        }
    }

    @Test
    void writersAgreeExceptNonAxisBakedNormalsForVanillaLight() {
        Random random = new Random(4800);
        int normalDiffs = 0;
        for (int trial = 0; trial < 6000; trial++) {
            int[] tintArgb = {randomTintArgb(random), randomTintArgb(random), randomTintArgb(random)};
            List<BakedQuad> quads = randomQuads(random, new int[] {-1, 0, 1, 2});
            PoseStack stack = randomPose(random);
            // Vanilla-range lightmap values and baked light 0 or vanilla-range too.
            int light = LightTexture.pack(random.nextInt(16), random.nextInt(16));
            List<BakedQuad> lit = new ArrayList<>();
            for (BakedQuad quad : quads) {
                int[] data = quad.getVertices().clone();
                for (int v = 0; v < 4; v++) data[v * 8 + 6] = random.nextBoolean() ? 0 : LightTexture.pack(random.nextInt(16), random.nextInt(16));
                lit.add(new BakedQuad(data, quad.getTintIndex(), quad.getDirection(), null, quad.isShade()));
            }
            float[] pose = store4(stack.last()), normal = store3(stack.last());
            ItemMesh forge = ItemMeshCapture.capture(lit, ItemWriter.FORGE);
            ItemMesh embeddium = ItemMeshCapture.capture(lit, ItemWriter.EMBEDDIUM);
            int[] tints = tintWords(forge, tintArgb);
            int[] a = expandAll(forge, tints, pose, normal, 7, light, false);
            int[] b = expandAll(embeddium, tints, pose, normal, 7, light, true);
            for (int w = 0; w < a.length; w++) {
                if (w % 9 == 8 && a[w] != b[w]) {
                    normalDiffs++;
                    boolean axisOnly = true;
                    int v = w / 9;
                    int baked = lit.get(v / 4).getVertices()[v % 4 * 8 + 7];
                    if ((baked & 0xFFFFFF) != 0) axisOnly = false;
                    // Forge keeps a previous vertex's baked normal when this vertex has none.
                    for (int k = 0; k < v % 4 && axisOnly; k++) {
                        if ((lit.get(v / 4).getVertices()[k * 8 + 7] & 0xFFFFFF) != 0) axisOnly = false;
                    }
                    assertTrue(!axisOnly, "normal differs without any baked normal at word " + w);
                } else if (w % 9 != 8) {
                    assertEquals(a[w], b[w], "trial " + trial + " word " + w);
                }
            }
        }
        assertTrue(normalDiffs >= 0);
    }

    // ---------------------------------------------------------------- Iris

    @Test
    void forgeIrisExtensionIsTheEntityExtension() {
        Random random = new Random(64);
        for (int trial = 0; trial < 3000; trial++) {
            List<BakedQuad> quads = randomQuads(random, new int[] {-1});
            PoseStack stack = randomPose(random);
            ItemMesh mesh = ItemMeshCapture.capture(quads, ItemWriter.FORGE);
            int[] nine = expandAll(mesh, tintWords(mesh, new int[3]), store4(stack.last()), store3(stack.last()),
                    randomOverlay(random), randomLight(random), false);
            int ids = IrisEntityExtension.idsWord(random.nextInt(), random.nextInt());
            int item = IrisEntityExtension.itemWord(random.nextInt());
            int[] expected = new int[mesh.vertexCount() * 14];
            int[] actual = new int[expected.length];
            IrisEntityExtension.extend(nine, 0, mesh.vertexCount(), ids, item, expected, 0);
            ItemReference.extendIris(nine, 0, mesh.vertexCount(), ids, item, false, actual, 0);
            assertArrayEquals(expected, actual, "trial " + trial);
        }
    }

    /** Independent transcription of W8 section 2 with Oculus' real NormalHelper. */
    private static int[] embeddiumIrisExpected(int[] nine, int vertexCount, int entity, int blockEntity, int item) {
        int[] out = new int[vertexCount * 14];
        for (int q = 0; q < vertexCount; q += 4) {
            float[][] quad = new float[4][5];
            for (int k = 0; k < 4; k++) {
                int b = (q + k) * 9;
                quad[k][0] = Float.intBitsToFloat(nine[b]);
                quad[k][1] = Float.intBitsToFloat(nine[b + 1]);
                quad[k][2] = Float.intBitsToFloat(nine[b + 2]);
                quad[k][3] = Float.intBitsToFloat(nine[b + 4]);
                quad[k][4] = Float.intBitsToFloat(nine[b + 5]);
            }
            QuadView view = new QuadView() {
                public float x(int i) { return quad[i][0]; }
                public float y(int i) { return quad[i][1]; }
                public float z(int i) { return quad[i][2]; }
                public float u(int i) { return quad[i][3]; }
                public float v(int i) { return quad[i][4]; }
            };
            int n = nine[(q + 3) * 9 + 8];
            int packed;
            float nx, ny, nz;
            if (n == 0) {
                net.coderbot.iris.vendored.joml.Vector3f face = new net.coderbot.iris.vendored.joml.Vector3f();
                NormalHelper.computeFaceNormal(face, view);
                packed = NormalHelper.packNormal(face, 0.0F);
                nx = face.x;
                ny = face.y;
                nz = face.z;
            } else {
                packed = n;
                nx = (byte) (n & 255) * NORM3B;
                ny = (byte) (n >> 8 & 255) * NORM3B;
                nz = (byte) (n >> 16 & 255) * NORM3B;
            }
            int tangent = NormalHelper.computeTangent(nx, ny, nz, view);
            float uSum = 0.0F, vSum = 0.0F;
            for (int k = 0; k < 4; k++) {
                uSum += quad[k][3];
                vSum += quad[k][4];
            }
            uSum = (float) ((double) uSum * 0.25);
            vSum = (float) ((double) vSum * 0.25);
            for (int k = 0; k < 4; k++) {
                ByteBuffer vertex = ByteBuffer.allocate(56).order(ByteOrder.LITTLE_ENDIAN);
                for (int w = 0; w < 8; w++) vertex.putInt(w * 4, nine[(q + k) * 9 + w]);
                vertex.putInt(32, packed);
                vertex.putShort(36, (short) entity);
                vertex.putShort(38, (short) blockEntity);
                vertex.putShort(40, (short) item);
                vertex.putFloat(42, uSum);
                vertex.putFloat(46, vSum);
                vertex.putInt(50, tangent);
                for (int w = 0; w < 14; w++) out[(q + k) * 14 + w] = vertex.getInt(w * 4);
            }
        }
        return out;
    }

    @Test
    void embeddiumIrisExtensionFollowsOculusDirectWriter() {
        Random random = new Random(96);
        int differsFromEntityExtension = 0;
        for (int trial = 0; trial < 20_000; trial++) {
            List<BakedQuad> quads = randomQuads(random, new int[] {-1, 0});
            PoseStack stack = randomPose(random);
            ItemMesh mesh = ItemMeshCapture.capture(quads, ItemWriter.EMBEDDIUM);
            int[] nine = expandAll(mesh, tintWords(mesh, new int[] {randomTintArgb(random)}), store4(stack.last()),
                    store3(stack.last()), randomOverlay(random), randomLight(random), true);
            if (trial % 50 == 0) nine[(random.nextInt(quads.size()) * 4 + 3) * 9 + 8] = 0; // face normal branch
            int entity = random.nextInt(0x10000), blockEntity = random.nextInt(0x10000), item = random.nextInt(0x10000);
            int[] expected = embeddiumIrisExpected(nine, mesh.vertexCount(), entity, blockEntity, item);
            int[] actual = new int[expected.length];
            ItemReference.extendIris(nine, 0, mesh.vertexCount(), IrisEntityExtension.idsWord(entity, blockEntity),
                    IrisEntityExtension.itemWord(item), true, actual, 0);
            // Bytes 54-55 (top half of word 13) are padding.
            for (int w = 13; w < expected.length; w += 14) {
                expected[w] &= 0xFFFF;
                actual[w] &= 0xFFFF;
            }
            assertArrayEquals(expected, actual, "trial " + trial);
            int[] entityExt = new int[expected.length];
            IrisEntityExtension.extend(nine, 0, mesh.vertexCount(), IrisEntityExtension.idsWord(entity, blockEntity),
                    IrisEntityExtension.itemWord(item), entityExt, 0);
            if (!java.util.Arrays.equals(entityExt, actual)) differsFromEntityExtension++;
        }
        assertTrue(differsFromEntityExtension > 0, "the Embeddium rule must differ from the entity rule somewhere");
    }

    // ---------------------------------------------------------------- capture

    @Test
    void captureRecordsTintRunsAndLayout() {
        Random random = new Random(7);
        for (ItemWriter writer : ItemWriter.values()) {
            for (int trial = 0; trial < 500; trial++) {
                List<BakedQuad> quads = randomQuads(random, new int[] {-1, 0, 1, 2, 5});
                ItemMesh mesh = ItemMeshCapture.capture(quads, writer);
                assertEquals(quads.size() * 4, mesh.vertexCount());
                assertEquals(0, mesh.vertexCount() % 4);
                assertEquals(mesh.vertexCount() * ItemMesh.WORDS_PER_VERTEX, mesh.words().length);
                assertEquals(10, ItemMesh.WORDS_PER_VERTEX);
                for (int q = 0; q < quads.size(); q++) {
                    BakedQuad quad = quads.get(q);
                    assertEquals(quad.getTintIndex() >= 0 ? quad.getTintIndex() : -1, mesh.quadTint()[q]);
                    int[] data = quad.getVertices();
                    for (int v = 0; v < 4; v++) {
                        int d = (q * 4 + v) * ItemMesh.WORDS_PER_VERTEX;
                        assertEquals(data[v * 8], mesh.words()[d]);
                        assertEquals(data[v * 8 + 4], mesh.words()[d + 3]);
                        assertEquals(data[v * 8 + 5], mesh.words()[d + 4]);
                        assertEquals(data[v * 8 + 3], mesh.words()[d + 8]);
                        assertEquals(data[v * 8 + 6], mesh.words()[d + 9]);
                    }
                }
            }
        }
    }

    @Test
    void forgeCaptureCarriesPreviousNormalAndEmbeddiumUsesFacePacking() {
        int[] data = new int[32];
        data[7] = 0;                       // vertex 0: no baked normal -> face (UP)
        data[15] = 127 << 16;              // vertex 1: +z
        data[23] = 0;                      // vertex 2: none -> previous (+z)
        data[31] = (-127 & 255) | (64 << 8); // vertex 3
        BakedQuad quad = new BakedQuad(data, -1, Direction.UP, null, true);
        ItemMesh forge = ItemMeshCapture.capture(List.of(quad), ItemWriter.FORGE);
        float[][] expectedForge = {{0, 1, 0}, {0, 0, 1}, {0, 0, 1}, {-127 / 127.0F, 64 / 127.0F, 0}};
        ItemMesh embeddium = ItemMeshCapture.capture(List.of(quad), ItemWriter.EMBEDDIUM);
        float[][] expectedEmb = {{0, 127 * NORM3B, 0}, {0, 0, 127 * NORM3B}, {0, 127 * NORM3B, 0},
                {-127 * NORM3B, 64 * NORM3B, 0}};
        for (int v = 0; v < 4; v++) {
            for (int c = 0; c < 3; c++) {
                assertEquals(Float.floatToRawIntBits(expectedForge[v][c]),
                        forge.words()[v * 10 + 5 + c], "forge v" + v + " c" + c);
                assertEquals(Float.floatToRawIntBits(expectedEmb[v][c]),
                        embeddium.words()[v * 10 + 5 + c], "embeddium v" + v + " c" + c);
            }
        }
    }

    @Test
    void nonStandardQuadsAreNotCaptured() {
        Random random = new Random(1);
        List<BakedQuad> good = List.of(randomQuad(random, -1));
        BakedQuad big = new BakedQuad(new int[40], -1, Direction.UP, null, true);
        BakedQuad small = new BakedQuad(new int[24], -1, Direction.UP, null, true);
        for (ItemWriter writer : ItemWriter.values()) {
            assertNull(ItemMeshCapture.capture(List.of(good.get(0), big), writer));
            assertNull(ItemMeshCapture.capture(List.of(small), writer));
            assertSame(ItemMesh.EMPTY, ItemMeshCapture.capture(List.of(), writer));
        }
    }
}
