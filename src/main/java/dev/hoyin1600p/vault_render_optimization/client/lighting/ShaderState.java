package dev.hoyin1600p.vault_render_optimization.client.lighting;

import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.Optional;

final class ShaderState {
    private static Method currentPackMethod;
    private static boolean resolved;
    private static boolean failureReported;

    private ShaderState() {
    }

    static boolean shadersActive() {
        if (!ModList.get().isLoaded("oculus")) {
            return false;
        }

        try {
            if (!resolved) {
                Class<?> iris = Class.forName("net.coderbot.iris.Iris");
                currentPackMethod = iris.getMethod("getCurrentPack");
                resolved = true;
            }
            Object result = currentPackMethod.invoke(null);
            return result instanceof Optional<?> optional && optional.isPresent();
        } catch (ReflectiveOperationException | LinkageError exception) {
            if (!failureReported) {
                failureReported = true;
                VaultRenderOptimization.LOGGER.warn(
                        "Could not query Oculus shader state; VRO dynamic lights will use the shader-off policy",
                        exception
                );
            }
            return false;
        }
    }
}
