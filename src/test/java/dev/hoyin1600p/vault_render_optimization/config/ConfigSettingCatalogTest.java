package dev.hoyin1600p.vault_render_optimization.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast.VroImmediatelyFastConfig;
import dev.hoyin1600p.vault_render_optimization.config.ConfigSettingCatalog.Category;
import dev.hoyin1600p.vault_render_optimization.config.ConfigSettingCatalog.Setting;
import dev.hoyin1600p.vault_render_optimization.config.ConfigSettingCatalog.Storage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.jupiter.api.Test;

class ConfigSettingCatalogTest {
    @Test
    void everyForgeConfigValueIsCoveredExactlyOnce() {
        List<String> specPaths = new ArrayList<>();
        collectValuePaths(ClientOptimizationConfig.SPEC.getValues(), specPaths);
        List<String> catalogPaths = ConfigSettingCatalog.all().stream()
                .filter(setting -> setting.storage() == Storage.CLIENT_TOML)
                .map(setting -> String.join(".", setting.path()))
                .toList();

        assertEquals(catalogPaths.size(), new HashSet<>(catalogPaths).size(), "a .toml value is listed twice");
        assertEquals(new HashSet<>(specPaths), new HashSet<>(catalogPaths));
    }

    @Test
    void everyImmediatelyFastOptionIsListedOnceOrDeliberatelyUnlisted() {
        Set<String> options = new HashSet<>();
        for (Field field : VroImmediatelyFastConfig.class.getFields()) {
            if (field.getType() == boolean.class && !Modifier.isStatic(field.getModifiers())) {
                options.add(field.getName());
            }
        }
        List<String> listed = ConfigSettingCatalog.all().stream()
                .filter(setting -> setting.storage() == Storage.IMMEDIATELY_FAST_JSON)
                .map(setting -> setting.path().get(0))
                .toList();

        assertEquals(listed.size(), new HashSet<>(listed).size(), "an ImmediatelyFast option is listed twice");
        // VRO's built-in copy never reads this one, so it stays off the screen.
        assertEquals(Set.of("debug_only_and_not_recommended_disable_mod_conflict_handling"),
                ConfigSettingCatalog.UNLISTED_IMMEDIATELY_FAST_OPTIONS);
        for (String unlisted : ConfigSettingCatalog.UNLISTED_IMMEDIATELY_FAST_OPTIONS) {
            assertTrue(options.contains(unlisted), unlisted);
            assertFalse(listed.contains(unlisted), unlisted);
        }
        Set<String> expected = new HashSet<>(options);
        expected.removeAll(ConfigSettingCatalog.UNLISTED_IMMEDIATELY_FAST_OPTIONS);
        assertEquals(expected, new HashSet<>(listed));
    }

    @Test
    void settingIdsAndTranslationKeysAreUnique() {
        List<String> keys = ConfigSettingCatalog.all().stream().map(Setting::labelKey).toList();
        assertEquals(keys.size(), new HashSet<>(keys).size());
    }

    @Test
    void everyCatalogKeyHasEnglishText() throws Exception {
        JsonObject lang;
        try (InputStream stream = ConfigSettingCatalogTest.class.getResourceAsStream(
                "/assets/vault_render_optimization/lang/en_us.json")) {
            assertNotNull(stream, "en_us.json is missing");
            lang = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        List<String> required = new ArrayList<>(List.of(
                "key.vault_render_optimization.open_config",
                "key.categories.vault_render_optimization",
                "vro.config.title",
                "vro.config.badge.restart",
                "vro.config.button.default",
                "vro.config.button.experimental",
                "vro.config.dialog.default.title",
                "vro.config.dialog.default.message",
                "vro.config.dialog.experimental.title",
                "vro.config.dialog.experimental.message",
                "vro.config.dialog.experimental.none",
                "vro.config.dialog.restart.title",
                "vro.config.dialog.restart.message",
                "vro.config.dialog.more",
                "vro.config.message.cloth_missing",
                "vro.config.message.cloth_incompatible"
        ));
        for (Category category : Category.values()) {
            required.add(category.translationKey());
        }
        for (Setting setting : ConfigSettingCatalog.all()) {
            required.add(setting.labelKey());
            required.add(setting.summaryKey());
            required.add(setting.tooltipKey());
        }
        for (String key : required) {
            assertTrue(lang.has(key), "en_us.json lacks " + key);
            assertFalse(lang.get(key).getAsString().isBlank(), "en_us.json has blank " + key);
        }
    }

    @Test
    void experimentalIsDefaultOffBooleansOutsideDiagnosticsAndExclusions() {
        Set<Setting> expected = new HashSet<>();
        VroImmediatelyFastConfig jsonDefaults = new VroImmediatelyFastConfig();
        for (Setting setting : ConfigSettingCatalog.all()) {
            Object defaultValue = setting.storage() == Storage.CLIENT_TOML
                    ? ((ForgeConfigSpec.ValueSpec) ClientOptimizationConfig.SPEC.getSpec().get(setting.path()))
                            .getDefault()
                    : readField(jsonDefaults, setting.path().get(0));
            if (Boolean.FALSE.equals(defaultValue) && setting.category() != Category.DIAGNOSTICS
                    && !ConfigSettingCatalog.EXPERIMENTAL_EXCLUSIONS.contains(setting.id())) {
                expected.add(setting);
            }
        }

        List<Setting> experimental = ConfigSettingStore.experimentalTargets();
        assertEquals(expected, new HashSet<>(experimental));
        assertTrue(experimental.stream().noneMatch(Setting::diagnostics));
        assertEquals(Set.of("horizontal_enabled", "gpu_entity_models", "gpu_items", "gpu_particles",
                "gpu_entity_models_with_shaders", "gpu_particles_with_shaders"),
                ConfigSettingCatalog.EXPERIMENTAL_EXCLUSIONS);
        assertTrue(experimental.stream().noneMatch(setting -> setting.id().equals("horizontal_enabled")));
        // The current set, so an accidental new default-off option is noticed in review.
        assertEquals(Set.of(
                "sort_geometry_cache",
                "hud_text_geometry",
                "vertex_buffer_adaptive_trimming",
                "experimental_item_hud_batching"
        ), experimental.stream().map(Setting::id).collect(Collectors.toSet()));
    }

    @Test
    void defaultsResetEveryGpuSwitchOffAndExperimentalExcludesThem() {
        Map<Setting, Object> defaults = ConfigSettingStore.defaults();
        ConfigSettingCatalog.in(Category.GPU).forEach(setting -> {
            assertEquals(false, defaults.get(setting), setting.id());
            assertTrue(ConfigSettingCatalog.EXPERIMENTAL_EXCLUSIONS.contains(setting.id()), setting.id());
            assertFalse(ConfigSettingStore.experimentalTargets().contains(setting), setting.id());
        });
    }

    @Test
    void startupBoundSettingsCarryTheRestartBadge() {
        Map<String, Boolean> restart = new HashMap<>();
        ConfigSettingCatalog.all().forEach(setting -> restart.put(setting.id(), setting.restartRequired()));
        ConfigSettingCatalog.all().stream()
                .filter(setting -> setting.id().startsWith("backport_") || setting.id().startsWith("transfer_")
                        || setting.storage() == Storage.IMMEDIATELY_FAST_JSON)
                .forEach(setting -> assertTrue(setting.restartRequired(), setting.id()));
        assertTrue(restart.get("compare_mode"));
        assertTrue(restart.get("async_arena_growth_divisor"));
        assertTrue(restart.get("async_arena_max_headroom_mib"));
        assertFalse(restart.get("gpu_entity_models"));
        assertFalse(restart.get("vertex_buffer_max_retained_mib"));
    }

    @Test
    void integerSettingsExposeTheirSpecRange() {
        Setting vertical = ConfigSettingCatalog.all().stream()
                .filter(setting -> setting.id().equals("vertical_distance")).findFirst().orElseThrow();
        int[] range = ConfigSettingStore.intRange(vertical);
        assertNotNull(range);
        assertEquals(1, range[0]);
        assertEquals(64, range[1]);
        assertEquals(12, ConfigSettingStore.defaultValue(vertical));
    }

    private static void collectValuePaths(UnmodifiableConfig config, List<String> out) {
        for (Object value : config.valueMap().values()) {
            if (value instanceof UnmodifiableConfig child) {
                collectValuePaths(child, out);
            } else if (value instanceof ForgeConfigSpec.ConfigValue<?> configValue) {
                out.add(String.join(".", configValue.getPath()));
            }
        }
    }

    private static Object readField(VroImmediatelyFastConfig config, String name) {
        try {
            return VroImmediatelyFastConfig.class.getField(name).getBoolean(config);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }
}
