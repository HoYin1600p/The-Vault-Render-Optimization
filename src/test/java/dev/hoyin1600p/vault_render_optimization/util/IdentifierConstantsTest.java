package dev.hoyin1600p.vault_render_optimization.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/** The identifier strings are part of the config file, the commands and the mod ecosystem, so they must not drift. */
class IdentifierConstantsTest {
    private static Map<String, Object> constantsOf(Class<?> type) throws IllegalAccessException {
        Map<String, Object> constants = new LinkedHashMap<>();
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())
                    && !field.isSynthetic()) {
                constants.put(field.getName(), field.get(null));
            }
        }
        return constants;
    }

    private static void assertConstants(Class<?> type, Map<String, Object> expected) throws IllegalAccessException {
        Map<String, Object> actual = constantsOf(type);
        assertEquals(new TreeMap<>(expected), new TreeMap<>(actual), type.getSimpleName());
    }

    @Test
    void modIdsMatchTheRegisteredIds() throws IllegalAccessException {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("ASYNC_PARTICLES", "asyncparticles");
        expected.put("BAD_OPTIMIZATIONS", "badoptimizations");
        expected.put("BETTER_FPS_DIST", "betterfpsdist");
        expected.put("CLOTH_CONFIG", "cloth_config");
        expected.put("CODE_CHICKEN_LIB", "codechickenlib");
        expected.put("CREATE", "create");
        expected.put("CREATE_ADDITION", "createaddition");
        expected.put("EMBEDDIUM", "embeddium");
        expected.put("ENTITY_COLLISION_FPS_FIX", "entitycollisionfpsfix");
        expected.put("EXORDIUM", "exordium");
        expected.put("FARSIGHT_VIEW", "farsight_view");
        expected.put("FASTLOAD", "fastload");
        expected.put("FLEROVIUM", "flerovium");
        expected.put("FLUIDLOGGED", "fluidlogged");
        expected.put("FLYWHEEL", "flywheel");
        expected.put("ISOMETRIC_RENDERS", "isometric-renders");
        expected.put("ISPAWNER", "ispawner");
        expected.put("MODERN_UI", "modernui");
        expected.put("MODERNFIX", "modernfix");
        expected.put("OCULUS", "oculus");
        expected.put("PARTICLE_CORE", "particle_core");
        expected.put("POWAH", "powah");
        expected.put("RUBIDIUM", "rubidium");
        expected.put("SMOOTH_FONT", "smoothfont");
        expected.put("SODIUM", "sodium");
        expected.put("SOPHISTICATED_CORE", "sophisticatedcore");
        expected.put("SOPHISTICATED_STORAGE", "sophisticatedstorage");
        expected.put("THE_VAULT", "the_vault");
        expected.put("UNOBTAINIUM", "unobtainium");
        expected.put("VAULT_LOOT_BEAMS", "vaultlootbeams");
        expected.put("VH_ACCELERATOR", "vhaccelerator");
        expected.put("WITHER_STORM_MOD", "witherstormmod");
        expected.put("XAERO_WORLD_MAP", "xaeroworldmap");
        assertConstants(ModIds.class, expected);
    }

    @Test
    void configSectionsMatchTheGeneratedFile() throws IllegalAccessException {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("UPDATES", "updates");
        expected.put("BENCHMARK", "benchmark");
        expected.put("CHUNK_UPDATES", "chunk_updates");
        expected.put("MODERNFIX_BACKPORTS", "modernfix_backports");
        expected.put("EMBEDDIUM_TRANSFERS", "embeddium_transfers");
        expected.put("RENDER_FAST_PATHS", "render_fast_paths");
        expected.put("CLIENT_TICK_FAST_PATHS", "client_tick_fast_paths");
        expected.put("RENDERER_LOOKUP_CACHES", "renderer_lookup_caches");
        expected.put("SOPHISTICATED_STORAGE", "sophisticated_storage");
        expected.put("SECTION_DISTANCE_CULLING", "section_distance_culling");
        expected.put("CREATE_RENDERING", "create_rendering");
        assertConstants(ConfigSections.class, expected);
    }

    @Test
    void configKeysMatchTheGeneratedFile() throws IllegalAccessException {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("CLIENT_CONFIG_FILE", "vault_render_optimization-client.toml");
        expected.put("ALLOCATION_FREE_FRUSTUM", "allocation_free_frustum");
        expected.put("ASYNC_ARENA_GROWTH_DIVISOR", "asyncArenaGrowthDivisor");
        expected.put("ASYNC_ARENA_MAX_HEADROOM_MIB", "asyncArenaMaxHeadroomMib");
        expected.put("COMPARE_MODE", "compare_mode");
        expected.put("FASTLOAD_FRUSTUM_BYPASS", "fastload_frustum_bypass");
        expected.put("GPU_ENTITY_MODELS", "gpu_entity_models");
        expected.put("GPU_ENTITY_MODELS_WITH_SHADERS", "gpu_entity_models_with_shaders");
        expected.put("GPU_ITEMS", "gpu_items");
        expected.put("GPU_PARTICLES", "gpu_particles");
        expected.put("GPU_PARTICLES_WITH_SHADERS", "gpu_particles_with_shaders");
        expected.put("HORIZONTAL_ENABLED", "horizontal_enabled");
        expected.put("HUD_TEXT_GEOMETRY", "hud_text_geometry");
        expected.put("PARTICLE_COLLISION_CACHE", "particle_collision_cache");
        expected.put("PARTICLE_PROVIDER_CACHE", "particle_provider_cache");
        expected.put("PARTICLE_SHARED_RANDOM", "particle_shared_random");
        expected.put("PARTICLE_TICK_COMPACTION", "particle_tick_compaction");
        assertConstants(ConfigKeys.class, expected);
    }

    @Test
    void guiColoursKeepTheirValues() throws IllegalAccessException {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("WHITE", 0xFFFFFF);
        expected.put("HEADING", 0xFFFF55);
        expected.put("TEXT", 0xE0E0E0);
        expected.put("STATUS", 0xA0A0A0);
        expected.put("SUMMARY", 0xA0A0A0);
        expected.put("SUMMARY_HOVER", 0xD0D0D0);
        expected.put("DESCRIPTION", 0xC4C4C4);
        assertConstants(VroGuiColors.class, expected);
    }

    @Test
    void onOffWording() {
        assertEquals("ON", CommandText.onOff(true));
        assertEquals("OFF", CommandText.onOff(false));
    }
}
