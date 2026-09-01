package dev.hoyin1600p.vault_render_optimization.renderertransfer;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraftforge.fml.loading.FMLPaths;

/** Reads startup-bound renderer-transfer controls before Forge attaches the client config. */
public final class BootstrapRendererTransferConfig {
    private static volatile Snapshot snapshot;

    private BootstrapRendererTransferConfig() {
    }

    public static boolean enabled(RendererTransferFeature feature) {
        capture();
        return snapshot.options().get(feature);
    }

    public static synchronized void capture() {
        if (snapshot != null) {
            return;
        }
        Path path = FMLPaths.CONFIGDIR.get().resolve("vault_render_optimization-client.toml");
        if (!Files.isRegularFile(path)) {
            snapshot = resolveValues(key -> null);
            return;
        }
        try (CommentedFileConfig config = CommentedFileConfig.of(path)) {
            config.load();
            snapshot = resolveValues(config::get);
        } catch (Exception exception) {
            VaultRenderOptimization.LOGGER.warn(
                    "Could not capture renderer-transfer options from {}; safe defaults will be used", path, exception
            );
            snapshot = resolveValues(key -> null);
        }
    }

    static Snapshot resolveValues(Function<List<String>, Object> lookup) {
        EnumMap<RendererTransferFeature, Boolean> options =
                new EnumMap<>(RendererTransferFeature.class);
        for (RendererTransferFeature feature : RendererTransferFeature.values()) {
            Object value = lookup.apply(List.of("embeddium_transfers", feature.configKey()));
            options.put(feature, value instanceof Boolean enabled ? enabled : true);
        }
        return new Snapshot(Map.copyOf(options));
    }

    record Snapshot(Map<RendererTransferFeature, Boolean> options) {
    }
}
