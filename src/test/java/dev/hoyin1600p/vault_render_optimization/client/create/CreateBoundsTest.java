package dev.hoyin1600p.vault_render_optimization.client.create;

import dev.hoyin1600p.vault_render_optimization.mixin.Matrix4fAccessor;
import net.minecraft.world.phys.AABB;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CreateBoundsTest {
    record Rows(float[] m) implements Matrix4fAccessor {
        public float vro$m00(){return m[0];} public float vro$m01(){return m[1];}
        public float vro$m02(){return m[2];} public float vro$m03(){return m[3];}
        public float vro$m10(){return m[4];} public float vro$m11(){return m[5];}
        public float vro$m12(){return m[6];} public float vro$m13(){return m[7];}
        public float vro$m20(){return m[8];} public float vro$m21(){return m[9];}
        public float vro$m22(){return m[10];} public float vro$m23(){return m[11];}
    }

    @Test void matchesEightCornersExactlyForRotationShearReflectionAndLargeCoordinates() {
        var random = new Random(0xA57A);
        for (int trial = 0; trial < 10000; trial++) {
            float[] m = new float[12];
            for (int i = 0; i < m.length; i++) m[i] = (random.nextFloat() - .5f) * 10;
            double scale = trial % 2 == 0 ? 30_000_000 : 100;
            var box = new AABB(random.nextDouble()*scale, random.nextDouble()*scale,
                    random.nextDouble()*scale, random.nextDouble()*scale,
                    random.nextDouble()*scale, random.nextDouble()*scale);
            double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
            double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
            for (int corner = 0; corner < 8; corner++) {
                float x = (float) ((corner & 1) == 0 ? box.minX : box.maxX);
                float y = (float) ((corner & 2) == 0 ? box.minY : box.maxY);
                float z = (float) ((corner & 4) == 0 ? box.minZ : box.maxZ);
                for (int row = 0; row < 3; row++) {
                    int k = row*4;
                    float value = m[k]*x + m[k+1]*y + m[k+2]*z + m[k+3];
                    min[row] = Math.min(min[row], value); max[row] = Math.max(max[row], value);
                }
            }
            assertEquals(new AABB(min[0], min[1], min[2], max[0], max[1], max[2]),
                    CreateRenderContext.transformBounds(box, new Rows(m)));
        }
    }
}
