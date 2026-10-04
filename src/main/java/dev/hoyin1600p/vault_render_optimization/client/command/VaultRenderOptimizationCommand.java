package dev.hoyin1600p.vault_render_optimization.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.hoyin1600p.vault_render_optimization.backport.RenderBackportOwnershipRegistry;
import dev.hoyin1600p.vault_render_optimization.client.create.CreateDiagnostics;
import dev.hoyin1600p.vault_render_optimization.config.model.UpdateNoticeFilter;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.FlywheelShaderCompatState;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import dev.hoyin1600p.vault_render_optimization.util.CommandText;
import dev.hoyin1600p.vault_render_optimization.util.ModIds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.fml.ModList;

public final class VaultRenderOptimizationCommand {
    private VaultRenderOptimizationCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("vro")
                        .executes(context -> report(context.getSource()))
                        .then(Commands.literal("updates")
                                .executes(context -> reportUpdates(context.getSource()))
                                .then(Commands.literal("status")
                                        .executes(context -> reportUpdates(context.getSource())))
                                .then(Commands.literal("on")
                                        .executes(context -> setUpdates(context.getSource(), true)))
                                .then(Commands.literal("off")
                                        .executes(context -> setUpdates(context.getSource(), false)))
                                .then(Commands.literal("critical")
                                        .executes(context -> setUpdateFilter(
                                                context.getSource(),
                                                UpdateNoticeFilter.CRITICAL)))
                                .then(Commands.literal("all")
                                        .executes(context -> setUpdateFilter(
                                                context.getSource(),
                                                UpdateNoticeFilter.ALL))))
                        .then(Commands.literal("compare")
                                .executes(context -> report(context.getSource()))
                                .then(Commands.literal("on")
                                        .executes(context -> set(context.getSource(), true)))
                                .then(Commands.literal("off")
                                        .executes(context -> set(context.getSource(), false)))
                                .then(Commands.literal("status")
                                        .executes(context -> report(context.getSource()))))
                        .then(Commands.literal("backports")
                                .executes(context -> reportBackports(context.getSource())))
                        .then(ParticleCommand.build())
                        .then(GpuEntityCommand.build())
                        .then(RuntimeExperimentCommand.build())
                        .then(FeatureToggleCommand.build())
                        .then(FeatureToggleCommand.buildAlloc())
                        .then(ChunkUpdateCommand.build())
                        .then(SophisticatedStorageCommand.build())
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
                        .then(Commands.literal("create")
                                .executes(context -> reportCreate(context.getSource()))
                                .then(Commands.literal("status")
                                        .executes(context -> reportCreate(context.getSource())))
                                .then(Commands.literal("shader_compat")
                                        .executes(context -> reportCreateShaderCompat(context.getSource()))
                                        .then(Commands.literal("on")
                                                .executes(context -> setCreateShaderCompat(context.getSource(), true)))
                                        .then(Commands.literal("off")
                                                .executes(context -> setCreateShaderCompat(context.getSource(), false)))
                                        .then(Commands.literal("status")
                                                .executes(context -> reportCreateShaderCompat(context.getSource())))))
        );
    }

    private static int setUpdates(CommandSourceStack source, boolean enabled) {
        ClientOptimizationConfig.setUpdateChecks(enabled);
        source.sendSuccess(
                new TextComponent(
                        "[VRO] Update checks " + (enabled ? "enabled" : "disabled")
                                + " and saved. The change applies immediately."
                ),
                false
        );
        reportUpdates(source);
        return enabled ? 1 : 0;
    }

    private static int setUpdateFilter(
            CommandSourceStack source,
            UpdateNoticeFilter filter
    ) {
        ClientOptimizationConfig.setUpdateNoticeFilter(filter);
        source.sendSuccess(
                new TextComponent(
                        "[VRO] Update types set to " + filter.name()
                                + " and saved. The change applies immediately."
                ),
                false
        );
        reportUpdates(source);
        return 1;
    }

    private static int reportUpdates(CommandSourceStack source) {
        boolean enabled = ClientOptimizationConfig.updateChecksEnabled();
        source.sendSuccess(
                new TextComponent(
                        "[VRO] Update checks are " + CommandText.onOff(enabled)
                                + "; displayed update types: "
                                + ClientOptimizationConfig.updateNoticeFilter().name()
                                + "."
                ),
                false
        );
        return enabled ? 1 : 0;
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
                                + CommandText.onOff(enabled)
                                + ". Performance optimizations are "
                                + (enabled ? "OFF" : "ON")
                                + "."
                ),
                false
        );
        return enabled ? 1 : 0;
    }

    private static int reportBackports(CommandSourceStack source) {
        source.sendSuccess(
                new TextComponent(
                        "[VRO] ModernFix render-backport ownership: "
                                + RenderBackportOwnershipRegistry.summary()
                ),
                false
        );
        for (String line : RenderBackportOwnershipRegistry.reportLines()) {
            source.sendSuccess(new TextComponent("[VRO] " + line), false);
        }
        return 1;
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
                                + CommandText.onOff(ClientOptimizationConfig.verticalSectionCulling)
                                + " at " + ClientOptimizationConfig.verticalSectionDistance
                                + " sections; horizontal "
                                + CommandText.onOff(ClientOptimizationConfig.horizontalSectionCulling)
                                + " at " + ClientOptimizationConfig.horizontalSectionDistance
                                + " sections. Changes apply immediately."
                ),
                false
        );
        return 1;
    }

    private static int reportCreate(CommandSourceStack source) {
        if (!ModList.get().isLoaded(ModIds.CREATE)) {
            source.sendSuccess(new TextComponent("[VRO] Create is not installed."), false);
            return 0;
        }
        int result = CreateDiagnostics.report(source);
        reportCreateShaderCompat(source);
        return result;
    }

    private static int setCreateShaderCompat(CommandSourceStack source, boolean enabled) {
        ClientOptimizationConfig.setCreateFlywheelShaderCompat(enabled);
        return reportCreateShaderCompat(source);
    }

    private static int reportCreateShaderCompat(CommandSourceStack source) {
        if (!ModList.get().isLoaded(ModIds.OCULUS) || !ModList.get().isLoaded(ModIds.FLYWHEEL)) {
            source.sendSuccess(
                    new TextComponent("[VRO] Create shader compatibility is unavailable; Oculus and Flywheel are required."),
                    false
            );
            return 0;
        }
        source.sendSuccess(
                new TextComponent(
                        "[VRO] Create shader compatibility: configured "
                                + CommandText.onOff(ClientOptimizationConfig.createFlywheelShaderCompat)
                                + ", pipeline " + CommandText.onOff(FlywheelShaderCompatState.hasPipeline())
                                + ", active " + CommandText.onOff(FlywheelShaderCompatState.isRenderPathActive())
                                + ", failed " + (FlywheelShaderCompatState.hasFailed() ? "YES" : "NO")
                                + ". Changes apply immediately."
                ),
                false
        );
        return FlywheelShaderCompatState.isRenderPathActive() ? 1 : 0;
    }
}
