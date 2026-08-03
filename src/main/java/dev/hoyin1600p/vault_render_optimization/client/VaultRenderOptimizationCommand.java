package dev.hoyin1600p.vault_render_optimization.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
                        .then(Commands.literal("culling")
                                .executes(context -> reportCulling(context.getSource()))
                                .then(Commands.literal("vertical")
                                        .then(Commands.literal("on")
                                                .executes(context -> setVerticalCulling(
                                                        context.getSource(), true)))
                                        .then(Commands.literal("off")
                                                .executes(context -> setVerticalCulling(
                                                        context.getSource(), false)))
                                        .then(Commands.argument("distance", IntegerArgumentType.integer(1, 64))
                                                .executes(context -> setVerticalDistance(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "distance")))))
                                .then(Commands.literal("horizontal")
                                        .then(Commands.literal("on")
                                                .executes(context -> setHorizontalCulling(
                                                        context.getSource(), true)))
                                        .then(Commands.literal("off")
                                                .executes(context -> setHorizontalCulling(
                                                        context.getSource(), false)))
                                        .then(Commands.argument("distance", IntegerArgumentType.integer(1, 64))
                                                .executes(context -> setHorizontalDistance(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "distance"))))))
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

    private static int setVerticalCulling(CommandSourceStack source, boolean enabled) {
        ClientOptimizationConfig.setVerticalSectionCulling(enabled);
        return reportCulling(source);
    }

    private static int setHorizontalCulling(CommandSourceStack source, boolean enabled) {
        ClientOptimizationConfig.setHorizontalSectionCulling(enabled);
        return reportCulling(source);
    }

    private static int setVerticalDistance(CommandSourceStack source, int distance) {
        ClientOptimizationConfig.setVerticalSectionDistance(distance);
        return reportCulling(source);
    }

    private static int setHorizontalDistance(CommandSourceStack source, int distance) {
        ClientOptimizationConfig.setHorizontalSectionDistance(distance);
        return reportCulling(source);
    }

    private static int reportCulling(CommandSourceStack source) {
        source.sendSuccess(
                new TextComponent(
                        "[VRO] Section culling: vertical "
                                + state(ClientOptimizationConfig.verticalSectionCulling)
                                + " at " + ClientOptimizationConfig.verticalSectionDistance
                                + " sections; horizontal "
                                + state(ClientOptimizationConfig.horizontalSectionCulling)
                                + " at " + ClientOptimizationConfig.horizontalSectionDistance
                                + " sections. Changes apply immediately."
                ),
                false
        );
        return 1;
    }

    private static String state(boolean enabled) {
        return enabled ? "ON" : "OFF";
    }
}
