package dev.hoyin1600p.vault_render_optimization.client;

import com.mojang.brigadier.CommandDispatcher;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;

public final class VaultRenderOptimizationCommand {
    private VaultRenderOptimizationCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("vro")
                        .executes(context -> report(context.getSource()))
                        .then(Commands.literal("compare")
                                .executes(context -> report(context.getSource()))
                                .then(Commands.literal("on")
                                        .executes(context -> set(context.getSource(), true)))
                                .then(Commands.literal("off")
                                        .executes(context -> set(context.getSource(), false)))
                                .then(Commands.literal("status")
                                        .executes(context -> report(context.getSource()))))
        );
    }

    private static int set(CommandSourceStack source, boolean enabled) {
        ClientOptimizationConfig.setCompareMode(enabled);
        source.sendSuccess(
                new TextComponent(
                        "[VRO] Compare Mode "
                                + (enabled ? "enabled" : "disabled")
                                + " and saved. Performance optimizations are now "
                                + (enabled ? "OFF" : "ON")
                                + "; safety and compatibility fixes remain active."
                ),
                false
        );
        return 1;
    }

    private static int report(CommandSourceStack source) {
        boolean enabled = ClientOptimizationConfig.compareModeEnabled();
        source.sendSuccess(
                new TextComponent(
                        "[VRO] Compare Mode is "
                                + (enabled ? "ON" : "OFF")
                                + ". Performance optimizations are "
                                + (enabled ? "OFF" : "ON")
                                + "."
                ),
                false
        );
        return enabled ? 1 : 0;
    }
}
