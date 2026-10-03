package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;
import net.coderbot.iris.vendored.joml.Vector3f;
import net.coderbot.iris.vertices.NormalHelper;
import net.coderbot.iris.vertices.QuadView;
import org.junit.jupiter.api.Test;

/**
 * Oculus' own {@code NormalHelper} is the oracle: {@link IrisEntityExtension} must give its face normal, packed
 * normal and tangent for random, flat, degenerate and repeated-UV quads, exactly as {@code fillExtendedData}
 * combines them.
 */
class IrisEntityExtensionTest {
    @Test
    void extensionMatchesOculusNormalHelper() {
        Random random = new Random(1600);
        float[][] quad = new float[4][5];
        QuadView view = new QuadView() {
            public float x(int i) { return quad[i][0]; }
            public float y(int i) { return quad[i][1]; }
            public float z(int i) { return quad[i][2]; }
            public float u(int i) { return quad[i][3]; }
            public float v(int i) { return quad[i][4]; }
        };
        int[] in = new int[4 * EntityVertexPacking.WORDS_PER_VERTEX];
        int[] out = new int[4 * IrisEntityExtension.WORDS_PER_VERTEX];
        Vector3f normal = new Vector3f();
        for (int trial = 0; trial < 200_000; trial++) {
            int shape = random.nextInt(6);
            for (int k = 0; k < 4; k++) {
                for (int c = 0; c < 3; c++) {
                    quad[k][c] = switch (shape) {
                        case 0 -> 0.25F;                                   // every vertex the same: zero normal
                        case 1 -> c == 1 ? 0.5F : random.nextFloat() * 4 - 2; // flat in y
                        case 2 -> (random.nextFloat() - 0.5F) * 1e-4F;       // tiny faces
                        default -> (random.nextFloat() - 0.5F) * 64;
                    };
                }
                quad[k][3] = shape == 1 && random.nextBoolean() ? 0.125F : random.nextInt(64) / 64.0F;
                quad[k][4] = random.nextInt(4) == 0 ? quad[0][4] : random.nextInt(64) / 64.0F;
            }
            for (int k = 0; k < 4; k++) {
                int b = k * EntityVertexPacking.WORDS_PER_VERTEX;
                in[b] = Float.floatToRawIntBits(quad[k][0]);
                in[b + 1] = Float.floatToRawIntBits(quad[k][1]);
                in[b + 2] = Float.floatToRawIntBits(quad[k][2]);
                in[b + 3] = random.nextInt();
                in[b + 4] = Float.floatToRawIntBits(quad[k][3]);
                in[b + 5] = Float.floatToRawIntBits(quad[k][4]);
                in[b + 6] = random.nextInt();
                in[b + 7] = random.nextInt();
                in[b + 8] = random.nextInt();
            }
            int entity = random.nextInt(), blockEntity = random.nextInt(), item = random.nextInt();
            IrisEntityExtension.extend(in, 0, 4, IrisEntityExtension.idsWord(entity, blockEntity),
                    IrisEntityExtension.itemWord(item), out, 0);

            NormalHelper.computeFaceNormal(normal, view);
            int packed = NormalHelper.packNormal(normal, 0.0F);
            int tangent = NormalHelper.computeTangent(normal.x, normal.y, normal.z, view);
            float midU = 0.0F, midV = 0.0F;
            for (int k = 0; k < 4; k++) {
                midU += quad[k][3];
                midV += quad[k][4];
            }
            midU /= 4.0F;
            midV /= 4.0F;
            for (int k = 0; k < 4; k++) {
                int o = k * IrisEntityExtension.WORDS_PER_VERTEX;
                String at = "trial " + trial + " vertex " + k;
                for (int w = 0; w < 8; w++) assertEquals(in[k * EntityVertexPacking.WORDS_PER_VERTEX + w], out[o + w], at);
                assertEquals(packed, out[o + 8], at + " normal");
                assertEquals((entity & 0xFFFF) | (blockEntity << 16), out[o + 9], at + " ids");
                long bytes40 = (out[o + 10] & 0xFFFFFFFFL) | ((long) out[o + 11] << 32);
                assertEquals(item & 0xFFFF, (int) (bytes40 & 0xFFFF), at + " item");
                assertEquals(Float.floatToRawIntBits(midU), (int) (bytes40 >>> 16), at + " midU");
                long bytes48 = (out[o + 12] & 0xFFFFFFFFL) | ((long) out[o + 13] << 32);
                int midVBits = (out[o + 11] >>> 16) | (out[o + 12] << 16);
                assertEquals(Float.floatToRawIntBits(midV), midVBits, at + " midV");
                assertEquals(tangent, (int) (bytes48 >>> 16), at + " tangent");
                assertEquals(0, out[o + 13] >>> 16, at + " padding");
            }
        }
    }
}
