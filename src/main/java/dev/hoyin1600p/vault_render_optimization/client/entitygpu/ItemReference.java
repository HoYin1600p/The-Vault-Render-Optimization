package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

/**
 * Java reference (oracle) for the GPU item expansion. Reproduces, word for word, what Forge's
 * {@code putBulkData} or Embeddium's {@code renderQuadList} overwrite writes into a {@code NEW_ENTITY}
 * {@code BufferBuilder} for one baked quad list, and what Oculus adds on top of it under a shader pack.
 *
 * <p>Matrices are column-major as {@code Matrix4f.store}/{@code Matrix3f.store} write them, as in
 * {@link ReferenceExpander}. The tint is an ABGR word ({@code -1} for untinted).
 */
public final class ItemReference {
    private ItemReference() {
    }

    public static void expand(ItemMesh mesh, int firstVertex, int vertexCount, float[] pose, float[] normal,
                              int tint, int overlay, int light, boolean embeddium, int[] out, int outOffset) {
        float m00 = pose[0], m10 = pose[1], m20 = pose[2];
        float m01 = pose[4], m11 = pose[5], m21 = pose[6];
        float m02 = pose[8], m12 = pose[9], m22 = pose[10];
        float m03 = pose[12], m13 = pose[13], m23 = pose[14];
        float n00 = normal[0], n10 = normal[1], n20 = normal[2];
        float n01 = normal[3], n11 = normal[4], n21 = normal[5];
        float n02 = normal[6], n12 = normal[7], n22 = normal[8];
        int overlayWord = EntityVertexPacking.shorts(overlay);
        int[] words = mesh.words();
        int o = outOffset;
        for (int v = firstVertex; v < firstVertex + vertexCount; v++) {
            int d = v * ItemMesh.WORDS_PER_VERTEX;
            float x = Float.intBitsToFloat(words[d]);
            float y = Float.intBitsToFloat(words[d + 1]);
            float z = Float.intBitsToFloat(words[d + 2]);
            float nx = Float.intBitsToFloat(words[d + 5]);
            float ny = Float.intBitsToFloat(words[d + 6]);
            float nz = Float.intBitsToFloat(words[d + 7]);
            int baked = words[d + 8];
            int bakedLight = words[d + 9];
            float px = m00 * x + m01 * y + m02 * z + m03 * 1.0F;
            float py = m10 * x + m11 * y + m12 * z + m13 * 1.0F;
            float pz = m20 * x + m21 * y + m22 * z + m23 * 1.0F;
            float tx = n00 * nx + n01 * ny + n02 * nz;
            float ty = n10 * nx + n11 * ny + n12 * nz;
            float tz = n20 * nx + n21 * ny + n22 * nz;
            out[o++] = Float.floatToRawIntBits(px);
            out[o++] = Float.floatToRawIntBits(py);
            out[o++] = Float.floatToRawIntBits(pz);
            out[o++] = multiply(baked, tint);
            out[o++] = words[d + 3];
            out[o++] = words[d + 4];
            out[o++] = overlayWord;
            out[o++] = embeddium ? embeddiumLight(bakedLight, light) : forgeLight(bakedLight, light);
            out[o++] = EntityVertexPacking.normal(tx, ty, tz);
        }
    }

    /** Per byte lane {@code (q * t) / 255}; equals both writers' float math for every (q, t). */
    static int multiply(int baked, int tint) {
        int result = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            int q = baked >>> shift & 255;
            int t = tint >>> shift & 255;
            result |= (q * t / 255) << shift;
        }
        return result;
    }

    /** Forge {@code applyBakedLighting}: unsigned 16-bit maxima of both halves. */
    static int forgeLight(int baked, int light) {
        int bl = Math.max(light & 0xFFFF, baked & 0xFFFF);
        int sl = Math.max(light >>> 16 & 0xFFFF, baked >>> 16 & 0xFFFF);
        return bl | sl << 16;
    }

    /** Embeddium {@code mergeBakedLight}. */
    static int embeddiumLight(int baked, int light) {
        if (baked == 0) return light;
        return (Math.max(baked >> 16 & 255, light >> 16 & 255) << 16) | Math.max(baked & 255, light & 255);
    }

    /**
     * Extends {@code vertexCount} (a multiple of four) {@code NEW_ENTITY} vertices into Oculus' fourteen-word
     * extended vertices. FORGE goes through Oculus' {@code fillExtendedData}, exactly
     * {@link IrisEntityExtension#extend}. EMBEDDIUM's direct extended writer stores the normal word of the
     * quad's fourth vertex in all four vertices and computes the tangent from that word unpacked with
     * {@code (byte) * 0.007874016F} (not normalized); a zero normal word takes the face normal branch.
     */
    public static void extendIris(int[] nine, int inOffset, int vertexCount, int idsWord, int itemWord,
                                  boolean embeddium, int[] out, int outOffset) {
        IrisEntityExtension.extend(nine, inOffset, vertexCount, idsWord, itemWord, out, outOffset);
        if (!embeddium) return;
        int in9 = EntityVertexPacking.WORDS_PER_VERTEX;
        int out14 = IrisEntityExtension.WORDS_PER_VERTEX;
        for (int q = 0; q + 4 <= vertexCount; q += 4) {
            int b = inOffset + q * in9;
            int word = nine[b + 3 * in9 + 8];
            if (word == 0) continue; // face normal branch, already written by extend
            float ux = (byte) word * 0.007874016F;
            float uy = (byte) (word >> 8) * 0.007874016F;
            float uz = (byte) (word >> 16) * 0.007874016F;
            int tangent = IrisEntityExtension.tangent(ux, uy, uz,
                    f(nine, b, 0, 0), f(nine, b, 0, 1), f(nine, b, 0, 2),
                    f(nine, b, 1, 0), f(nine, b, 1, 1), f(nine, b, 1, 2),
                    f(nine, b, 2, 0), f(nine, b, 2, 1), f(nine, b, 2, 2),
                    f(nine, b, 0, 4), f(nine, b, 0, 5), f(nine, b, 1, 4), f(nine, b, 1, 5),
                    f(nine, b, 2, 4), f(nine, b, 2, 5));
            for (int k = 0; k < 4; k++) {
                int o = outOffset + (q + k) * out14;
                int midV = out[o + 11] >>> 16 | out[o + 12] << 16;
                out[o + 8] = word;
                out[o + 12] = midV >>> 16 | tangent << 16;
                out[o + 13] = tangent >>> 16;
            }
        }
    }

    private static float f(int[] words, int base, int vertex, int word) {
        return Float.intBitsToFloat(words[base + vertex * EntityVertexPacking.WORDS_PER_VERTEX + word]);
    }
}
