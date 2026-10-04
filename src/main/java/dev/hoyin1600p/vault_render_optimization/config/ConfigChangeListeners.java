package dev.hoyin1600p.vault_render_optimization.config;

import dev.hoyin1600p.vault_render_optimization.config.model.UpdateNoticeFilter;
import java.util.function.Consumer;

/**
 * Seam that lets client services react to configuration changes without the config package
 * depending on them. Client code registers its callbacks during mod construction, before the
 * config spec can load, so every change reaches the same services as before.
 */
public final class ConfigChangeListeners {
    private static volatile Consumer<Boolean> updateChecksChanged = enabled -> { };
    private static volatile Consumer<UpdateNoticeFilter> updateFilterChanged = filter -> { };
    private static volatile Runnable hudTextGeometryChanged = () -> { };

    private ConfigChangeListeners() {
    }

    public static void onUpdateChecksChanged(Consumer<Boolean> listener) {
        updateChecksChanged = listener;
    }

    public static void onUpdateFilterChanged(Consumer<UpdateNoticeFilter> listener) {
        updateFilterChanged = listener;
    }

    public static void onHudTextGeometryChanged(Runnable listener) {
        hudTextGeometryChanged = listener;
    }

    static void updateChecksChanged(boolean enabled) {
        updateChecksChanged.accept(enabled);
    }

    static void updateFilterChanged(UpdateNoticeFilter filter) {
        updateFilterChanged.accept(filter);
    }

    static void hudTextGeometryChanged() {
        hudTextGeometryChanged.run();
    }
}
