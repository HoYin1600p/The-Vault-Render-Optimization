package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

/**
 * Java reference for the GPU expansion shader, and the oracle for its startup self-test. Uses
 * exactly {@code Vector4f.transform} and {@code Vector3f.transform}'s float operations in their order
 * ({@code ((m00*x + m01*y) + m02*z) + m03*w}), with no normalization, so its words equal what
 * {@code ModelPart.render} writes through {@code BufferBuilder}.
 *
 * <p>Matrices are column-major as {@code Matrix4f.store}/{@code Matrix3f.store} write them:
 * {@code pose[c * 4 + r]} and {@code normal[c * 3 + r]}.
 */
public final class ReferenceExpander {
    private ReferenceExpander() {
    }

    public static void expand(ModelMesh mesh, float[] pose, float[] normal, int color, int overlay, int light,
                              int[] out, int outOffset) {
        expand(mesh, pose, normal, color, overlay, light, null, out, outOffset);
    }

    /**
     * @param sprite null, or {@code {u0, u1 - u0, v0, v1 - v0}}: UVs are remapped exactly as
     *               {@code SpriteCoordinateExpander} does, {@code sprite.getU(u * 16F)} =
     *               {@code u0 + (u1 - u0) * (u * 16F) / 16F}
     */
    public static void expand(ModelMesh mesh, float[] pose, float[] normal, int color, int overlay, int light,
                              float[] sprite, int[] out, int outOffset) {
        expand(mesh, pose, normal, color, overlay, light, sprite, 0, out, outOffset);
    }

    /**
     * @param flips {@link InstanceRecord#FLIPS} bits: GeckoLib's {@code IGeoRenderer.renderCube} negates a
     *              negative transformed normal component of a cube that is flat along another axis
     *              ({@code normal.mul(-1, 1, 1)} and so on; multiplying by 1 is exact)
     */
    public static void expand(ModelMesh mesh, float[] pose, float[] normal, int color, int overlay, int light,
                              float[] sprite, int flips, int[] out, int outOffset) {
        float m00 = pose[0], m10 = pose[1], m20 = pose[2];
        float m01 = pose[4], m11 = pose[5], m21 = pose[6];
        float m02 = pose[8], m12 = pose[9], m22 = pose[10];
        float m03 = pose[12], m13 = pose[13], m23 = pose[14];
        float n00 = normal[0], n10 = normal[1], n20 = normal[2];
        float n01 = normal[3], n11 = normal[4], n21 = normal[5];
        float n02 = normal[6], n12 = normal[7], n22 = normal[8];
        int overlayWord = EntityVertexPacking.shorts(overlay);
        int lightWord = EntityVertexPacking.shorts(light);
        float[] data = mesh.data();
        int o = outOffset;
        for (int v = 0; v < mesh.vertexCount(); v++) {
            int d = v * ModelMesh.FLOATS_PER_VERTEX;
            float x = data[d], y = data[d + 1], z = data[d + 2];
            float nx = data[d + 5], ny = data[d + 6], nz = data[d + 7];
            float px = m00 * x + m01 * y + m02 * z + m03 * 1.0F;
            float py = m10 * x + m11 * y + m12 * z + m13 * 1.0F;
            float pz = m20 * x + m21 * y + m22 * z + m23 * 1.0F;
            float tx = n00 * nx + n01 * ny + n02 * nz;
            float ty = n10 * nx + n11 * ny + n12 * nz;
            float tz = n20 * nx + n21 * ny + n22 * nz;
            if ((flips & InstanceRecord.FLIP_X) != 0 && tx < 0.0F) tx *= -1.0F;
            if ((flips & InstanceRecord.FLIP_Y) != 0 && ty < 0.0F) ty *= -1.0F;
            if ((flips & InstanceRecord.FLIP_Z) != 0 && tz < 0.0F) tz *= -1.0F;
            out[o++] = Float.floatToRawIntBits(px);
            out[o++] = Float.floatToRawIntBits(py);
            out[o++] = Float.floatToRawIntBits(pz);
            out[o++] = color;
            float tu = data[d + 3], tv = data[d + 4];
            if (sprite != null) {
                tu = sprite[0] + sprite[1] * (float) (double) (tu * 16.0F) / 16.0F;
                tv = sprite[2] + sprite[3] * (float) (double) (tv * 16.0F) / 16.0F;
            }
            out[o++] = Float.floatToRawIntBits(tu);
            out[o++] = Float.floatToRawIntBits(tv);
            out[o++] = overlayWord;
            out[o++] = lightWord;
            out[o++] = EntityVertexPacking.normal(tx, ty, tz);
        }
    }
}
