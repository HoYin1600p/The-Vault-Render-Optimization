/*
 * Angle-based section visibility path occlusion for Embeddium 0.3.18/0.3.19.
 * Concept informed by Sodium PR #2811 and MoePus/Flerovium OcclusionCullerMixin (LGPL-3.0-only),
 * adapted for Embeddium 0.3.18's ChunkGraphInfo visibilityData bit layout.
 */
package dev.hoyin1600p.vault_render_optimization.client.render;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;

/**
 * Masks opposing-face visibility paths through a 16x16x16 chunk section when the perspective
 * camera angle makes it geometrically impossible for a ray to enter one face and exit the
 * opposite face of that section.
 *
 * <p>Embeddium 0.3.18's {@code ChunkGraphInfo.calculateVisibilityData} encodes visibility from
 * {@code from} to {@code to} at bit {@code (from.ordinal() << 3) | to.ordinal()}, using vanilla
 * {@code Direction} ordinals: DOWN=0, UP=1, NORTH=2, SOUTH=3, WEST=4, EAST=5.
 */
public final class AngleSectionOcclusion {
    private static final int DOWN = 0;
    private static final int UP = 1;
    private static final int NORTH = 2;
    private static final int SOUTH = 3;
    private static final int WEST = 4;
    private static final int EAST = 5;

    public static final long UP_DOWN_OCCLUDED = pairMask(DOWN, UP);
    public static final long NORTH_SOUTH_OCCLUDED = pairMask(NORTH, SOUTH);
    public static final long WEST_EAST_OCCLUDED = pairMask(WEST, EAST);

    private static boolean active;
    private static double cameraX;
    private static double cameraY;
    private static double cameraZ;

    private AngleSectionOcclusion() {
    }

    private static long pairMask(int a, int b) {
        return (1L << ((a << 3) | b)) | (1L << ((b << 3) | a));
    }

    /** Begins a main-camera BFS pass on the render thread. Skipped in shadow passes, spectator or Compare Mode. */
    public static void beginPass(double camX, double camY, double camZ, boolean spectator) {
        if (spectator
                || !ClientOptimizationConfig.optimizationsEnabled()
                || !ClientOptimizationConfig.angleSectionOcclusion
                || OculusShadowState.isShadowPass()) {
            active = false;
            return;
        }
        cameraX = camX;
        cameraY = camY;
        cameraZ = camZ;
        active = true;
    }

    public static void endPass() {
        active = false;
    }

    public static long maskVisibility(long visibilityData, int originX, int originY, int originZ) {
        if (!active) {
            return visibilityData;
        }
        return visibilityData & computeAngleMask(cameraX, cameraY, cameraZ, originX, originY, originZ);
    }

    /**
     * Pure geometric mask: returns a bitmask with impossible opposing-face transitions cleared.
     * For a cube centered at {@code (origin + 8)} with half-extent 8, a ray from the camera can
     * cross opposing faces along axis A only if {@code |cam_A - center_A| >= |cam_B - center_B|}
     * for both other axes B.
     */
    public static long computeAngleMask(
            double camX, double camY, double camZ,
            int originX, int originY, int originZ
    ) {
        double dx = Math.abs(camX - (originX + 8.0));
        double dy = Math.abs(camY - (originY + 8.0));
        double dz = Math.abs(camZ - (originZ + 8.0));

        long occluded = 0L;
        if (dx > dy || dz > dy) {
            occluded |= UP_DOWN_OCCLUDED;
        }
        if (dx > dz || dy > dz) {
            occluded |= NORTH_SOUTH_OCCLUDED;
        }
        if (dy > dx || dz > dx) {
            occluded |= WEST_EAST_OCCLUDED;
        }
        return ~occluded;
    }
}
