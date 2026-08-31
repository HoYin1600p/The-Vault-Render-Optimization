package dev.hoyin1600p.vault_render_optimization.backport;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraftforge.fml.loading.FMLPaths;

/** Captures restart-bound mixin options before Forge attaches the client config. */
public final class BootstrapRenderBackportConfig {
    private static final String CLIENT_CONFIG = "vault_render_optimization-client.toml";
    private static volatile Snapshot launchSnapshot;

    private BootstrapRenderBackportConfig() {
    }

    public static boolean enabled(RenderBackportFeature feature) {
        capture();
        return launchSnapshot.options().get(feature);
    }

    public static boolean compareMode() {
        capture();
        return launchSnapshot.compareMode();
    }

    public static synchronized void capture() {
        if (launchSnapshot != null) {
            return;
        }
        launchSnapshot = readLaunchSnapshot();
    }

    static Snapshot resolveValues(Function<List<String>, Object> lookup) {
        EnumMap<RenderBackportFeature, Boolean> options =
                new EnumMap<>(RenderBackportFeature.class);
        for (RenderBackportFeature feature : RenderBackportFeature.values()) {
            Object configured = lookup.apply(List.of("modernfix_backports", feature.configKey()));
            options.put(feature, configured instanceof Boolean enabled ? enabled : true);
        }
        Object compare = lookup.apply(List.of("benchmark", "compare_mode"));
        return new Snapshot(Map.copyOf(options), compare instanceof Boolean enabled && enabled);
    }

    private static Snapshot readLaunchSnapshot() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve(CLIENT_CONFIG);
        if (!Files.isRegularFile(configPath)) {
            return resolveValues(path -> null);
        }
        try (CommentedFileConfig config = CommentedFileConfig.of(configPath)) {
            config.load();
            return resolveValues(config::get);
        } catch (Exception exception) {
            VaultRenderOptimization.LOGGER.warn(
                    "Could not capture restart-bound render backport options from {}; safe defaults will be used",
                    configPath,
                    exception
            );
            return resolveValues(path -> null);
        }
    }

    record Snapshot(Map<RenderBackportFeature, Boolean> options, boolean compareMode) {
    }
}
