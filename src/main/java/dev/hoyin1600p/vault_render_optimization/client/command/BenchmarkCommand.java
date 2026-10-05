package dev.hoyin1600p.vault_render_optimization.client.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.hoyin1600p.vault_render_optimization.client.benchmark.BenchmarkFrameHook;
import dev.hoyin1600p.vault_render_optimization.util.CommandText;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;

/** {@code /vro benchmark gpu start [view]|cancel|status|result}: start without {@code view} uses the crowded scene. */
public final class BenchmarkCommand {
    private BenchmarkCommand() { }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("benchmark").then(Commands.literal("gpu")
                .then(Commands.literal("start").executes(c -> send(c.getSource(), BenchmarkFrameHook.start()))
                        .then(Commands.literal("view")
                                .executes(c -> send(c.getSource(), BenchmarkFrameHook.start(false)))))
                .then(Commands.literal("cancel").executes(c -> send(c.getSource(), BenchmarkFrameHook.cancel())))
                .then(Commands.literal("status").executes(c -> send(c.getSource(), BenchmarkFrameHook.status())))
                .then(Commands.literal("result").executes(c -> {
                    for (String line : BenchmarkFrameHook.resultLines()) send(c.getSource(), line);
                    return 1;
                })));
    }

    private static int send(CommandSourceStack source, String line) {
        source.sendSuccess(new TextComponent("[VRO] " + line), false);
        CommandText.logCommandLine(line);
        return 1;
    }
}
