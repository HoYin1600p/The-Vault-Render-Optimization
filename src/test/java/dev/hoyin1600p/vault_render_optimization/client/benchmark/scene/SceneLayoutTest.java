package dev.hoyin1600p.vault_render_optimization.client.benchmark.scene;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SceneLayoutTest {
    @Test void fullCompositionHasFlatRowsAndDisjointBoundingBoxesWithinFieldOfView() {
        assertEquals(42, SceneLayout.MAX_DISTANCE);
        assertDisjointAndInView(SceneComposition.actors());
    }

    @Test void registeredWidthsDriveSpacingAndViewMargins() {
        var actors = new java.util.ArrayList<>(SceneComposition.actors());
        var vault = actors.get(3);
        assertTrue(vault.vaultMob());
        actors.set(3, new SceneComposition.Actor(vault.kind(), vault.variant(), vault.enchanted(), 2.4));
        assertDisjointAndInView(actors);
    }

    @Test void finalCapacityAllowsTheFullRecipeAndTwentyFourMoreConservativeMobs() {
        var actors = new java.util.ArrayList<>(SceneComposition.actors());
        for (int i = 0; i < 24; i++) actors.add(new SceneComposition.Actor(SceneComposition.Kind.MOB, 181 + i * 2, false));
        assertEquals(324, SceneLayout.positions(actors, 0, 64, 0, 0, 6).size());
        actors.add(new SceneComposition.Actor(SceneComposition.Kind.MOB, 229, false));
        assertThrows(IllegalArgumentException.class, () -> SceneLayout.positions(actors, 0, 64, 0, 0, 6));
    }

    private void assertDisjointAndInView(java.util.List<SceneComposition.Actor> actors) {
        for (float yaw : new float[] {0, 90, -90, 180, 37, 45, 721}) {
            var positions = SceneLayout.positions(actors, 103, 64, -87, yaw, SceneLayout.MIN_DEPTH);
            assertEquals(actors.size(), positions.size());
            assertEquals(positions, SceneLayout.positions(actors, 103, 64, -87, yaw, SceneLayout.MIN_DEPTH));
            double radians = Math.toRadians(yaw);
            for (int i = 0; i < actors.size(); i++) {
                var at = positions.get(i);
                double x = at.x() - 103, z = at.z() + 87;
                double depth = -Math.sin(radians) * x + Math.cos(radians) * z;
                assertTrue(depth >= 6 - 1e-9 && depth <= SceneLayout.MAX_DISTANCE + 1e-9);
                assertEquals(64, at.y());
                double half = actors.get(i).width() / 2;
                for (double dx : new double[] {-half, half}) {
                    for (double dz : new double[] {-half, half}) {
                        double cornerDepth = -Math.sin(radians) * (x + dx) + Math.cos(radians) * (z + dz);
                        double cornerSide = Math.cos(radians) * (x + dx) + Math.sin(radians) * (z + dz);
                        assertTrue(Math.abs(Math.toDegrees(Math.atan2(cornerSide, cornerDepth))) <= 35 + 1e-9);
                    }
                }
                for (int j = 0; j < i; j++) {
                    var other = positions.get(j);
                    double distance = Math.hypot(at.x() - other.x(), at.z() - other.z());
                    assertTrue(distance + 1e-9 >= Math.max(1.6, Math.max(actors.get(i).width(), actors.get(j).width()) + 0.6));
                    double combinedHalf = (actors.get(i).width() + actors.get(j).width()) / 2;
                    assertTrue(Math.abs(at.x() - other.x()) + 1e-9 >= combinedHalf
                            || Math.abs(at.z() - other.z()) + 1e-9 >= combinedHalf, "Actor bounding boxes overlap");
                }
            }
        }
    }

    @Test void emptyAndInvalidRequests() {
        assertTrue(SceneLayout.positions(0, 0, 0, 0, 0).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> SceneLayout.positions(-1, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> SceneLayout.positions(10_000, 0, 0, 0, 0));
    }
}
