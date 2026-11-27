package dev.hoyin1600p.vault_render_optimization.client.render;

import dev.hoyin1600p.vault_render_optimization.util.ModIds;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;

/**
 * Low-overhead probe for Oculus's active shadow pass ({@code ShadowRenderer.ACTIVE}).
 * Orthographic shadow passes must not use perspective angle-based chunk occlusion.
 */
public final class OculusShadowState {
    private static volatile boolean initialized;
    private static MethodHandle shadowActiveGetter;

    private OculusShadowState() {
    }

    public static boolean isShadowPass() {
        MethodHandle getter = resolveGetter();
        if (getter == null) {
            return false;
        }
        try {
            return (boolean) getter.invokeExact();
        } catch (Throwable failure) {
            shadowActiveGetter = null;
            return false;
        }
    }

    private static MethodHandle resolveGetter() {
        if (initialized) {
            return shadowActiveGetter;
        }
        synchronized (OculusShadowState.class) {
            if (initialized) {
                return shadowActiveGetter;
            }
            LoadingModList modList = FMLLoader.getLoadingModList();
            if (modList != null && modList.getModFileById(ModIds.OCULUS) != null) {
                try {
                    Class<?> shadowRenderer = Class.forName(
                            "net.coderbot.iris.pipeline.ShadowRenderer",
                            false,
                            OculusShadowState.class.getClassLoader()
                    );
                    shadowActiveGetter = MethodHandles.lookup()
                            .findStaticGetter(shadowRenderer, "ACTIVE", boolean.class);
                } catch (Throwable ignored) {
                    shadowActiveGetter = null;
                }
            }
            initialized = true;
            return shadowActiveGetter;
        }
    }
}
