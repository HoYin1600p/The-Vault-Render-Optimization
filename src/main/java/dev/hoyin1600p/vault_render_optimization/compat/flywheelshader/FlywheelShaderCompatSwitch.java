package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader;

import java.util.function.BooleanSupplier;

/**
 * Whether the Flywheel shader compatibility is switched on. The mod registers the live
 * configuration value during construction, so this package does not depend on the config package.
 * Kept free of Flywheel and Oculus types so registering it never loads them.
 */
public final class FlywheelShaderCompatSwitch {
    private static volatile BooleanSupplier enabled = () -> false;

    private FlywheelShaderCompatSwitch() {
    }

    public static void register(BooleanSupplier source) {
        enabled = source;
    }

    static boolean isEnabled() {
        return enabled.getAsBoolean();
    }
}
