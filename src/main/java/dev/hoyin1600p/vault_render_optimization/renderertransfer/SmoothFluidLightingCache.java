package dev.hoyin1600p.vault_render_optimization.renderertransfer;

import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.level.material.Fluid;

/** Thread-safe because chunk meshing may query the renderer from worker threads. */
public final class SmoothFluidLightingCache {
    private static final ConcurrentHashMap<Fluid, Boolean> NON_LUMINOUS = new ConcurrentHashMap<>();

    private SmoothFluidLightingCache() {
    }

    public static boolean usesSmoothLighting(Fluid fluid, boolean ambientOcclusion) {
        return ambientOcclusion && NON_LUMINOUS.computeIfAbsent(
                fluid,
                key -> key.getAttributes().getLuminosity() == 0
        );
    }

    public static void clear() {
        NON_LUMINOUS.clear();
    }

    static boolean usesSmoothLighting(boolean ambientOcclusion, int luminosity) {
        return ambientOcclusion && luminosity == 0;
    }
}
