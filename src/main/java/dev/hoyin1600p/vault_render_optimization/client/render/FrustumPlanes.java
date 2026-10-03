package dev.hoyin1600p.vault_render_optimization.client.render;

import com.mojang.math.Vector4f;

/**
 * Allocation-free replacement for vanilla {@code Frustum.cubeInFrustum(FFFFFF)}.
 *
 * <p>Vanilla builds eight {@code Vector4f} corners per plane and rejects the box when every
 * {@code plane.dot(corner) > 0} test fails. The corner that maximizes the dot product picks, per axis,
 * the maximum coordinate for a positive plane component and the minimum otherwise. Its dot product is
 * computed here with exactly {@code Vector4f.dot}'s float operations and order
 * ({@code ((a*x + b*y) + c*z) + w*1}). Round-to-nearest multiplication and addition are monotonic in
 * each operand, so that corner's float result is at least every other corner's float result, and the
 * answer is bit-for-bit vanilla's. Non-finite coordinates break the monotonic argument (NaN,
 * infinity minus infinity), so the caller falls back to vanilla for them.
 */
public final class FrustumPlanes {
    private FrustumPlanes() {
    }

    public static boolean finite(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return Float.isFinite(minX) && Float.isFinite(minY) && Float.isFinite(minZ)
                && Float.isFinite(maxX) && Float.isFinite(maxY) && Float.isFinite(maxZ);
    }

    /** Live check counters for {@code /vro feature frustum verify}; touched only on the render thread. */
    public static volatile boolean verify;
    public static long verifyChecks;
    public static long verifyMismatches;

    /** Vanilla's test: some of the eight corners must be strictly in front of every plane. */
    public static boolean reference(Vector4f[] planes, float minX, float minY, float minZ,
                                    float maxX, float maxY, float maxZ) {
        for (Vector4f plane : planes) {
            float a = plane.x(), b = plane.y(), c = plane.z(), w = plane.w();
            boolean any = false;
            for (int corner = 0; corner < 8 && !any; corner++) {
                float x = (corner & 1) == 0 ? minX : maxX;
                float y = (corner & 2) == 0 ? minY : maxY;
                float z = (corner & 4) == 0 ? minZ : maxZ;
                any = a * x + b * y + c * z + w * 1.0F > 0.0F;
            }
            if (!any) {
                return false;
            }
        }
        return true;
    }

    public static boolean intersects(Vector4f[] planes, float minX, float minY, float minZ,
                                     float maxX, float maxY, float maxZ) {
        for (Vector4f plane : planes) {
            float a = plane.x(), b = plane.y(), c = plane.z(), w = plane.w();
            float x = a > 0.0F ? maxX : minX;
            float y = b > 0.0F ? maxY : minY;
            float z = c > 0.0F ? maxZ : minZ;
            if (!(a * x + b * y + c * z + w * 1.0F > 0.0F)) {
                return false;
            }
        }
        return true;
    }
}
