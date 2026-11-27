package dev.hoyin1600p.vault_render_optimization.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

final class AngleSectionOcclusionTest {
    private static final long ALL_DIRECTIONS = ~0L;

    @Test
    void masksOnlyOpposingAxesThatCameraAngleCannotCross() {
        // Section [0..16]^3 has center (8, 8, 8).
        // Camera directly along +X at (40, 8, 8): dx=32, dy=0, dz=0 -> only WEST<->EAST can be crossed.
        long maskX = AngleSectionOcclusion.computeAngleMask(40.0, 8.0, 8.0, 0, 0, 0);
        assertEquals(0L, maskX & AngleSectionOcclusion.UP_DOWN_OCCLUDED);
        assertEquals(0L, maskX & AngleSectionOcclusion.NORTH_SOUTH_OCCLUDED);
        assertEquals(AngleSectionOcclusion.WEST_EAST_OCCLUDED, maskX & AngleSectionOcclusion.WEST_EAST_OCCLUDED);

        // Camera directly along +Y at (8, 56, 8): dy=48 -> only DOWN<->UP can be crossed.
        long maskY = AngleSectionOcclusion.computeAngleMask(8.0, 56.0, 8.0, 0, 0, 0);
        assertEquals(AngleSectionOcclusion.UP_DOWN_OCCLUDED, maskY & AngleSectionOcclusion.UP_DOWN_OCCLUDED);
        assertEquals(0L, maskY & AngleSectionOcclusion.NORTH_SOUTH_OCCLUDED);
        assertEquals(0L, maskY & AngleSectionOcclusion.WEST_EAST_OCCLUDED);

        // Exact diagonal (dx == dy == dz): none of the strict inequalities hold, so nothing is occluded.
        long maskDiag = AngleSectionOcclusion.computeAngleMask(32.0, 32.0, 32.0, 0, 0, 0);
        assertEquals(ALL_DIRECTIONS, maskDiag);
    }

    @Test
    void nonOpposingTransitionsAreNeverMasked() {
        long mask = AngleSectionOcclusion.computeAngleMask(100.0, 12.0, 9.0, 0, 0, 0);
        // Orthogonal pair (e.g., WEST=4 -> UP=1, DOWN=0 -> NORTH=2) must always remain set.
        long westToUp = 1L << ((4 << 3) | 1);
        long downToNorth = 1L << ((0 << 3) | 2);
        long southToEast = 1L << ((3 << 3) | 5);
        assertNotEquals(0L, mask & westToUp);
        assertNotEquals(0L, mask & downToNorth);
        assertNotEquals(0L, mask & southToEast);
    }

    @Test
    void monteCarloRaysEnteringAndExitingOppositeFacesAreNeverFalseCulled() {
        Random rng = new Random(0x5EED_2811L);
        int originX = -32;
        int originY = 64;
        int originZ = 16;

        for (int i = 0; i < 100_000; i++) {
            // Sample a random ray passing from one face of [origin, origin+16]^3 to the opposite face,
            // then place a camera along that ray outside the cube on the entry side.
            int axis = rng.nextInt(3);
            boolean positiveEntry = rng.nextBoolean();
            double u1 = rng.nextDouble() * 16.0;
            double v1 = rng.nextDouble() * 16.0;
            double u2 = rng.nextDouble() * 16.0;
            double v2 = rng.nextDouble() * 16.0;
            double tCam = 0.001 + rng.nextDouble() * 12.0;

            double[] pEntry = new double[3];
            double[] pExit = new double[3];
            int uAxis = (axis + 1) % 3;
            int vAxis = (axis + 2) % 3;
            double[] origins = {originX, originY, originZ};

            pEntry[axis] = origins[axis] + (positiveEntry ? 16.0 : 0.0);
            pExit[axis] = origins[axis] + (positiveEntry ? 0.0 : 16.0);
            pEntry[uAxis] = origins[uAxis] + u1;
            pEntry[vAxis] = origins[vAxis] + v1;
            pExit[uAxis] = origins[uAxis] + u2;
            pExit[vAxis] = origins[vAxis] + v2;

            // Camera lies behind pEntry along the line from pExit through pEntry.
            double camX = pEntry[0] + tCam * (pEntry[0] - pExit[0]);
            double camY = pEntry[1] + tCam * (pEntry[1] - pExit[1]);
            double camZ = pEntry[2] + tCam * (pEntry[2] - pExit[2]);

            long mask = AngleSectionOcclusion.computeAngleMask(camX, camY, camZ, originX, originY, originZ);
            long requiredBit = axis == 0 ? AngleSectionOcclusion.WEST_EAST_OCCLUDED
                    : axis == 1 ? AngleSectionOcclusion.UP_DOWN_OCCLUDED
                    : AngleSectionOcclusion.NORTH_SOUTH_OCCLUDED;
            assertTrue(
                    (mask & requiredBit) == requiredBit,
                    () -> "False occlusion on axis " + axis + " for camera (" + camX + "," + camY + "," + camZ + ")"
            );
        }
    }
}
