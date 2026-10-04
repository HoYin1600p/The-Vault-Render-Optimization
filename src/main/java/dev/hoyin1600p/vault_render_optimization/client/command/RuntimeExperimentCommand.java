package dev.hoyin1600p.vault_render_optimization.client.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.hoyin1600p.vault_render_optimization.client.hud.HudTextGeometry;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import dev.hoyin1600p.vault_render_optimization.renderertransfer.RetainedBufferPressure;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;

public final class RuntimeExperimentCommand {
    private RuntimeExperimentCommand() { }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("experiments").then(option("hud", true)).then(option("memory", false));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> option(String name, boolean hud) {
        return Commands.literal(name).executes(c -> report(c.getSource(), hud))
                .then(Commands.literal("status").executes(c -> report(c.getSource(), hud)))
                .then(Commands.literal("on").executes(c -> set(c.getSource(), hud, true)))
                .then(Commands.literal("off").executes(c -> set(c.getSource(), hud, false)));
    }

    private static int set(CommandSourceStack source, boolean hud, boolean enabled) {
        if (hud) ClientOptimizationConfig.setHudTextGeometry(enabled);
        else ClientOptimizationConfig.setVertexBufferAdaptiveTrimming(enabled);
        return report(source, hud);
    }

    private static int report(CommandSourceStack source, boolean hud) {
        boolean configured = hud ? ClientOptimizationConfig.hudTextGeometry : ClientOptimizationConfig.vertexBufferAdaptiveTrimming;
        source.sendSuccess(new TextComponent("[VRO] " + (hud ? "HUD glyph reuse" : "CPU-native trimming")
                + ": configured=" + configured + ", compare allows=" + ClientOptimizationConfig.optimizationsEnabled()
                + "; " + (hud ? HudTextGeometry.status() : RetainedBufferPressure.status())
                + (hud ? "; requires compatible plain-string HUD path." : "; requires VRO-EMB-02 ownership; acts at next worker start, not a hard memory cap.")), false);
        return 1;
    }
}
