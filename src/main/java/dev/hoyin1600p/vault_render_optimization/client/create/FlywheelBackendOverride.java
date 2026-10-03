package dev.hoyin1600p.vault_render_optimization.client.create;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;

/**
 * Session-only Flywheel instancing: while VRO optimizations are on, {@code auto_enable_flywheel_instancing}
 * is on and the pack stores Flywheel's backend as OFF, Flywheel's backend setting reads as INSTANCING.
 * Nothing is written to Flywheel's config; Flywheel's own startup and shader checks still decide
 * whether instancing can actually run.
 */
public final class FlywheelBackendOverride {
    // Only set when VRO also runs FlywheelBackendManager (Create with Flywheel 0.6.11), which refreshes
    // Flywheel whenever the decision changes.
    private static volatile boolean managed;

    private FlywheelBackendOverride() {
    }

    public static void enableManagement() {
        managed = true;
    }

    /** Whether the override applies now, given whether Flywheel's stored backend is OFF. */
    public static boolean active(boolean storedOff) {
        return managed && shouldOverride(storedOff, ClientOptimizationConfig.optimizationsEnabled(),
                ClientOptimizationConfig.createFlywheelAutoEnable);
    }

    static boolean shouldOverride(boolean storedOff, boolean optimizationsEnabled, boolean autoEnable) {
        return storedOff && optimizationsEnabled && autoEnable;
    }

    /** The backend Flywheel should see: {@code instancing} for a stored {@code off} when overriding, else stored. */
    static <T> T effective(T stored, T off, T instancing, boolean optimizationsEnabled, boolean autoEnable) {
        return shouldOverride(stored == off, optimizationsEnabled, autoEnable) ? instancing : stored;
    }
}
