package dev.hoyin1600p.vault_render_optimization.client.benchmark.scene;

import java.util.List;
import java.util.function.IntBinaryOperator;

/** Pure terrain selection. Height function returns the surface height (leaves excluded by the caller).
 * The plane is one block above the highest terrain between the camera and the scene's far edge, and never more
 * than two blocks below the eyes. If the plane exceeds eye height + 12, repack two blocks farther ahead and resample.
 * Stop when it fits or moving another row would exceed the 42-block far bound/capacity.
 * At that cap, terrain clearance wins: the flat plane may still exceed eye height + 12. */
public final class SceneHeight {
    public record Selection(List<SceneLayout.Position> positions, double planeY, double nearDepth, boolean capped) { }
    private SceneHeight() { }

    public static Selection select(List<SceneComposition.Actor> actors, double cameraX, double eyeY,
                                   double cameraZ, float yaw, IntBinaryOperator height) {
        Selection last = null;
        for (double near = SceneLayout.MIN_DEPTH; near <= SceneLayout.MAX_DISTANCE; near += SceneLayout.ROW_SPACING) {
            List<SceneLayout.Position> positions;
            try {
                positions = SceneLayout.positions(actors, cameraX, 0, cameraZ, yaw, near);
            } catch (IllegalArgumentException capacity) {
                if (last == null) throw capacity;
                break;
            }
            // Terrain between the player and the scene counts too (it would block the view), and the plane never
            // sits more than two blocks below the eyes, so the scene stays at about eye level.
            double plane = Math.max(highest(actors, positions, cameraX, cameraZ, height) + 1, Math.floor(eyeY - 2));
            List<SceneLayout.Position> flat = positions.stream()
                    .map(p -> new SceneLayout.Position(p.x(), plane, p.z(), p.yaw())).toList();
            last = new Selection(flat, plane, near, false);
            if (plane <= eyeY + 12) return last;
        }
        return new Selection(last.positions(), last.planeY(), last.nearDepth(), true);
    }

    private static double highest(List<SceneComposition.Actor> actors, List<SceneLayout.Position> positions,
                                  double cameraX, double cameraZ, IntBinaryOperator height) {
        if (actors.isEmpty()) throw new IllegalArgumentException("Empty scene footprint");
        // The box starts at the camera, so it covers the corridor between the player and the scene.
        double minX = cameraX, maxX = cameraX;
        double minZ = cameraZ, maxZ = cameraZ;
        for (int i = 0; i < actors.size(); i++) {
            var p = positions.get(i);
            double half = actors.get(i).width() / 2;
            minX = Math.min(minX, p.x() - half); maxX = Math.max(maxX, p.x() + half);
            minZ = Math.min(minZ, p.z() - half); maxZ = Math.max(maxZ, p.z() + half);
        }
        int highest = Integer.MIN_VALUE;
        // Include all columns across the footprint, including the gaps between actors.
        for (int x = (int) Math.floor(minX); x <= (int) Math.floor(maxX); x++) {
            for (int z = (int) Math.floor(minZ); z <= (int) Math.floor(maxZ); z++) {
                highest = Math.max(highest, height.applyAsInt(x, z));
            }
        }
        return highest;
    }
}
