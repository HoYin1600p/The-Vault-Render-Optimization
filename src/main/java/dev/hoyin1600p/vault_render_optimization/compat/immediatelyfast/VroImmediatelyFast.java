/*
 * Based on ImmediatelyFast Reforged's ImmediatelyFast entry class (LGPL-3.0-or-later),
 * Copyright (C) 2023 RK_01/RaphiMC and contributors. Modified by HoYin1600p for VRO, 2026:
 * no longer a mod entry point; owns the configuration, the one-time ownership decision and the
 * Oculus lookup, and never exits the game on failure.
 */
package dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast.compat.IrisCompat;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.LoadingModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

/**
 * VRO's built-in copy of ImmediatelyFast Reforged 1.18.2 (1.1.10). It applies only when enabled in
 * {@code config/vault_render_optimization-immediatelyfast.json}, when the standalone
 * {@code immediatelyfast} mod is absent (that one always wins), and, with Oculus installed, when
 * the Oculus members its shader compatibility needs are present.
 */
public final class VroImmediatelyFast {
    public static final Logger LOGGER = LoggerFactory.getLogger("VRO ImmediatelyFast");
    public static final String NAMESPACE = "vault_render_optimization";
    public static final String RENDER_TYPE_PREFIX = "vault_render_optimization_immediatelyfast_";
    public static final String STANDALONE_MOD_ID = "immediatelyfast";
    public static final String CONFIG_FILE = "vault_render_optimization-immediatelyfast.json";
    public static final Unsafe UNSAFE = unsafe();
    public static VroImmediatelyFastConfig config = new VroImmediatelyFastConfig();

    private static Boolean applied;
    private static String reason = "not evaluated";

    private VroImmediatelyFast() {
    }

    /** One decision for the whole session; safe to call from any mixin plugin. */
    public static synchronized boolean applied() {
        if (applied == null) {
            loadConfig();
            applied = decide(FMLLoader.getLoadingModList());
            LOGGER.info("Built-in ImmediatelyFast: {} ({})", applied ? "ACTIVE" : "OFF", reason);
        }
        return applied;
    }

    public static synchronized String status() {
        applied();
        return (applied ? "ACTIVE" : "OFF") + " (" + reason + ")";
    }

    /** True when ImmediatelyFast (standalone or built in) owns text lookup or HUD batching. */
    public static boolean ownsText() {
        LoadingModList mods = FMLLoader.getLoadingModList();
        if (mods != null && mods.getModFileById(STANDALONE_MOD_ID) != null) return true;
        return applied() && (config.hud_batching || config.fast_text_lookup);
    }

    private static boolean decide(LoadingModList mods) {
        if (!config.enabled) {
            reason = "disabled in " + CONFIG_FILE;
            return false;
        }
        if (mods == null) {
            reason = "mod list unavailable";
            return false;
        }
        if (mods.getModFileById(STANDALONE_MOD_ID) != null) {
            reason = "the standalone ImmediatelyFast mod is installed and owns these optimizations";
            return false;
        }
        if (mods.getModFileById("oculus") != null) {
            String problem = IrisCompat.preflight(mods);
            if (problem != null) {
                reason = "Oculus is installed but " + problem;
                return false;
            }
        }
        reason = "enabled; standalone ImmediatelyFast absent";
        return true;
    }

    public static void loadConfig() {
        config = readConfigFile();
        writeConfigFile(config);
    }

    /**
     * Reads the JSON as it is on disk now (defaults when missing or unreadable). The settings screen
     * edits this copy; the running session keeps {@link #config}, which only a restart reloads.
     */
    public static VroImmediatelyFastConfig readConfigFile() {
        Path file = FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE);
        VroImmediatelyFastConfig loaded = null;
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                loaded = new Gson().fromJson(reader, VroImmediatelyFastConfig.class);
            } catch (Throwable failure) {
                LOGGER.error("Failed to read {}; using defaults", CONFIG_FILE, failure);
            }
        }
        return loaded == null ? new VroImmediatelyFastConfig() : loaded;
    }

    public static boolean writeConfigFile(VroImmediatelyFastConfig values) {
        Path file = FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE);
        try {
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(values));
            return true;
        } catch (Throwable failure) {
            LOGGER.error("Failed to write {}", CONFIG_FILE, failure);
            return false;
        }
    }

    private static Unsafe unsafe() {
        try {
            for (Field field : Unsafe.class.getDeclaredFields()) {
                if (field.getType().equals(Unsafe.class)) {
                    field.setAccessible(true);
                    return (Unsafe) field.get(null);
                }
            }
        } catch (Throwable ignored) {
        }
        throw new IllegalStateException("Unable to get Unsafe instance");
    }
}
