package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.iris;

import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import java.nio.FloatBuffer;

/** Per-wrapper exact-value cache, never shared between shader programs/passes. */
public final class NormalMatrixCache {
    private final Matrix4f previous = new Matrix4f();
    private final Matrix4f inverse = new Matrix4f();
    private final Matrix3f normal = new Matrix3f();
    private final FloatBuffer values = FloatBuffer.allocate(16);
    private boolean valid;

    public Matrix3f get(Matrix4f modelView) {
        if (!valid || !previous.equals(modelView)) {
            previous.load(modelView);
            inverse.load(modelView);
            // Retain original 4x4 inversion semantics, including singular matrices.
            inverse.invert();
            inverse.transpose();
            inverse.store(values);
            for (int row = 0; row < 3; row++)
                for (int col = 0; col < 3; col++) normal.set(row, col, values.get(col * 4 + row));
            valid = true;
        }
        return normal;
    }
}
