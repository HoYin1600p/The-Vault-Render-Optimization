package dev.hoyin1600p.vault_render_optimization.client.particle;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;

public final class ParticleCommand {
    private ParticleCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("particles")
                .executes(context -> report(context.getSource()))
                .then(Commands.literal("status")
                        .executes(context -> report(context.getSource())))
                .then(Commands.literal("billboards")
                        .then(Commands.literal("on")
                                .executes(context -> setBillboards(context.getSource(), true)))
                        .then(Commands.literal("off")
                                .executes(context -> setBillboards(context.getSource(), false))))
                .then(Commands.literal("owner")
                        .then(Commands.literal("auto")
                                .executes(context -> setOwner(
                                        context.getSource(), ParticleBillboardOwner.AUTO)))
                        .then(Commands.literal("renderer")
                                .executes(context -> setOwner(
                                        context.getSource(), ParticleBillboardOwner.RENDERER)))
                        .then(Commands.literal("vro")
                                .executes(context -> setOwner(
                                        context.getSource(), ParticleBillboardOwner.VRO))))
                .then(Commands.literal("shared_light")
                        .then(Commands.literal("on")
                                .executes(context -> setSharedLight(context.getSource(), true)))
                        .then(Commands.literal("off")
                                .executes(context -> setSharedLight(context.getSource(), false))))
                .then(Commands.literal("diagnostics")
                        .then(Commands.literal("on")
                                .executes(context -> setDiagnostics(context.getSource(), true)))
                        .then(Commands.literal("off")
                                .executes(context -> setDiagnostics(context.getSource(), false)))
                        .then(Commands.literal("reset")
                                .executes(context -> resetDiagnostics(context.getSource()))));
    }

    private static int setBillboards(CommandSourceStack source, boolean enabled) {
        ClientOptimizationConfig.setParticleBillboardFastPath(enabled);
        return report(source);
    }

    private static int setOwner(CommandSourceStack source, ParticleBillboardOwner owner) {
        ClientOptimizationConfig.setParticleBillboardOwner(owner);
        return report(source);
    }

    private static int setSharedLight(CommandSourceStack source, boolean enabled) {
        ClientOptimizationConfig.setParticleSharedLightCache(enabled);
        ParticleSharedLightCache.clearCurrentThread();
        return report(source);
    }

    private static int setDiagnostics(CommandSourceStack source, boolean enabled) {
        if (enabled && !ClientOptimizationConfig.particleDiagnostics) {
            ParticleDiagnostics.reset();
        }
        ClientOptimizationConfig.setParticleDiagnostics(enabled);
        return report(source);
    }

    private static int resetDiagnostics(CommandSourceStack source) {
        ParticleDiagnostics.reset();
        source.sendSuccess(new TextComponent("[VRO] Particle diagnostics reset."), false);
        return report(source);
    }

    private static int report(CommandSourceStack source) {
        ParticleDiagnostics.Snapshot snapshot = ParticleDiagnostics.snapshot();
        source.sendSuccess(
                new TextComponent(
                        "[VRO] Particles: billboards " + state(ClientOptimizationConfig.particleBillboardFastPath)
                                + ", configured owner "
                                + ClientOptimizationConfig.particleBillboardOwner.name().toLowerCase(Locale.ROOT)
                                + ", resolved owner " + snapshot.billboardOwner()
                                + ", renderer " + availability(snapshot.rendererAvailable())
                                + ", shared light " + state(ClientOptimizationConfig.particleSharedLightCache)
                                + ", diagnostics " + state(snapshot.enabled())
                                + ", Compare Mode " + state(ClientOptimizationConfig.compareModeEnabled())
                                + ". Changes apply immediately."
                ),
                false
        );

        if (!snapshot.enabled()) {
            return 1;
        }

        source.sendSuccess(
                new TextComponent(
                        "[VRO] Particle sample: queued " + snapshot.queuedParticles()
                                + "; VRO writes packed/portable " + snapshot.vroRendererWrites()
                                + "/" + snapshot.vroPortableWrites()
                                + "; renderer passthroughs " + snapshot.rendererPassthroughs()
                                + "; light hits particle/shared/lookups " + snapshot.particleLightHits()
                                + "/" + snapshot.sharedLightHits()
                                + "/" + snapshot.lightLookups()
                                + "; empty render skips " + snapshot.emptyRenderSkips() + "."
                ),
                false
        );
        source.sendSuccess(
                new TextComponent(
                        "[VRO] Particle timings ms last/average: render "
                                + milliseconds(snapshot.lastRenderNanos()) + "/"
                                + milliseconds(snapshot.averageRenderNanos())
                                + " over " + snapshot.renderCalls()
                                + " calls; tick " + milliseconds(snapshot.lastTickNanos()) + "/"
                                + milliseconds(snapshot.averageTickNanos())
                                + " over " + snapshot.tickCalls() + " calls."
                ),
                false
        );

        snapshot.queuedClasses().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .forEach(entry -> source.sendSuccess(
                        new TextComponent(
                                "[VRO] Queued " + shortName(entry.getKey()) + ": " + entry.getValue()
                        ),
                        false
                ));
        return 1;
    }

    private static String state(boolean enabled) {
        return enabled ? "ON" : "OFF";
    }

    private static String availability(boolean available) {
        return available ? "available" : "absent";
    }

    private static String milliseconds(long nanos) {
        return String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0D);
    }

    private static String shortName(String className) {
        int separator = className.lastIndexOf('.');
        return separator < 0 ? className : className.substring(separator + 1);
    }
}
