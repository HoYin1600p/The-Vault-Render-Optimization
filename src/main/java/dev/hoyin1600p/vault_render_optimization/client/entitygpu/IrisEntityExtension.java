package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import net.minecraft.util.Mth;

/**
 * Oculus' extended entity vertex ({@code IrisVertexFormats.ENTITY}, 56 bytes) as Oculus 1.6.x writes it while
 * a shader pack is active: the vanilla {@code NEW_ENTITY} elements, then {@code iris_Entity} (entity, block
 * entity and item IDs as shorts), {@code mc_midTexCoord} (two floats), {@code at_tangent} (four bytes) and two
 * padding bytes. After each quad, Oculus' {@code MixinBufferBuilder.fillExtendedData} overwrites every vertex's
 * normal with the quad's face normal and writes the mid UV and the tangent; the float arithmetic below is
 * {@code NormalHelper.computeFaceNormal}, {@code computeTangent} and {@code packNormal} operation by operation.
 *
 * <p>The words here are fourteen little-endian 32-bit words per vertex. Oculus never writes the two padding
 * bytes (they are skipped, and are not a vertex attribute); VRO writes them as zero.
 */
public final class IrisEntityExtension {
    public static final int WORDS_PER_VERTEX = 14;
    public static final int VERTEX_BYTES = WORDS_PER_VERTEX * 4;

    private IrisEntityExtension() {
    }

    /** The two record words that carry Oculus' {@code CapturedRenderingState} IDs, as shorts. */
    public static int idsWord(int entity, int blockEntity) {
        return (entity & 0xFFFF) | ((blockEntity & 0xFFFF) << 16);
    }

    public static int itemWord(int item) {
        return item & 0xFFFF;
    }

    /**
     * Extends {@code vertexCount} (a multiple of four) {@code NEW_ENTITY} vertices of nine words each into
     * Oculus' extended vertices of fourteen words.
     */
    public static void extend(int[] in, int inOffset, int vertexCount, int idsWord, int itemWord, int[] out,
                              int outOffset) {
        for (int q = 0; q + 4 <= vertexCount; q += 4) {
            int b = inOffset + q * EntityVertexPacking.WORDS_PER_VERTEX;
            float x0 = f(in, b, 0, 0), y0 = f(in, b, 0, 1), z0 = f(in, b, 0, 2);
            float x1 = f(in, b, 1, 0), y1 = f(in, b, 1, 1), z1 = f(in, b, 1, 2);
            float x2 = f(in, b, 2, 0), y2 = f(in, b, 2, 1), z2 = f(in, b, 2, 2);
            float x3 = f(in, b, 3, 0), y3 = f(in, b, 3, 1), z3 = f(in, b, 3, 2);
            float u0 = f(in, b, 0, 4), v0 = f(in, b, 0, 5);
            float u1 = f(in, b, 1, 4), v1 = f(in, b, 1, 5);
            float u2 = f(in, b, 2, 4), v2 = f(in, b, 2, 5);
            float u3 = f(in, b, 3, 4), v3 = f(in, b, 3, 5);

            // fillExtendedData: sums from 0F in vertex order, divided by the vertex count.
            float midU = 0.0F;
            midU += u0;
            midU += u1;
            midU += u2;
            midU += u3;
            midU /= 4.0F;
            float midV = 0.0F;
            midV += v0;
            midV += v1;
            midV += v2;
            midV += v3;
            midV /= 4.0F;

            // NormalHelper.computeFaceNormal.
            float dx0 = x2 - x0, dy0 = y2 - y0, dz0 = z2 - z0;
            float dx1 = x3 - x1, dy1 = y3 - y1, dz1 = z3 - z1;
            float nx = dy0 * dz1 - dz0 * dy1;
            float ny = dz0 * dx1 - dx0 * dz1;
            float nz = dx0 * dy1 - dy0 * dx1;
            float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (l != 0.0F) {
                nx /= l;
                ny /= l;
                nz /= l;
            }
            int normal = packNormal(nx, ny, nz, 0.0F);
            int tangent = tangent(nx, ny, nz, x0, y0, z0, x1, y1, z1, x2, y2, z2, u0, v0, u1, v1, u2, v2);

            int mu = Float.floatToRawIntBits(midU);
            int mv = Float.floatToRawIntBits(midV);
            for (int k = 0; k < 4; k++) {
                int s = b + k * EntityVertexPacking.WORDS_PER_VERTEX;
                int o = outOffset + (q + k) * WORDS_PER_VERTEX;
                System.arraycopy(in, s, out, o, 8);
                out[o + 8] = normal;
                out[o + 9] = idsWord;
                out[o + 10] = (itemWord & 0xFFFF) | (mu << 16);
                out[o + 11] = (mu >>> 16) | (mv << 16);
                out[o + 12] = (mv >>> 16) | (tangent << 16);
                out[o + 13] = tangent >>> 16;
            }
        }
    }

    private static float f(int[] words, int base, int vertex, int word) {
        return Float.intBitsToFloat(words[base + vertex * EntityVertexPacking.WORDS_PER_VERTEX + word]);
    }

    /** {@code NormalHelper.computeTangent(normalX, normalY, normalZ, polygon)} over vertices 0 to 2. */
    static int tangent(float normalX, float normalY, float normalZ, float x0, float y0, float z0, float x1, float y1,
                       float z1, float x2, float y2, float z2, float u0, float v0, float u1, float v1, float u2,
                       float v2) {
        float edge1x = x1 - x0, edge1y = y1 - y0, edge1z = z1 - z0;
        float edge2x = x2 - x0, edge2y = y2 - y0, edge2z = z2 - z0;
        float deltaU1 = u1 - u0, deltaV1 = v1 - v0;
        float deltaU2 = u2 - u0, deltaV2 = v2 - v0;
        float fdenom = deltaU1 * deltaV2 - deltaU2 * deltaV1;
        float f = (double) fdenom == 0.0 ? 1.0F : 1.0F / fdenom;
        float tangentX = f * (deltaV2 * edge1x - deltaV1 * edge2x);
        float tangentY = f * (deltaV2 * edge1y - deltaV1 * edge2y);
        float tangentZ = f * (deltaV2 * edge1z - deltaV1 * edge2z);
        float tcoeff = rsqrt(tangentX * tangentX + tangentY * tangentY + tangentZ * tangentZ);
        tangentX *= tcoeff;
        tangentY *= tcoeff;
        tangentZ *= tcoeff;
        float bitangentX = f * (-deltaU2 * edge1x + deltaU1 * edge2x);
        float bitangentY = f * (-deltaU2 * edge1y + deltaU1 * edge2y);
        float bitangentZ = f * (-deltaU2 * edge1z + deltaU1 * edge2z);
        float bcoeff = rsqrt(bitangentX * bitangentX + bitangentY * bitangentY + bitangentZ * bitangentZ);
        bitangentX *= bcoeff;
        bitangentY *= bcoeff;
        bitangentZ *= bcoeff;
        float pbitangentX = tangentY * normalZ - tangentZ * normalY;
        float pbitangentY = tangentZ * normalX - tangentX * normalZ;
        float pbitangentZ = tangentX * normalY - tangentY * normalX;
        float dot = bitangentX * pbitangentX + bitangentY * pbitangentY + bitangentZ * pbitangentZ;
        float tangentW = dot < 0.0F ? -1.0F : 1.0F;
        return packNormal(tangentX, tangentY, tangentZ, tangentW);
    }

    /** {@code NormalHelper.rsqrt}: 1 for 0, else the double reciprocal square root narrowed to float. */
    static float rsqrt(float value) {
        if (value == 0.0F) return 1.0F;
        return (float) (1.0 / Math.sqrt(value));
    }

    /** {@code NormalHelper.packNormal(x, y, z, w)}. */
    static int packNormal(float x, float y, float z, float w) {
        x = Mth.clamp(x, -1.0F, 1.0F);
        y = Mth.clamp(y, -1.0F, 1.0F);
        z = Mth.clamp(z, -1.0F, 1.0F);
        w = Mth.clamp(w, -1.0F, 1.0F);
        return ((int) (x * 127.0F) & 255) | (((int) (y * 127.0F) & 255) << 8) | (((int) (z * 127.0F) & 255) << 16)
                | (((int) (w * 127.0F) & 255) << 24);
    }
}
