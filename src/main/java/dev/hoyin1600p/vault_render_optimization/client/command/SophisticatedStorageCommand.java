package dev.hoyin1600p.vault_render_optimization.client.command;

import dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage.SophisticatedStorageDiagnostics;
import com.mojang.brigadier.builder.ArgumentBuilder;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import dev.hoyin1600p.vault_render_optimization.util.CommandText;
import dev.hoyin1600p.vault_render_optimization.util.ModIds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.fml.ModList;

public final class SophisticatedStorageCommand {
    private SophisticatedStorageCommand() {
    }

    public static ArgumentBuilder<CommandSourceStack, ?> build() {
        return Commands.literal("storage")
                .executes(context -> report(context.getSource()))
                .then(Commands.literal("status").executes(context -> report(context.getSource())))
                .then(toggle("face", ClientOptimizationConfig::setSophisticatedStorageFaceCulling))
                .then(toggle("counts", ClientOptimizationConfig::setSophisticatedStorageCountCache))
                .then(toggle("fill", ClientOptimizationConfig::setSophisticatedStorageFillFastPath))
                .then(toggle("updates", ClientOptimizationConfig::setSophisticatedStorageRenderUpdateFilter))
                .then(Commands.literal("diagnostics")
                        .executes(context -> diagnostics(context.getSource()))
                        .then(Commands.literal("on").executes(context -> setDiagnostics(context.getSource(), true)))
                        .then(Commands.literal("off").executes(context -> setDiagnostics(context.getSource(), false)))
                        .then(Commands.literal("reset").executes(context -> resetDiagnostics(context.getSource()))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> toggle(String name, Toggle setter) {
        return Commands.literal(name)
                .then(Commands.literal("on").executes(context -> set(context.getSource(), setter, true)))
                .then(Commands.literal("off").executes(context -> set(context.getSource(), setter, false)));
    }

    private static int set(CommandSourceStack source, Toggle setter, boolean enabled) {
        setter.set(enabled);
        return report(source);
    }

    private static int setDiagnostics(CommandSourceStack source, boolean enabled) {
        ClientOptimizationConfig.setSophisticatedStorageDiagnostics(enabled);
        return diagnostics(source);
    }

    private static int resetDiagnostics(CommandSourceStack source) {
        SophisticatedStorageDiagnostics.reset();
        source.sendSuccess(new TextComponent("[VRO] Sophisticated Storage diagnostics reset."), false);
        return 1;
    }

    private static int report(CommandSourceStack source) {
        source.sendSuccess(new TextComponent(
                "[VRO] Sophisticated Storage: mod " + CommandText.onOff(ModList.get().isLoaded(ModIds.SOPHISTICATED_STORAGE))
                        + ", front-display culling " + CommandText.onOff(ClientOptimizationConfig.sophisticatedStorageFaceCulling)
                        + ", quantity cache " + CommandText.onOff(ClientOptimizationConfig.sophisticatedStorageCountCache)
                        + ", fill fast path " + CommandText.onOff(ClientOptimizationConfig.sophisticatedStorageFillFastPath)
                        + ", count-only rebuild filter "
                        + CommandText.onOff(ClientOptimizationConfig.sophisticatedStorageRenderUpdateFilter)
                        + ". Changes apply immediately; Compare Mode overrides performance paths."
        ), false);
        return 1;
    }

    private static int diagnostics(CommandSourceStack source) {
        SophisticatedStorageDiagnostics.Snapshot snapshot = SophisticatedStorageDiagnostics.snapshot();
        source.sendSuccess(new TextComponent(
                "[VRO] Sophisticated Storage diagnostics " + CommandText.onOff(snapshot.enabled())
                        + ": front rendered=" + snapshot.frontFacesRendered()
                        + ", backface skipped=" + snapshot.backFacesSkipped()
                        + ", covered skipped=" + snapshot.coveredFacesSkipped()
                        + ", count labels=" + snapshot.countLabelsRendered()
                        + ", count cache hit/miss=" + snapshot.countCacheHits() + "/" + snapshot.countCacheMisses()
                        + ", fill bars=" + snapshot.fillBarsRendered()
                        + ", model rebuild allowed=" + snapshot.modelRebuildsAllowed()
                        + ", count-only rebuild skipped=" + snapshot.countOnlyRebuildsSkipped()
                        + "."
        ), false);
        return snapshot.enabled() ? 1 : 0;
    }

        @FunctionalInterface
    private interface Toggle {
        void set(boolean enabled);
    }
}
