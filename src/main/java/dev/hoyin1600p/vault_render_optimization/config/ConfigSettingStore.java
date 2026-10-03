package dev.hoyin1600p.vault_render_optimization.config;

import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast.VroImmediatelyFast;
import dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast.VroImmediatelyFastConfig;
import dev.hoyin1600p.vault_render_optimization.config.ConfigSettingCatalog.Setting;
import dev.hoyin1600p.vault_render_optimization.config.ConfigSettingCatalog.Storage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Reads, defaults and writes catalog settings. Writing goes through the Forge spec values (or the
 * ImmediatelyFast JSON), saves once, re-bakes every live field the same way a config reload does,
 * and then runs the existing setters whose side effects (cache clears, renderer reloads) matter.
 */
public final class ConfigSettingStore {
    private ConfigSettingStore() {
    }

    public static Object defaultValue(Setting setting) {
        if (setting.storage() == Storage.IMMEDIATELY_FAST_JSON) {
            return readJsonField(new VroImmediatelyFastConfig(), setting);
        }
        return valueSpec(setting).getDefault();
    }

    /** Inclusive {min, max} for integer settings, or null when the value is not a ranged integer. */
    public static int[] intRange(Setting setting) {
        if (setting.storage() != Storage.CLIENT_TOML) {
            return null;
        }
        ForgeConfigSpec.Range<?> range = valueSpec(setting).getRange();
        if (range == null || !(range.getMin() instanceof Integer min) || !(range.getMax() instanceof Integer max)) {
            return null;
        }
        return new int[] {min, max};
    }

    /** Current saved values of every setting; the ImmediatelyFast JSON is read from disk once. */
    public static Map<Setting, Object> currentValues() {
        VroImmediatelyFastConfig json = VroImmediatelyFast.readConfigFile();
        Map<Setting, Object> values = new LinkedHashMap<>();
        for (Setting setting : ConfigSettingCatalog.all()) {
            values.put(setting, setting.storage() == Storage.IMMEDIATELY_FAST_JSON
                    ? readJsonField(json, setting)
                    : configValue(setting).get());
        }
        return values;
    }

    /**
     * Boolean settings that ship off, outside Diagnostics and the explicit exclusions: what the
     * Experimental button turns on.
     */
    public static List<Setting> experimentalTargets() {
        return ConfigSettingCatalog.all().stream()
                .filter(setting -> !setting.diagnostics())
                .filter(setting -> !ConfigSettingCatalog.EXPERIMENTAL_EXCLUSIONS.contains(setting.id()))
                .filter(setting -> Boolean.FALSE.equals(defaultValue(setting)))
                .toList();
    }

    /** Experimental targets that are currently off, i.e. the ones Experimental would change. */
    public static List<Setting> experimentalChanges(Map<Setting, Object> current) {
        return experimentalTargets().stream()
                .filter(setting -> !Boolean.TRUE.equals(current.get(setting)))
                .toList();
    }

    public static Map<Setting, Object> defaults() {
        Map<Setting, Object> values = new LinkedHashMap<>();
        for (Setting setting : ConfigSettingCatalog.all()) {
            values.put(setting, defaultValue(setting));
        }
        return values;
    }

    /**
     * Writes the requested values, saves and applies them. Returns the changed settings that need
     * a restart (or rejoin) before they take effect.
     */
    public static List<Setting> apply(Map<Setting, Object> requested) {
        Map<Setting, Object> current = currentValues();
        Map<Setting, Object> changed = new LinkedHashMap<>();
        requested.forEach((setting, value) -> {
            if (value != null && !Objects.equals(current.get(setting), value)) {
                changed.put(setting, value);
            }
        });
        if (changed.isEmpty()) {
            return List.of();
        }

        VroImmediatelyFastConfig json = null;
        boolean tomlChanged = false;
        for (Map.Entry<Setting, Object> entry : changed.entrySet()) {
            Setting setting = entry.getKey();
            if (setting.storage() == Storage.IMMEDIATELY_FAST_JSON) {
                if (json == null) {
                    json = VroImmediatelyFast.readConfigFile();
                }
                writeJsonField(json, setting, (Boolean) entry.getValue());
            } else {
                setConfigValue(setting, entry.getValue());
                tomlChanged = true;
            }
        }
        if (json != null) {
            VroImmediatelyFast.writeConfigFile(json);
        }
        if (tomlChanged) {
            ClientOptimizationConfig.SPEC.save();
            ClientOptimizationConfig.applySavedValues();
            // After the bake, so side effects such as a Create renderer reload see the new values.
            changed.forEach((setting, value) -> {
                if (setting.liveSetter() != null) {
                    setting.liveSetter().accept(value);
                }
            });
        }

        List<Setting> restart = new ArrayList<>();
        changed.keySet().stream().filter(Setting::restartRequired).forEach(restart::add);
        VaultRenderOptimization.LOGGER.info("Settings screen saved {} changed setting(s)", changed.size());
        return restart;
    }

    private static ForgeConfigSpec.ValueSpec valueSpec(Setting setting) {
        Object spec = ClientOptimizationConfig.SPEC.getSpec().get(setting.path());
        if (!(spec instanceof ForgeConfigSpec.ValueSpec valueSpec)) {
            throw new IllegalStateException("No config value at " + String.join(".", setting.path()));
        }
        return valueSpec;
    }

    static ForgeConfigSpec.ConfigValue<?> configValue(Setting setting) {
        Object value = ClientOptimizationConfig.SPEC.getValues().get(setting.path());
        if (!(value instanceof ForgeConfigSpec.ConfigValue<?> configValue)) {
            throw new IllegalStateException("No config value at " + String.join(".", setting.path()));
        }
        return configValue;
    }

    @SuppressWarnings("unchecked")
    private static void setConfigValue(Setting setting, Object value) {
        ((ForgeConfigSpec.ConfigValue<Object>) configValue(setting)).set(value);
    }

    private static Field jsonField(Setting setting) {
        try {
            return VroImmediatelyFastConfig.class.getField(setting.path().get(0));
        } catch (NoSuchFieldException missing) {
            throw new IllegalStateException("No ImmediatelyFast option " + setting.path().get(0), missing);
        }
    }

    private static Boolean readJsonField(VroImmediatelyFastConfig config, Setting setting) {
        try {
            return jsonField(setting).getBoolean(config);
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static void writeJsonField(VroImmediatelyFastConfig config, Setting setting, boolean value) {
        try {
            jsonField(setting).setBoolean(config, value);
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
