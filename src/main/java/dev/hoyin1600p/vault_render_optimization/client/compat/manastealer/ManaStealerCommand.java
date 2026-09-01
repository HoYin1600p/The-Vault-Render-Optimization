package dev.hoyin1600p.vault_render_optimization.client.compat.manastealer;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
