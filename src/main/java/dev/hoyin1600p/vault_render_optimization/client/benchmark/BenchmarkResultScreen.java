package dev.hoyin1600p.vault_render_optimization.client.benchmark;

import dev.hoyin1600p.vault_render_optimization.config.ConfigSettingCatalog;
import dev.hoyin1600p.vault_render_optimization.config.ConfigSettingCatalog.Category;
import dev.hoyin1600p.vault_render_optimization.config.ConfigSettingCatalog.Setting;
import dev.hoyin1600p.vault_render_optimization.config.ConfigSettingStore;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;

/** The dialog shown when a GPU benchmark finishes. Its recommended changes are saved only on Apply. */
public final class BenchmarkResultScreen {
    private BenchmarkResultScreen() {
    }

    public static Screen create(BenchmarkRunner runner) {
        Map<Setting, Object> changes = changes(runner.recommendations());
        Component title = new TranslatableComponent("vro.benchmark.result.title");
        if (changes.isEmpty()) {
            return new AlertScreen(BenchmarkResultScreen::close, title,
                    new TranslatableComponent("vro.benchmark.result.none"));
        }
        return new ConfirmScreen(apply -> {
            if (apply) ConfigSettingStore.apply(changes);
            close();
        }, title, message(runner, changes), new TranslatableComponent("vro.benchmark.result.apply"),
                new TranslatableComponent("vro.benchmark.result.keep"));
    }

    /** Each recommended GPU switch whose saved value differs from the recommendation. */
    static Map<Setting, Object> changes(BenchmarkRecommendations.Resolved recommendations) {
        Map<Setting, Object> changes = new LinkedHashMap<>();
        if (recommendations == null) return changes;
        Map<Setting, Object> saved = ConfigSettingStore.currentValues();
        recommendations.states().forEach((id, enabled) -> {
            for (Setting setting : ConfigSettingCatalog.in(Category.GPU)) {
                if (setting.id().equals(id) && !Objects.equals(saved.get(setting), enabled)) {
                    changes.put(setting, enabled);
                }
            }
        });
        return changes;
    }

    private static Component message(BenchmarkRunner runner, Map<Setting, Object> changes) {
        MutableComponent text = new TranslatableComponent("vro.benchmark.result.changes");
        changes.forEach((setting, enabled) -> {
            text.append("\n- ").append(new TranslatableComponent(setting.labelKey())).append(": ")
                    .append((Boolean) enabled ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
            for (BenchmarkRunner.ToggleResult result : runner.results()) {
                if (result.id().equals(setting.id())) text.append(" (" + percent(result.verdict()) + ")");
            }
        });
        if (runner.confirmation() != null) {
            text.append("\n\n").append(new TranslatableComponent("vro.benchmark.result.together",
                    percent(runner.confirmation())));
        }
        return text;
    }

    private static String percent(BenchmarkVerdict verdict) {
        return String.format(Locale.ROOT, "%+.0f%% FPS", verdict.deltaPercent());
    }

    private static void close() {
        Minecraft.getInstance().setScreen(null);
    }
}
