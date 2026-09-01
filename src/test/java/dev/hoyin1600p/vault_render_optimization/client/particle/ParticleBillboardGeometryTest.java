package dev.hoyin1600p.vault_render_optimization.client.particle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

final class ParticleBillboardGeometryTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void constructsVanillaCornerOrderFromCameraBasis() {
        ParticleBillboardGeometry geometry = ParticleBillboardGeometry.compute(
                1.0F, 0.0F, 0.0F,
                0.0F, 1.0F, 0.0F,
                0.0F,
                2.0F,
                10.0F, 20.0F, 30.0F
        );

        assertCorner(geometry.x0(), geometry.y0(), geometry.z0(), 8.0F, 18.0F, 30.0F);
        assertCorner(geometry.x1(), geometry.y1(), geometry.z1(), 8.0F, 22.0F, 30.0F);
        assertCorner(geometry.x2(), geometry.y2(), geometry.z2(), 12.0F, 22.0F, 30.0F);
        assertCorner(geometry.x3(), geometry.y3(), geometry.z3(), 12.0F, 18.0F, 30.0F);
    }

    @Test
    void rotatesLeftAndUpBasisForParticleRoll() {
        ParticleBillboardGeometry geometry = ParticleBillboardGeometry.compute(
                1.0F, 0.0F, 0.0F,
                0.0F, 1.0F, 0.0F,
                (float) (Math.PI / 2.0D),
                2.0F,
                10.0F, 20.0F, 30.0F
        );

        assertCorner(geometry.x0(), geometry.y0(), geometry.z0(), 12.0F, 18.0F, 30.0F);
        assertCorner(geometry.x1(), geometry.y1(), geometry.z1(), 8.0F, 18.0F, 30.0F);
        assertCorner(geometry.x2(), geometry.y2(), geometry.z2(), 8.0F, 22.0F, 30.0F);
        assertCorner(geometry.x3(), geometry.y3(), geometry.z3(), 12.0F, 22.0F, 30.0F);
    }

    @Test
    void reusesOneGeometryObjectPerThread() {
        ParticleBillboardGeometry first = ParticleBillboardGeometry.compute(
                1.0F, 0.0F, 0.0F,
                0.0F, 1.0F, 0.0F,
                0.0F, 1.0F, 0.0F, 0.0F, 0.0F
        );
        ParticleBillboardGeometry second = ParticleBillboardGeometry.compute(
                1.0F, 0.0F, 0.0F,
                0.0F, 1.0F, 0.0F,
                0.0F, 1.0F, 1.0F, 1.0F, 1.0F
        );

        assertSame(first, second);
    }

    private static void assertCorner(
            float actualX,
            float actualY,
            float actualZ,
            float expectedX,
            float expectedY,
            float expectedZ
    ) {
        assertEquals(expectedX, actualX, EPSILON);
        assertEquals(expectedY, actualY, EPSILON);
        assertEquals(expectedZ, actualZ, EPSILON);
    }
}
