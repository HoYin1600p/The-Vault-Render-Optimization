package dev.hoyin1600p.vault_render_optimization.client.chunk;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import dev.hoyin1600p.vault_render_optimization.client.chunk.sorting.IndexSortState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;

public final class ChunkUpdateCommand {
    private ChunkUpdateCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("chunks")
                .executes(context -> report(context.getSource()))
                .then(Commands.literal("status").executes(context -> report(context.getSource())))
                .then(Commands.literal("sorting")
                        .executes(context -> sortStatus(context.getSource()))
                        .then(Commands.literal("status").executes(context -> sortStatus(context.getSource())))
                        .then(Commands.literal("on").executes(context -> setSorting(context.getSource(), true)))
                        .then(Commands.literal("off").executes(context -> setSorting(context.getSource(), false))))
                .then(Commands.literal("defer")
                        .then(Commands.literal("on").executes(context -> set(context.getSource(), true)))
                        .then(Commands.literal("off").executes(context -> set(context.getSource(), false))));
    }

    private static int set(CommandSourceStack source, boolean enabled) {
        ClientOptimizationConfig.setDeferChunkUpdates(enabled);
        source.sendSuccess(new TextComponent("[VRO] Chunk deferral " + (enabled ? "enabled" : "disabled")
                + " and saved; applies to new scheduling decisions without a restart."
                + " Renderer settings are not overwritten."), false);
        return report(source);
    }

    private static int report(CommandSourceStack source) {
        source.sendSuccess(new TextComponent("[VRO] Chunk updates: " + ChunkUpdateState.status() + "."), false);
        source.sendSuccess(new TextComponent("[VRO] Deferred updates can delay visible block changes."
                + " Off/Compare Mode yields to renderer settings; it does not force synchronous updates."), false);
        return 1;
    }

    private static int setSorting(CommandSourceStack source, boolean enabled) {
        ClientOptimizationConfig.setIndexOnlySorting(enabled);
        source.sendSuccess(new TextComponent("[VRO] Index-only sorting " + (enabled ? "enabled" : "disabled")
                + " and saved. New jobs use this setting; queued jobs finish safely."), false);
        return sortStatus(source);
    }

    private static int sortStatus(CommandSourceStack source) {
        source.sendSuccess(new TextComponent("[VRO] Index-only sorting: " + IndexSortState.status()), false);
        return 1;
    }
}
