package dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BarrelFrontVisibilityTest {
    @Test
    void keepsCameraOnOrInFrontOfDisplayPlane() {
        assertTrue(BarrelFrontVisibility.cameraIsInFront(
                10.0D, 4.5D, 3.5D,
                3.5D, 4.5D, 3.5D,
                1, 0, 0
        ));
        assertTrue(BarrelFrontVisibility.cameraIsInFront(
                4.0D, 4.5D, 3.5D,
                3.5D, 4.5D, 3.5D,
                1, 0, 0
        ));
    }

    @Test
    void rejectsCameraBehindDisplayPlaneInEveryAxis() {
        assertFalse(BarrelFrontVisibility.cameraIsInFront(
                3.9D, 4.5D, 3.5D,
                3.5D, 4.5D, 3.5D,
                1, 0, 0
        ));
        assertFalse(BarrelFrontVisibility.cameraIsInFront(
                3.5D, 4.5D, 3.5D,
                3.5D, 4.5D, 3.5D,
                0, 1, 0
        ));
        assertFalse(BarrelFrontVisibility.cameraIsInFront(
                3.5D, 4.5D, 4.1D,
                3.5D, 4.5D, 3.5D,
                0, 0, -1
        ));
    }
}
