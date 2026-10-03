package dev.hoyin1600p.vault_render_optimization.client.particle;

import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import java.util.concurrent.atomic.LongAdder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Ownership, counters and the verification switch for exact cached particle collision. */
public final class ParticleCollisionState {
    private static boolean compatible;
    private static String reason = "discovery pending";
    private static volatile boolean verify;
    private static final LongAdder cached = new LongAdder();
    private static final LongAdder cachedFree = new LongAdder();
    private static final LongAdder vanilla = new LongAdder();
    private static final LongAdder verified = new LongAdder();
    private static final LongAdder mismatches = new LongAdder();
    private static int mismatchesLogged;

    private ParticleCollisionState() { }

    public static void configure(boolean available, String detail) { compatible = available; reason = detail; }

    public static boolean enabled() {
        return compatible && ClientOptimizationConfig.particleCollisionCache
                && ClientOptimizationConfig.optimizationsEnabled();
    }

    public static boolean verifying() { return verify; }

    public static void setVerify(boolean enabled) {
        verify = enabled;
        if (enabled) {
            verified.reset();
            mismatches.reset();
            mismatchesLogged = 0;
        }
    }

    static boolean countersEnabled() { return verify || ParticleDiagnostics.enabled(); }

    public static void recordCached(boolean noShapes) {
        if (!countersEnabled()) return;
        cached.increment();
        if (noShapes) cachedFree.increment();
    }

    public static void recordVanilla() {
        if (countersEnabled()) vanilla.increment();
    }

    public static void recordVerification(boolean equal, AABB box, Vec3 motion, Vec3 expected, Vec3 actual) {
        verified.increment();
        if (equal) return;
        mismatches.increment();
        if (mismatchesLogged < 10) {
            mismatchesLogged++;
            VaultRenderOptimization.LOGGER.warn(
                    "Particle collision cache mismatch: box {} motion {} vanilla {} cached {}",
                    box, motion, expected, actual);
        }
    }

    public static void resetCounters() {
        cached.reset();
        cachedFree.reset();
        vanilla.reset();
        verified.reset();
        mismatches.reset();
        mismatchesLogged = 0;
    }

    public static String status() {
        return (compatible ? enabled() ? "APPLIED" : "YIELDED" : "BLOCKED") + ": " + reason
                + "; cached " + cached.sum() + " (no shapes " + cachedFree.sum() + "), vanilla " + vanilla.sum()
                + (verify || verified.sum() > 0
                        ? "; verify " + (verify ? "ON" : "OFF") + ": checked " + verified.sum()
                                + ", mismatches " + mismatches.sum()
                        : "")
                + " (counters need particle diagnostics or verify)";
    }
}
