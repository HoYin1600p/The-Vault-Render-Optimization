package dev.hoyin1600p.vault_render_optimization.config;

import dev.hoyin1600p.vault_render_optimization.backport.RenderBackportFeature;
import dev.hoyin1600p.vault_render_optimization.renderertransfer.RendererTransferFeature;
import dev.hoyin1600p.vault_render_optimization.util.ConfigKeys;
import dev.hoyin1600p.vault_render_optimization.util.ConfigSections;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The one list of player-facing settings: which settings-screen category each value belongs to,
 * where it is stored, whether a change needs a restart, and how it applies live. The settings
 * screen, Default and Experimental all read it; tests check it covers every config value once.
 */
public final class ConfigSettingCatalog {
    /** Settings-screen tabs, in display order. Not the .toml sections. */
    public enum Category {
        GPU("gpu"),
        CHUNKS("chunks"),
        ENTITIES("entities"),
        INTERFACE("interface"),
        COMPAT("compat"),
        IMMEDIATELY_FAST("immediatelyfast"),
        UPDATES("updates"),
        DIAGNOSTICS("diagnostics");

        private final String id;

        Category(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public String translationKey() {
            return "vro.config.category." + id;
        }
    }

    /** Where a value lives: the Forge client .toml, or the built-in ImmediatelyFast JSON. */
    public enum Storage {
        CLIENT_TOML,
        IMMEDIATELY_FAST_JSON
    }

    /**
     * One setting. {@code path} is the .toml path, or the single JSON field name for ImmediatelyFast.
     * {@code liveSetter}, when present, is the existing setter whose side effects (cache clears,
     * renderer reloads) must run on a change; every other value applies through the config bake.
     */
    public record Setting(
            String id,
            Category category,
            Storage storage,
            List<String> path,
            boolean restartRequired,
            Consumer<Object> liveSetter
    ) {
        public boolean diagnostics() {
            return category == Category.DIAGNOSTICS;
        }

        public String labelKey() {
            return "vro.config." + category.id() + "." + id;
        }

        public String summaryKey() {
            return labelKey() + ".summary";
        }

        public String tooltipKey() {
            return labelKey() + ".tooltip";
        }
    }

    /**
     * ImmediatelyFast JSON options deliberately left off the screen: VRO's built-in copy never reads
     * this one. Default and Experimental leave it as it is in the file.
     */
    public static final Set<String> UNLISTED_IMMEDIATELY_FAST_OPTIONS =
            Set.of("debug_only_and_not_recommended_disable_mod_conflict_handling");

    /**
     * Default-off settings the Experimental button never turns on, by setting id. Horizontal section
     * culling is not an experiment but a deliberate trade: it shortens the sideways draw distance.
     */
    public static final Set<String> EXPERIMENTAL_EXCLUSIONS = Set.of(ConfigKeys.HORIZONTAL_ENABLED,
            ConfigKeys.GPU_ENTITY_MODELS, ConfigKeys.GPU_ITEMS, ConfigKeys.GPU_PARTICLES,
            ConfigKeys.GPU_ENTITY_MODELS_WITH_SHADERS, ConfigKeys.GPU_PARTICLES_WITH_SHADERS);

    private static final List<Setting> SETTINGS = build();

    private ConfigSettingCatalog() {
    }

    public static List<Setting> all() {
        return SETTINGS;
    }

    public static List<Setting> in(Category category) {
        return SETTINGS.stream().filter(setting -> setting.category() == category).toList();
    }

    private static List<Setting> build() {
        List<Setting> settings = new ArrayList<>();

        // GPU rendering: all read every frame, so changes apply live.
        toml(settings, Category.GPU, ConfigKeys.GPU_ENTITY_MODELS, ConfigSections.RENDER_FAST_PATHS, false);
        toml(settings, Category.GPU, ConfigKeys.GPU_PARTICLES, ConfigSections.RENDER_FAST_PATHS, false);
        toml(settings, Category.GPU, ConfigKeys.GPU_ITEMS, ConfigSections.RENDER_FAST_PATHS, false);
        toml(settings, Category.GPU, ConfigKeys.GPU_ENTITY_MODELS_WITH_SHADERS, ConfigSections.RENDER_FAST_PATHS, false);
        toml(settings, Category.GPU, ConfigKeys.GPU_PARTICLES_WITH_SHADERS, ConfigSections.RENDER_FAST_PATHS, false);

        // Chunks & terrain.
        toml(settings, Category.CHUNKS, "defer_updates", ConfigSections.CHUNK_UPDATES, false);
        toml(settings, Category.CHUNKS, "index_only_sorting", ConfigSections.CHUNK_UPDATES, false);
        toml(settings, Category.CHUNKS, "sort_geometry_cache", ConfigSections.CHUNK_UPDATES, false);
        toml(settings, Category.CHUNKS, "adaptive_budget_v2", ConfigSections.CHUNK_UPDATES, false);
        toml(settings, Category.CHUNKS, "vertical_enabled", ConfigSections.SECTION_DISTANCE_CULLING, false);
        toml(settings, Category.CHUNKS, "vertical_distance", ConfigSections.SECTION_DISTANCE_CULLING, false);
        toml(settings, Category.CHUNKS, ConfigKeys.HORIZONTAL_ENABLED, ConfigSections.SECTION_DISTANCE_CULLING, false);
        toml(settings, Category.CHUNKS, "horizontal_distance", ConfigSections.SECTION_DISTANCE_CULLING, false);

        // Entities & particles.
        toml(settings, Category.ENTITIES, ConfigKeys.PARTICLE_COLLISION_CACHE, ConfigSections.RENDER_FAST_PATHS, false);
        toml(settings, Category.ENTITIES, "particle_light_cache", ConfigSections.RENDER_FAST_PATHS, false);
        toml(settings, Category.ENTITIES, "particle_shared_light_cache", ConfigSections.RENDER_FAST_PATHS, false);
        toml(settings, Category.ENTITIES, "particle_billboard_fast_path", ConfigSections.RENDER_FAST_PATHS, false);
        toml(settings, Category.ENTITIES, "particle_billboard_owner", ConfigSections.RENDER_FAST_PATHS, false);
        toml(settings, Category.ENTITIES, "skip_empty_particle_render", ConfigSections.RENDER_FAST_PATHS, false);
        toml(settings, Category.ENTITIES, ConfigKeys.PARTICLE_TICK_COMPACTION, ConfigSections.RENDER_FAST_PATHS, false);
        toml(settings, Category.ENTITIES, ConfigKeys.PARTICLE_SHARED_RANDOM, ConfigSections.RENDER_FAST_PATHS, false);
        toml(settings, Category.ENTITIES, ConfigKeys.PARTICLE_PROVIDER_CACHE, ConfigSections.RENDER_FAST_PATHS, false);
        toml(settings, Category.ENTITIES, ConfigKeys.ALLOCATION_FREE_FRUSTUM, ConfigSections.RENDER_FAST_PATHS, false);
        toml(settings, Category.ENTITIES, "entity_renderer_cache", ConfigSections.RENDERER_LOOKUP_CACHES, false);
        toml(settings, Category.ENTITIES, "block_entity_renderer_cache", ConfigSections.RENDERER_LOOKUP_CACHES, false);

        // Interface & HUD.
        settings.add(new Setting(ConfigKeys.HUD_TEXT_GEOMETRY, Category.INTERFACE, Storage.CLIENT_TOML,
                List.of(ConfigSections.RENDER_FAST_PATHS, ConfigKeys.HUD_TEXT_GEOMETRY), false,
                value -> ClientOptimizationConfig.setHudTextGeometry((Boolean) value)));
        toml(settings, Category.INTERFACE, "skip_empty_toast_render", ConfigSections.RENDER_FAST_PATHS, false);
        toml(settings, Category.INTERFACE, "skip_empty_debug_render", ConfigSections.RENDER_FAST_PATHS, false);
        toml(settings, Category.INTERFACE, "skip_inactive_tutorial", ConfigSections.CLIENT_TICK_FAST_PATHS, false);

        // Mod compatibility: Farsight, Fastload, Create, Sophisticated Storage, ModernFix, Embeddium.
        toml(settings, Category.COMPAT, "farsight_chunk_bound", ConfigSections.CHUNK_UPDATES, false);
        toml(settings, Category.COMPAT, ConfigKeys.FASTLOAD_FRUSTUM_BYPASS, ConfigSections.RENDER_FAST_PATHS, false);
        toml(settings, Category.COMPAT, "create_skip_empty_contraption_buffer_flush",
                List.of(ConfigSections.CREATE_RENDERING, "skip_empty_contraption_buffer_flush"), false);
        toml(settings, Category.COMPAT, "create_contraption_block_entity_culling",
                List.of(ConfigSections.CREATE_RENDERING, "contraption_block_entity_culling"), false);
        toml(settings, Category.COMPAT, "create_contraption_actor_culling",
                List.of(ConfigSections.CREATE_RENDERING, "contraption_actor_culling"), false);
        // Flywheel builds sectioned meshes when a contraption renderer is created: rebuild them.
        settings.add(new Setting("create_sectioned_contraption_meshes", Category.COMPAT, Storage.CLIENT_TOML,
                List.of(ConfigSections.CREATE_RENDERING, "sectioned_contraption_meshes"), false,
                value -> ClientOptimizationConfig.reloadCreateRenderers()));
        settings.add(new Setting("create_sectioned_mesh_block_threshold", Category.COMPAT, Storage.CLIENT_TOML,
                List.of(ConfigSections.CREATE_RENDERING, "sectioned_mesh_block_threshold"), false,
                value -> ClientOptimizationConfig.reloadCreateRenderers()));
        toml(settings, Category.COMPAT, "create_smart_machinery_render_bounds",
                List.of(ConfigSections.CREATE_RENDERING, "smart_machinery_render_bounds"), false);
        // Session-only, read live: FlywheelBackendManager refreshes Flywheel when the decision changes.
        toml(settings, Category.COMPAT, "create_auto_enable_flywheel_instancing",
                List.of(ConfigSections.CREATE_RENDERING, "auto_enable_flywheel_instancing"), false);
        settings.add(new Setting("create_flywheel_shader_compat", Category.COMPAT, Storage.CLIENT_TOML,
                List.of(ConfigSections.CREATE_RENDERING, "flywheel_shader_compat"), false,
                value -> ClientOptimizationConfig.setCreateFlywheelShaderCompat((Boolean) value)));
        toml(settings, Category.COMPAT, "storage_front_display_culling",
                List.of(ConfigSections.SOPHISTICATED_STORAGE, "front_display_culling"), false);
        toml(settings, Category.COMPAT, "storage_quantity_text_cache",
                List.of(ConfigSections.SOPHISTICATED_STORAGE, "quantity_text_cache"), false);
        toml(settings, Category.COMPAT, "storage_fill_level_fast_path",
                List.of(ConfigSections.SOPHISTICATED_STORAGE, "fill_level_fast_path"), false);
        toml(settings, Category.COMPAT, "storage_count_only_render_update_filter",
                List.of(ConfigSections.SOPHISTICATED_STORAGE, "count_only_render_update_filter"), false);
        // Backports and transfers choose their mixins at startup.
        for (RenderBackportFeature feature : RenderBackportFeature.values()) {
            toml(settings, Category.COMPAT, "backport_" + feature.id(),
                    List.of(ConfigSections.MODERNFIX_BACKPORTS, feature.configKey()), true);
        }
        for (RendererTransferFeature feature : RendererTransferFeature.values()) {
            toml(settings, Category.COMPAT, "transfer_" + snakeCase(feature.configKey()),
                    List.of(ConfigSections.EMBEDDIUM_TRANSFERS, feature.configKey()), true);
        }
        // Tunables of the transfers above. The buffer values are read on every buffer start; the arena
        // values are captured per arena on its first growth, so they apply after a world rejoin.
        toml(settings, Category.COMPAT, "vertex_buffer_max_retained_mib",
                List.of(ConfigSections.EMBEDDIUM_TRANSFERS, "vertexBufferMaxRetainedMib"), false);
        toml(settings, Category.COMPAT, "vertex_buffer_adaptive_trimming",
                List.of(ConfigSections.EMBEDDIUM_TRANSFERS, "vertexBufferAdaptiveTrimming"), false);
        toml(settings, Category.COMPAT, "vertex_buffer_aggregate_retained_mib",
                List.of(ConfigSections.EMBEDDIUM_TRANSFERS, "vertexBufferAggregateRetainedMib"), false);
        toml(settings, Category.COMPAT, "async_arena_growth_divisor",
                List.of(ConfigSections.EMBEDDIUM_TRANSFERS, ConfigKeys.ASYNC_ARENA_GROWTH_DIVISOR), true);
        toml(settings, Category.COMPAT, "async_arena_max_headroom_mib",
                List.of(ConfigSections.EMBEDDIUM_TRANSFERS, ConfigKeys.ASYNC_ARENA_MAX_HEADROOM_MIB), true);

        // Built-in ImmediatelyFast: the JSON is read once at startup for mixin selection. Its cosmetic
        // and debug-only options sit in Diagnostics so Experimental never turns them on.
        json(settings, Category.IMMEDIATELY_FAST, "enabled", "enabled");
        json(settings, Category.IMMEDIATELY_FAST, "font_atlas_resizing", "font_atlas_resizing");
        json(settings, Category.IMMEDIATELY_FAST, "map_atlas_generation", "map_atlas_generation");
        json(settings, Category.IMMEDIATELY_FAST, "hud_batching", "hud_batching");
        json(settings, Category.IMMEDIATELY_FAST, "fast_text_lookup", "fast_text_lookup");
        json(settings, Category.IMMEDIATELY_FAST, "fast_buffer_upload", "fast_buffer_upload");
        json(settings, Category.IMMEDIATELY_FAST, "experimental_item_hud_batching",
                "experimental_item_hud_batching");

        // Updates.
        toml(settings, Category.UPDATES, "check_for_updates", ConfigSections.UPDATES, false);
        toml(settings, Category.UPDATES, "update_types", ConfigSections.UPDATES, false);

        // Diagnostics: never touched by Experimental. Compare Mode switches runtime paths at once, but
        // the backports and transfers read it from the launch snapshot, so it carries the badge.
        settings.add(new Setting(ConfigKeys.COMPARE_MODE, Category.DIAGNOSTICS, Storage.CLIENT_TOML,
                List.of(ConfigSections.BENCHMARK, ConfigKeys.COMPARE_MODE), true,
                value -> ClientOptimizationConfig.setCompareMode((Boolean) value)));
        toml(settings, Category.DIAGNOSTICS, "particle_diagnostics", ConfigSections.RENDER_FAST_PATHS, false);
        toml(settings, Category.DIAGNOSTICS, "storage_diagnostics",
                List.of(ConfigSections.SOPHISTICATED_STORAGE, "diagnostics"), false);
        json(settings, Category.DIAGNOSTICS, "immediatelyfast_dont_add_info_into_debug_hud",
                "dont_add_info_into_debug_hud");
        json(settings, Category.DIAGNOSTICS, "immediatelyfast_disable_universal_batching",
                "debug_only_and_not_recommended_disable_universal_batching");
        return Collections.unmodifiableList(settings);
    }

    private static void toml(List<Setting> settings, Category category, String key, String section,
                             boolean restartRequired) {
        toml(settings, category, key, List.of(section, key), restartRequired);
    }

    private static void toml(List<Setting> settings, Category category, String id, List<String> path,
                             boolean restartRequired) {
        settings.add(new Setting(id, category, Storage.CLIENT_TOML, path, restartRequired, null));
    }

    private static void json(List<Setting> settings, Category category, String id, String field) {
        settings.add(new Setting(id, category, Storage.IMMEDIATELY_FAST_JSON, List.of(field), true, null));
    }

    static String snakeCase(String camelCase) {
        StringBuilder out = new StringBuilder();
        for (char c : camelCase.toCharArray()) {
            if (Character.isUpperCase(c)) {
                out.append('_').append(Character.toLowerCase(c));
            } else {
                out.append(c);
            }
        }
        return out.toString().toLowerCase(Locale.ROOT);
    }
}
