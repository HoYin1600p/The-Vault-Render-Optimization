package dev.hoyin1600p.vault_render_optimization.client.benchmark.scene;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SceneHeightTest {
    @Test void highestTerrainIncludingGapsDeterminesOneFlatPlane() {
        var selected = SceneHeight.select(SceneComposition.actors(), 0, 64, 0, 0,
                (x, z) -> x == 0 && z == 15 ? 70 : 60);
        assertEquals(71, selected.planeY());
        assertEquals(6, selected.nearDepth());
        assertFalse(selected.capped());
        assertTrue(selected.positions().stream().allMatch(p -> p.y() == 71));
    }

    @Test void wallBetweenPlayerAndSceneLiftsThePlaneAboveIt() {
        // The wall stands in the corridor the camera looks through, so the scene must clear it to stay visible.
        var selected = SceneHeight.select(SceneComposition.actors(), 0, 64, 0, 0,
                (x, z) -> z >= 2 && z < 5 ? 100 : 60);
        assertEquals(101, selected.planeY());
        assertTrue(selected.capped());
        assertEquals(SceneComposition.TOTAL, selected.positions().size());
    }

    @Test void playerOnACliffKeepsTheSceneNearEyeLevel() {
        // Terrain ahead drops to sea level; the scene stays two blocks below the eyes instead of following it down.
        var selected = SceneHeight.select(SceneComposition.actors(), 0, 110.8, 0, 0,
                (x, z) -> z <= 1 ? 109 : 63);
        assertEquals(110, selected.planeY());
        assertEquals(6, selected.nearDepth());
        assertFalse(selected.capped());
    }

    @Test void capPreservesClearanceAndFarBoundWhenAllTerrainIsTooHigh() {
        var selected = SceneHeight.select(SceneComposition.actors(), 0, 64, 0, 0, (x, z) -> 100);
        assertTrue(selected.capped());
        assertTrue(selected.nearDepth() > 6);
        assertEquals(101, selected.planeY());
        assertTrue(selected.positions().stream().allMatch(p -> p.y() == 101 && p.z() <= SceneLayout.MAX_DISTANCE));
    }

    @Test void exactTwelveBlockLimitDoesNotMoveScene() {
        var selected = SceneHeight.select(SceneComposition.actors(), 0, 64, 0, 0, (x, z) -> 75);
        assertEquals(76, selected.planeY());
        assertEquals(6, selected.nearDepth());
        assertFalse(selected.capped());
    }
}
