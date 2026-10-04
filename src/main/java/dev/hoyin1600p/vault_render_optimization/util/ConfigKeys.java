package dev.hoyin1600p.vault_render_optimization.util;

/** Names of config values and files that more than one class refers to. */
public final class ConfigKeys {
    /** The client config file, relative to the Forge config directory. */
    public static final String CLIENT_CONFIG_FILE = "vault_render_optimization-client.toml";

    public static final String ALLOCATION_FREE_FRUSTUM = "allocation_free_frustum";
    public static final String ASYNC_ARENA_GROWTH_DIVISOR = "asyncArenaGrowthDivisor";
    public static final String ASYNC_ARENA_MAX_HEADROOM_MIB = "asyncArenaMaxHeadroomMib";
    public static final String COMPARE_MODE = "compare_mode";
    public static final String FASTLOAD_FRUSTUM_BYPASS = "fastload_frustum_bypass";
    public static final String GPU_ENTITY_MODELS = "gpu_entity_models";
    public static final String GPU_ENTITY_MODELS_WITH_SHADERS = "gpu_entity_models_with_shaders";
    public static final String GPU_ITEMS = "gpu_items";
    public static final String GPU_PARTICLES = "gpu_particles";
    public static final String GPU_PARTICLES_WITH_SHADERS = "gpu_particles_with_shaders";
    public static final String HORIZONTAL_ENABLED = "horizontal_enabled";
    public static final String HUD_TEXT_GEOMETRY = "hud_text_geometry";
    public static final String PARTICLE_COLLISION_CACHE = "particle_collision_cache";
    public static final String PARTICLE_PROVIDER_CACHE = "particle_provider_cache";
    public static final String PARTICLE_SHARED_RANDOM = "particle_shared_random";
    public static final String PARTICLE_TICK_COMPACTION = "particle_tick_compaction";

    private ConfigKeys() {
    }
}
