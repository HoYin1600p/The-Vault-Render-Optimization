/*
 * Adapted from MoePus/Flerovium (LGPL-3.0-only), commit 512e0307 (2026-01-26):
 * Sound/SoundEngineMixin and Sound/ClientLevelMixin, adjusted for Forge 1.18.2 SoundEngine.play.
 */
package dev.hoyin1600p.vault_render_optimization.client.sound;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;

/**
 * Pure decision helpers for client sound culling:
 * <ul>
 *   <li>Skip non-streaming sounds when the OpenAL static channel pool is already saturated.</li>
 *   <li>Skip non-relative, non-looping, non-tickable, linearly attenuated sounds that are beyond
 *       audible range plus a movement/teleport margin of every active listener reference point
 *       (OpenAL listener, main camera, and player eye position).</li>
 * </ul>
 */
public final class SoundOptimization {
    /** Extra distance margin (blocks) to prevent muting edge sounds during fast motion or packet lag. */
    public static final double RANGE_SAFETY_MARGIN = 4.0;

    private SoundOptimization() {
    }

    public static boolean shouldCullSaturatedStaticChannel(
            boolean streaming, int usedChannels, int maxChannels
    ) {
        if (streaming || !ClientOptimizationConfig.optimizationsEnabled()
                || !ClientOptimizationConfig.soundChannelCulling) {
            return false;
        }
        return maxChannels > 0 && usedChannels >= maxChannels;
    }

    public static double distanceSquared(
            double ax, double ay, double az, double bx, double by, double bz
    ) {
        double dx = ax - bx;
        double dy = ay - by;
        double dz = az - bz;
        return dx * dx + dy * dy + dz * dz;
    }

    public static boolean shouldCullOutOfRange(
            boolean relative,
            boolean looping,
            boolean tickable,
            boolean linearAttenuation,
            boolean canStartSilent,
            float volume,
            int attenuationDistance,
            double minDistanceSquared
    ) {
        if (relative || looping || tickable || !linearAttenuation || canStartSilent
                || !ClientOptimizationConfig.optimizationsEnabled()
                || !ClientOptimizationConfig.soundDistanceCulling) {
            return false;
        }
        double range = Math.max(volume, 1.0F) * (double) attenuationDistance;
        if (range <= 0.0 || !Double.isFinite(range) || !Double.isFinite(minDistanceSquared)) {
            return false;
        }
        double paddedRange = range + RANGE_SAFETY_MARGIN;
        return minDistanceSquared > paddedRange * paddedRange;
    }
}
