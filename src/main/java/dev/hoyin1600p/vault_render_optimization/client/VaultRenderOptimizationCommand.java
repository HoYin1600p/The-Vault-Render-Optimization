package dev.hoyin1600p.vault_render_optimization.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.hoyin1600p.vault_render_optimization.backport.RenderBackportOwnershipRegistry;
import dev.hoyin1600p.vault_render_optimization.client.lighting.DynamicLightEngine;
import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleCommand;
import dev.hoyin1600p.vault_render_optimization.client.chunk.ChunkUpdateCommand;
import dev.hoyin1600p.vault_render_optimization.client.compat.manastealer.ManaStealerCommand;
import dev.hoyin1600p.vault_render_optimization.client.create.CreateDiagnostics;
import dev.hoyin1600p.vault_render_optimization.client.update.UpdateNoticeFilter;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.FlywheelShaderCompatState;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
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
                        .then(ChunkUpdateCommand.build())
                        .then(ManaStealerCommand.build())
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
                        .then(Commands.literal("lights")
                                .executes(context -> reportLights(context.getSource()))
                                .then(Commands.literal("on")
                                        .executes(context -> setLights(context.getSource(), true)))
                                .then(Commands.literal("off")
                                        .executes(context -> setLights(context.getSource(), false)))
                                .then(Commands.literal("status")
                                        .executes(context -> reportLights(context.getSource())))
                                .then(Commands.literal("entities")
                                        .then(Commands.literal("on")
                                                .executes(context -> setLightEntities(context.getSource(), true)))
                                        .then(Commands.literal("off")
                                                .executes(context -> setLightEntities(context.getSource(), false))))
                                .then(Commands.literal("block_entities")
                                        .then(Commands.literal("on")
                                                .executes(context -> setLightBlockEntities(context.getSource(), true)))
                                        .then(Commands.literal("off")
                                                .executes(context -> setLightBlockEntities(context.getSource(), false))))
                                .then(Commands.literal("shaders")
                                        .then(Commands.literal("on")
                                                .executes(context -> setLightsWithShaders(context.getSource(), true)))
                                        .then(Commands.literal("off")
                                                .executes(context -> setLightsWithShaders(context.getSource(), false))))
                                .then(Commands.literal("interval")
                                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 20))
                                                .executes(context -> setLightInterval(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "ticks"))))))
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
                        "[VRO] Update checks are " + state(enabled)
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
                                + (enabled ? "ON" : "OFF")
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

    private static int setLights(CommandSourceStack source, boolean enabled) {
        ClientOptimizationConfig.setDynamicLights(enabled);
        if (!enabled) {
            DynamicLightEngine.clear();
        }
        return reportLights(source);
    }

    private static int setLightEntities(CommandSourceStack source, boolean enabled) {
        ClientOptimizationConfig.setDynamicLightEntities(enabled);
        return reportLights(source);
    }

    private static int setLightBlockEntities(CommandSourceStack source, boolean enabled) {
        ClientOptimizationConfig.setDynamicLightBlockEntities(enabled);
        return reportLights(source);
    }

    private static int setLightsWithShaders(CommandSourceStack source, boolean enabled) {
        ClientOptimizationConfig.setDynamicLightsWithShaders(enabled);
        return reportLights(source);
    }

    private static int setLightInterval(CommandSourceStack source, int ticks) {
        ClientOptimizationConfig.setDynamicLightUpdateInterval(ticks);
        return reportLights(source);
    }

    private static int reportLights(CommandSourceStack source) {
        DynamicLightEngine.Status status = DynamicLightEngine.status();
        source.sendSuccess(
                new TextComponent(
                        "[VRO] Dynamic lights: configured " + state(status.configured())
                                + ", active " + state(status.active())
                                + ", entities " + state(ClientOptimizationConfig.dynamicLightEntities)
                                + ", block entities " + state(ClientOptimizationConfig.dynamicLightBlockEntities)
                                + ", with shaders " + state(ClientOptimizationConfig.dynamicLightsWithShaders)
                                + ", interval " + ClientOptimizationConfig.dynamicLightUpdateInterval
                                + " tick(s); sources " + status.sources()
                                + ", cells " + status.cells()
                                + ", last rebuilds " + status.rebuilds()
                                + ", definitions " + status.itemDefinitions()
                                + "/" + status.blockEntityDefinitions()
                                + (status.shaderBlocked() ? ". Currently paused by active shaders." : ".")
                ),
                false
        );
        return status.active() ? 1 : 0;
    }

    private static int reportCreate(CommandSourceStack source) {
        if (!ModList.get().isLoaded("create")) {
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
        if (!ModList.get().isLoaded("oculus") || !ModList.get().isLoaded("flywheel")) {
            source.sendSuccess(
                    new TextComponent("[VRO] Create shader compatibility is unavailable; Oculus and Flywheel are required."),
                    false
            );
            return 0;
        }
        source.sendSuccess(
                new TextComponent(
                        "[VRO] Create shader compatibility: configured "
                                + state(ClientOptimizationConfig.createFlywheelShaderCompat)
                                + ", pipeline " + state(FlywheelShaderCompatState.hasPipeline())
                                + ", active " + state(FlywheelShaderCompatState.isRenderPathActive())
                                + ", failed " + (FlywheelShaderCompatState.hasFailed() ? "YES" : "NO")
                                + ". Changes apply immediately."
                ),
                false
        );
        return FlywheelShaderCompatState.isRenderPathActive() ? 1 : 0;
    }
}
