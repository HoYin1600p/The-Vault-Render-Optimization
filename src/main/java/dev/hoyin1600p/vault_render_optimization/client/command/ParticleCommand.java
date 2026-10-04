package dev.hoyin1600p.vault_render_optimization.client.command;

import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleDiagnostics;
import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleSharedLightCache;
import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleCollisionState;
import dev.hoyin1600p.vault_render_optimization.client.diagnostics.ParticleStress;

import dev.hoyin1600p.vault_render_optimization.config.model.ParticleBillboardOwner;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import dev.hoyin1600p.vault_render_optimization.util.CommandText;
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
                                .executes(context -> resetDiagnostics(context.getSource()))))
                .then(Commands.literal("stress")
                        .then(Commands.literal("status").executes(context -> stressStatus(context.getSource())))
                        .then(Commands.literal("stop").executes(context -> {
                            ParticleStress.stop();
                            return stressStatus(context.getSource());
                        }))
                        .then(stressKind("nova", ParticleStress.Kind.NOVA))
                        .then(stressKind("frost", ParticleStress.Kind.FROST)))
                .then(Commands.literal("new")
                        .then(Commands.literal("on").executes(context -> setNew(context.getSource(), true)))
                        .then(Commands.literal("off").executes(context -> setNew(context.getSource(), false))))
                .then(Commands.literal("collision")
                        .then(Commands.literal("on").executes(context -> setCollision(context.getSource(), true)))
                        .then(Commands.literal("off").executes(context -> setCollision(context.getSource(), false)))
                        .then(Commands.literal("verify")
                                .then(Commands.literal("on").executes(context -> setCollisionVerify(context.getSource(), true)))
                                .then(Commands.literal("off").executes(context -> setCollisionVerify(context.getSource(), false)))));
    }

    /** A/B switch for the new particle set (collision cache, tick compaction, shared random, provider cache). */
    private static int setNew(CommandSourceStack source, boolean enabled) {
        ClientOptimizationConfig.setNewParticleOptimizations(enabled);
        String text = "[VRO] Particle optimizations (collision cache, tick compaction, shared random, "
                + "provider cache) " + (enabled ? "enabled" : "disabled") + " and saved.";
        source.sendSuccess(new TextComponent(text), false);
        CommandText.logCommandLine(text);
        return report(source);
    }

    // /vro particles stress nova|frost <casts per second> <seconds> [radius]
    private static LiteralArgumentBuilder<CommandSourceStack> stressKind(String name, ParticleStress.Kind kind) {
        return Commands.literal(name)
                .then(Commands.argument("casts_per_second", IntegerArgumentType.integer(1, 400))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 600))
                                .executes(context -> startStress(context, kind, 10.0F))
                                .then(Commands.argument("radius", FloatArgumentType.floatArg(1.0F, 20.0F))
                                        .executes(context -> startStress(context, kind,
                                                FloatArgumentType.getFloat(context, "radius"))))));
    }

    private static int startStress(CommandContext<CommandSourceStack> context, ParticleStress.Kind kind, float radius) {
        String result = ParticleStress.start(kind,
                IntegerArgumentType.getInteger(context, "casts_per_second"),
                IntegerArgumentType.getInteger(context, "seconds"), radius);
        context.getSource().sendSuccess(new TextComponent("[VRO] Particle stress: " + result
                + ". Client-side replay of real cast particles; nothing is sent to the server."), false);
        return 1;
    }

    private static int stressStatus(CommandSourceStack source) {
        source.sendSuccess(new TextComponent("[VRO] Particle stress: " + ParticleStress.status()), false);
        return 1;
    }

    private static int setCollision(CommandSourceStack source, boolean enabled) {
        ClientOptimizationConfig.setParticleCollisionCache(enabled);
        return report(source);
    }

    private static int setCollisionVerify(CommandSourceStack source, boolean enabled) {
        // Not saved: verification also runs vanilla collision, so it costs more than either path alone.
        ParticleCollisionState.setVerify(enabled);
        source.sendSuccess(new TextComponent("[VRO] Particle collision verification " + (enabled
                ? "ON: every cached result is compared with vanilla and vanilla's result is used."
                : "OFF.")), false);
        return report(source);
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
        source.sendSuccess(new TextComponent("[VRO] Particle collision: " + ParticleCollisionState.status()), false);
        source.sendSuccess(
                new TextComponent(
                        "[VRO] Particles: billboards " + CommandText.onOff(ClientOptimizationConfig.particleBillboardFastPath)
                                + ", configured owner "
                                + ClientOptimizationConfig.particleBillboardOwner.name().toLowerCase(Locale.ROOT)
                                + ", resolved owner " + snapshot.billboardOwner()
                                + ", renderer " + availability(snapshot.rendererAvailable())
                                + ", shared light " + CommandText.onOff(ClientOptimizationConfig.particleSharedLightCache)
                                + ", diagnostics " + CommandText.onOff(snapshot.enabled())
                                + ", Compare Mode " + CommandText.onOff(ClientOptimizationConfig.compareModeEnabled())
                                + ". Changes apply immediately."
                ),
                false
        );

        if (!snapshot.enabled()) {
            return 1;
        }

        source.sendSuccess(
                new TextComponent(
                        "[VRO] Particle census (sampled every 250ms): queued " + snapshot.queuedParticles()
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
