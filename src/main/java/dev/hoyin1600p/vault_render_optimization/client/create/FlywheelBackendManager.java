package dev.hoyin1600p.vault_render_optimization.client.create;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.config.BackendType;
import com.jozufozu.flywheel.config.FlwConfig;
import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;

/**
 * Keeps Flywheel in step with {@link FlywheelBackendOverride}. The override itself is a read-time
 * answer from {@code FlwConfig.getBackendType}; this only refreshes Flywheel and reloads world
 * renderers when the decision changes (the VRO option, Compare Mode, or Flywheel's stored value),
 * so turning the option off returns to the pack's setting at once. It never writes Flywheel's config.
 */
public final class FlywheelBackendManager {
    // The override state Flywheel was last refreshed with; null until the first in-world check.
    private static Boolean applied;

    private FlywheelBackendManager() {
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().level == null) {
            return;
        }
        // The raw stored value: EnumValue.get() does not pass through the getBackendType override.
        boolean storedOff = FlwConfig.get().client.backend.get() == BackendType.OFF;
        boolean desired = FlywheelBackendOverride.active(storedOff);
        boolean first = applied == null;
        if (!first && applied == desired) {
            return;
        }
        applied = desired;
        // Flywheel's own startup refresh already read the override; only correct a mismatch once.
        if (first && (!storedOff || desired == Backend.isOn())) {
            return;
        }
        try {
            Backend.refresh();
            Backend.reloadWorldRenderers();
            VaultRenderOptimization.LOGGER.info(
                    desired ? "Flywheel instancing turned on for this session (the pack's setting is OFF); now {}"
                            : "Flywheel returned to the pack's backend setting; now {}",
                    Backend.getBackendType());
        } catch (RuntimeException | LinkageError exception) {
            VaultRenderOptimization.LOGGER.warn("Could not refresh Flywheel after a backend change", exception);
        }
    }

    /** True while VRO's session-only override is turning Flywheel's stored OFF into instancing. */
    public static boolean promotedBackend() {
        return Boolean.TRUE.equals(applied);
    }
}
