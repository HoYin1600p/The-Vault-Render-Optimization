package dev.hoyin1600p.vault_render_optimization.client.create;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.config.BackendType;
import com.jozufozu.flywheel.config.FlwConfig;
import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;

public final class FlywheelBackendManager {
    private static boolean checked;
    private static boolean promoted;

    private FlywheelBackendManager() {
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || checked) {
            return;
        }
        if (!ClientOptimizationConfig.optimizationsEnabled()
                || !ClientOptimizationConfig.createFlywheelAutoEnable
                || Minecraft.getInstance().level == null) {
            return;
        }

        if (FlwConfig.get().getBackendType() != BackendType.OFF) {
            checked = true;
            return;
        }

        checked = true;
        try {
            FlwConfig.get().client.backend.set(BackendType.INSTANCING);
            FlwConfig.get().client.backend.save();
            Backend.refresh();
            Backend.reloadWorldRenderers();
            promoted = Backend.getBackendType() == BackendType.INSTANCING;
            if (promoted) {
                VaultRenderOptimization.LOGGER.info(
                        "Restored Flywheel instancing because the configured backend was OFF"
                );
            } else {
                VaultRenderOptimization.LOGGER.warn(
                        "Requested Flywheel instancing, but Flywheel retained the {} backend; using its safe fallback",
                        Backend.getBackendType()
                );
            }
        } catch (RuntimeException | LinkageError exception) {
            promoted = false;
            VaultRenderOptimization.LOGGER.warn(
                    "Could not enable Flywheel instancing automatically; preserving Flywheel's fallback renderer",
                    exception
            );
        }
    }

    public static boolean promotedBackend() {
        return promoted;
    }
}
