package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import net.minecraftforge.fml.ModList;

/** True when an Oculus shader pack is active; any doubt counts as active (the GPU path then stays off). */
final class OculusShaderPackProbe {
    private static final boolean OCULUS = isLoaded();

    private OculusShaderPackProbe() {
    }

    private static boolean isLoaded() {
        try {
            ModList mods = ModList.get();
            return mods == null || mods.isLoaded("oculus");
        } catch (Throwable failure) {
            return true;
        }
    }

    static boolean shaderPackActive() {
        if (!OCULUS) return false;
        try {
            return Holder.inUse();
        } catch (Throwable failure) {
            return true;
        }
    }

    /** Loaded only when Oculus is installed. */
    private static final class Holder {
        static boolean inUse() {
            return net.irisshaders.iris.api.v0.IrisApi.getInstance().isShaderPackInUse();
        }
    }
}
