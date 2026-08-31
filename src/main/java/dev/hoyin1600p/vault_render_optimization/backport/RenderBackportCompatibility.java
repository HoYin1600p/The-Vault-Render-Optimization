package dev.hoyin1600p.vault_render_optimization.backport;

public final class RenderBackportCompatibility {
    private RenderBackportCompatibility() {
    }

    public static String blocker(
            RenderBackportFeature feature,
            boolean modDiscoveryFailed,
            boolean fluidloggedLoaded,
            boolean isometricRendersLoaded,
            boolean witherStormModLoaded,
            boolean rubidiumLoaded,
            boolean embeddiumLoaded,
            boolean ctmCompatible
    ) {
        if (modDiscoveryFailed) {
            return "loaded-mod discovery failed; ownership cannot be verified safely";
        }
        if (feature == RenderBackportFeature.CHUNK_MESHING && fluidloggedLoaded) {
            return "Fluidlogged changes the chunk meshing state lookup path";
        }
        if (feature == RenderBackportFeature.BUFFER_BUILDER_LEAK_FIX
                && (isometricRendersLoaded || witherStormModLoaded)) {
            return "an upstream-incompatible render mod is installed"
                    + " (Isometric Renders or Cracker's Wither Storm Mod)";
        }
        if (feature == RenderBackportFeature.CTM_METADATA_CACHE_CONCURRENCY
                && !ctmCompatible) {
            return "the validated ConnectedTexturesMod version is not installed";
        }
        if (feature == RenderBackportFeature.MODEL_DATA_MANAGER_CONCURRENCY
                && rubidiumLoaded
                && !embeddiumLoaded) {
            return "legacy Rubidium refreshes Forge model data only on worker threads";
        }
        return null;
    }
}
