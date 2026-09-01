package dev.hoyin1600p.vault_render_optimization.client.compat.manastealer;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.TextComponent;

public final class ManaStealerCommand {
    private ManaStealerCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("mana_stealer")
                .executes(context -> report(context.getSource()))
                .then(Commands.literal("status")
                        .executes(context -> report(context.getSource())))
                .then(Commands.literal("on")
                        .executes(context -> setEnabled(context.getSource(), true)))
                .then(Commands.literal("off")
                        .executes(context -> setEnabled(context.getSource(), false)))
                .then(Commands.literal("preview")
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1))
                                .executes(context -> startPreview(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "ticks")))
                                .then(Commands.argument("center", Vec3Argument.vec3(false))
                                        .executes(context -> startPreview(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "ticks"),
                                                Vec3Argument.getVec3(context, "center")))))
                        .then(Commands.literal("stop")
                                .executes(context -> stopPreview(context.getSource()))))
                .then(Commands.literal("sigil")
                        .then(Commands.literal("on")
                                .executes(context -> setSigil(context.getSource(), false)))
                        .then(Commands.literal("off")
                                .executes(context -> setSigil(context.getSource(), true))));
    }

    private static int setEnabled(CommandSourceStack source, boolean enabled) {
        ManaStealerVisualConfig.setEnabled(enabled);
        return report(source);
    }

    private static int setSigil(CommandSourceStack source, boolean replace) {
        ManaStealerVisualConfig.setReplaceGroundSigil(replace);
        return report(source);
    }

    private static int startPreview(CommandSourceStack source, int durationTicks) {
        ManaStealerPreviewController.StartResult result = ManaStealerPreviewController.start(durationTicks);
        return reportPreviewStart(source, result);
    }

    private static int startPreview(
            CommandSourceStack source,
            int durationTicks,
            net.minecraft.world.phys.Vec3 center
    ) {
        ManaStealerPreviewController.StartResult result = ManaStealerPreviewController.start(
                durationTicks,
                center
        );
        return reportPreviewStart(source, result);
    }

    private static int reportPreviewStart(
            CommandSourceStack source,
            ManaStealerPreviewController.StartResult result
    ) {
        if (!result.started()) {
            source.sendFailure(new TextComponent("[VRO] Mana Stealer preview could not start: " + result.error()));
            return 0;
        }
        source.sendSuccess(
                new TextComponent(
                        String.format(
                                "[VRO] Started client-only Mana Stealer preview for %d ticks at %.1f, %.1f, %.1f "
                                        + "(radius 6.0; visual/audio only, no mana drain).",
                                result.durationTicks(),
                                result.center().x,
                                result.center().y,
                                result.center().z
                        )
                ),
                false
        );
        return 1;
    }

    private static int stopPreview(CommandSourceStack source) {
        boolean stopped = ManaStealerPreviewController.stop();
        source.sendSuccess(
                new TextComponent(stopped
                        ? "[VRO] Stopped replenishing the Mana Stealer preview; existing orbs will finish naturally."
                        : "[VRO] No Mana Stealer preview is running."),
                false
        );
        return stopped ? 1 : 0;
    }

    private static int report(CommandSourceStack source) {
        source.sendSuccess(
                new TextComponent(
                        "[VRO] Mana Stealer prototype: configured " + state(ManaStealerVisualConfig.configuredEnabled())
                                + ", active " + state(ManaStealerVisualController.canReplace())
                                + ", legacy sigil "
                                + (ManaStealerVisualConfig.replaceGroundSigil() ? "replaced" : "retained")
                                + ", live targets All/Decreased/Minimal "
                                + ManaStealerVisualConfig.allPopulation() + "/"
                                + ManaStealerVisualConfig.decreasedPopulation() + "/"
                                + ManaStealerVisualConfig.minimalPopulation()
                                + ". Changes apply immediately."
                ),
                false
        );
        return 1;
    }

    private static String state(boolean enabled) {
        return enabled ? "ON" : "OFF";
    }
}
