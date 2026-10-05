package dev.hoyin1600p.vault_render_optimization.client.benchmark.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Pure, flat camera-relative rows; yaw zero faces positive Z.
 * The composition is bounded to fit one 70-degree field of view without floating tiers. */
public final class SceneLayout {
    public static final double MIN_DEPTH = 6;
    public static final double MAX_DISTANCE = 42;
    public static final double HALF_FOV_DEGREES = 35;
    public static final double ROW_SPACING = 2;
    public record Position(double x, double y, double z, float yaw) { }

    private SceneLayout() { }

    public static List<Position> positions(int count, double cameraX, double planeY,
                                           double cameraZ, float cameraYaw) {
        if (count < 0 || count > SceneComposition.TOTAL) throw new IllegalArgumentException("Invalid actor count");
        return positions(SceneComposition.actors().subList(0, count), cameraX, planeY,
                cameraZ, cameraYaw, MIN_DEPTH);
    }

    public static List<Position> positions(List<SceneComposition.Actor> actors, double cameraX,
                                           double planeY, double cameraZ, float cameraYaw, double minDepth) {
        if (minDepth < MIN_DEPTH || minDepth > MAX_DISTANCE) throw new IllegalArgumentException("Invalid near row");
        if (actors.isEmpty()) return List.of();
        double radians = Math.toRadians(cameraYaw);
        double forwardX = -Math.sin(radians), forwardZ = Math.cos(radians);
        double rightX = Math.cos(radians), rightZ = Math.sin(radians);
        double halfFov = Math.toRadians(HALF_FOV_DEGREES);
        Random random = new Random(SceneComposition.SEED);
        List<Position> positions = new ArrayList<>(actors.size());
        double previousDepth = minDepth, previousWidth = 0;
        while (positions.size() < actors.size()) {
            int start = positions.size(), end = start;
            double span = 0, rowWidth = 0, depth = minDepth;
            // Pack in recipe order, using each adjacent pair's actual width rather than wasting
            // the small item/stand slots on a grid sized for the largest mob.
            while (end < actors.size()) {
                double nextWidth = Math.max(rowWidth, actors.get(end).width());
                double nextDepth = start == 0 ? minDepth
                        : previousDepth + Math.max(ROW_SPACING, separation(previousWidth, nextWidth));
                double nextSpan = span + (end == start ? 0
                        : separation(actors.get(end - 1).width(), actors.get(end).width()));
                // Protect every AABB corner at every camera yaw, including the near-side corners.
                double margin = Math.sqrt(2) * nextWidth / (2 * Math.cos(halfFov)) + 0.01;
                if (nextDepth > MAX_DISTANCE + 1e-9
                        || nextSpan / 2 + margin > nextDepth * Math.tan(halfFov)) break;
                span = nextSpan;
                rowWidth = nextWidth;
                depth = nextDepth;
                end++;
            }
            if (end == start) throw new IllegalArgumentException("Actor count exceeds flat row capacity");
            double side = -span / 2;
            for (int i = start; i < end; i++) {
                if (i > start) side += separation(actors.get(i - 1).width(), actors.get(i).width());
                positions.add(new Position(cameraX + forwardX * depth + rightX * side, planeY,
                        cameraZ + forwardZ * depth + rightZ * side,
                        cameraYaw + 180 + random.nextFloat() * 30 - 15));
            }
            previousDepth = depth;
            previousWidth = rowWidth;
        }
        return List.copyOf(positions);
    }

    private static double separation(double a, double b) {
        // sqrt(2) protects disjoint world-aligned boxes when the camera-relative rows rotate.
        return Math.max(1.6, Math.max(Math.max(a, b) + 0.6, Math.sqrt(2) * (a + b) / 2));
    }
}
