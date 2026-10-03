package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader;

import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.iris.NormalMatrixCache;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NormalMatrixCacheTest {
    @Test void matchesOriginalAndDetectsInPlaceChangesAndSingularInput() {
        var cache = new NormalMatrixCache();
        var input = Matrix4f.createScaleMatrix(2, 3, -4);
        input.multiplyWithTranslation(1, 2, 3);
        var reused = cache.get(input);
        for (int i = 0; i < 20; i++) {
            var expected = new Matrix4f(input);
            expected.invert(); expected.transpose();
            assertEquals(new Matrix3f(expected), cache.get(input));
            assertSame(reused, cache.get(input));
            input.multiply(Matrix4f.createScaleMatrix(1.1f, .9f, 1));
        }
        input.load(new Matrix4f());
        assertEquals(new Matrix3f(), cache.get(input));
    }
}
