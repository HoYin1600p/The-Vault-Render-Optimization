package dev.hoyin1600p.vault_render_optimization.client.chunk;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
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
}
