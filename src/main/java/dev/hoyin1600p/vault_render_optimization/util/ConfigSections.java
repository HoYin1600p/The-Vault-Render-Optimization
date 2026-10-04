package dev.hoyin1600p.vault_render_optimization.util;

/** The top-level section names of the client config file. */
public final class ConfigSections {
    public static final String UPDATES = "updates";
    public static final String BENCHMARK = "benchmark";
    public static final String CHUNK_UPDATES = "chunk_updates";
    public static final String MODERNFIX_BACKPORTS = "modernfix_backports";
    public static final String EMBEDDIUM_TRANSFERS = "embeddium_transfers";
    public static final String RENDER_FAST_PATHS = "render_fast_paths";
    public static final String CLIENT_TICK_FAST_PATHS = "client_tick_fast_paths";
    public static final String RENDERER_LOOKUP_CACHES = "renderer_lookup_caches";
    public static final String SOPHISTICATED_STORAGE = "sophisticated_storage";
    public static final String SECTION_DISTANCE_CULLING = "section_distance_culling";
    public static final String CREATE_RENDERING = "create_rendering";

    private ConfigSections() {
    }
}
