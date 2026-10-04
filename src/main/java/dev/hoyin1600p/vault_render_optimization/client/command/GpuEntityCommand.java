package dev.hoyin1600p.vault_render_optimization.client.command;

import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuEntityModels;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import dev.hoyin1600p.vault_render_optimization.util.CommandText;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;

/** {@code /vro gpuentity status|on|off|stats|selftest}. Client commands run on the render thread. */
public final class GpuEntityCommand {
    private GpuEntityCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("gpuentity")
                .executes(context -> status(context.getSource()))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("on").executes(context -> set(context.getSource(), true)))
                .then(Commands.literal("off").executes(context -> set(context.getSource(), false)))
                .then(Commands.literal("stats")
                        .executes(context -> stats(context.getSource()))
                        .then(Commands.literal("reset").executes(context -> {
                            GpuEntityModels.resetStats();
                            return stats(context.getSource());
                        })))
                .then(Commands.literal("verify")
                        .executes(context -> verify(context.getSource()))
                        .then(Commands.literal("on").executes(context -> {
                            GpuEntityModels.setVerify(true);
                            return verify(context.getSource());
                        }))
                        .then(Commands.literal("off").executes(context -> {
                            GpuEntityModels.setVerify(false);
                            return verify(context.getSource());
                        })))
                .then(Commands.literal("selftest").executes(context -> {
                    long seed = System.nanoTime();
                    send(context.getSource(), "GPU entity self-test: " + GpuEntityModels.selfTest(seed));
                    return 1;
                }));
    }

    private static int status(CommandSourceStack source) {
        send(source, GpuEntityModels.status());
        return 1;
    }

    private static int verify(CommandSourceStack source) {
        send(source, GpuEntityModels.verifyStatus());
        return 1;
    }

    private static int stats(CommandSourceStack source) {
        send(source, "GPU entity models: " + GpuEntityModels.stats());
        return 1;
    }

    private static int set(CommandSourceStack source, boolean enabled) {
        ClientOptimizationConfig.setGpuEntityModels(enabled);
        send(source, "GPU entity models " + (enabled ? "enabled" : "disabled") + " (takes effect next frame).");
        return status(source);
    }

    private static void send(CommandSourceStack source, String text) {
        for (String line : text.split("\n")) {
            source.sendSuccess(new TextComponent("[VRO] " + line), false);
            // Also in the log, so automated test runs can read results without the chat overlay.
            CommandText.logCommandLine(line);
        }
    }
}
