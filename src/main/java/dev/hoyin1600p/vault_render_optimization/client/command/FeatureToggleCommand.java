package dev.hoyin1600p.vault_render_optimization.client.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.hoyin1600p.vault_render_optimization.client.diagnostics.AllocationProbe;
import dev.hoyin1600p.vault_render_optimization.client.render.FastloadFrustum;
import dev.hoyin1600p.vault_render_optimization.client.render.FrustumPlanes;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import dev.hoyin1600p.vault_render_optimization.util.CommandText;
import dev.hoyin1600p.vault_render_optimization.util.ConfigKeys;
import dev.hoyin1600p.vault_render_optimization.util.ModIds;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.fml.ModList;

/**
 * {@code /vro feature <name> on|off|status} switches single optimizations live, for one-launch A/B
 * checks, and {@code /vro alloc start|report} measures render-thread allocation per frame.
 */
public final class FeatureToggleCommand {
    private record Feature(String name, String config, BooleanSupplier get, Consumer<Boolean> set) {
    }

    private static final Feature[] FEATURES = {
            new Feature("collision", ConfigKeys.PARTICLE_COLLISION_CACHE,
                    () -> ClientOptimizationConfig.particleCollisionCache, ClientOptimizationConfig::setParticleCollisionCache),
            new Feature("compaction", ConfigKeys.PARTICLE_TICK_COMPACTION,
                    () -> ClientOptimizationConfig.particleTickCompaction, ClientOptimizationConfig::setParticleTickCompaction),
            new Feature("random", ConfigKeys.PARTICLE_SHARED_RANDOM,
                    () -> ClientOptimizationConfig.particleSharedRandom, ClientOptimizationConfig::setParticleSharedRandom),
            new Feature("provider", ConfigKeys.PARTICLE_PROVIDER_CACHE,
                    () -> ClientOptimizationConfig.particleProviderCache, ClientOptimizationConfig::setParticleProviderCache),
            new Feature("frustum", ConfigKeys.ALLOCATION_FREE_FRUSTUM,
                    () -> ClientOptimizationConfig.allocationFreeFrustum, ClientOptimizationConfig::setAllocationFreeFrustum),
            new Feature("gpushaders", ConfigKeys.GPU_ENTITY_MODELS_WITH_SHADERS,
                    () -> ClientOptimizationConfig.gpuEntityModelsWithShaders, ClientOptimizationConfig::setGpuEntityModelsWithShaders),
            new Feature("gpuitems", ConfigKeys.GPU_ITEMS,
                    () -> ClientOptimizationConfig.gpuItems, ClientOptimizationConfig::setGpuItems),
            new Feature("gpushaderparticles", ConfigKeys.GPU_PARTICLES_WITH_SHADERS,
                    () -> ClientOptimizationConfig.gpuParticlesWithShaders, ClientOptimizationConfig::setGpuParticlesWithShaders),
            new Feature("gpuparticles", ConfigKeys.GPU_PARTICLES,
                    () -> ClientOptimizationConfig.gpuParticles, ClientOptimizationConfig::setGpuParticles),
            new Feature("fastload", ConfigKeys.FASTLOAD_FRUSTUM_BYPASS,
                    () -> ClientOptimizationConfig.fastloadFrustumBypass, ClientOptimizationConfig::setFastloadFrustumBypass),
    };

    private FeatureToggleCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("feature")
                .executes(c -> list(c.getSource()))
                .then(Commands.literal("list").executes(c -> list(c.getSource())));
        for (Feature feature : FEATURES) {
            LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(feature.name())
                    .executes(c -> status(c.getSource(), feature))
                    .then(Commands.literal("status").executes(c -> status(c.getSource(), feature)))
                    .then(Commands.literal("on").executes(c -> set(c.getSource(), feature, true)))
                    .then(Commands.literal("off").executes(c -> set(c.getSource(), feature, false)));
            if (feature.name().equals("frustum")) {
                node.then(Commands.literal("verify")
                        .executes(c -> frustumVerify(c.getSource()))
                        .then(Commands.literal("on").executes(c -> {
                            FrustumPlanes.verifyChecks = 0;
                            FrustumPlanes.verifyMismatches = 0;
                            FrustumPlanes.verify = true;
                            return frustumVerify(c.getSource());
                        }))
                        .then(Commands.literal("off").executes(c -> {
                            FrustumPlanes.verify = false;
                            return frustumVerify(c.getSource());
                        })));
            }
            root.then(node);
        }
        return root;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildAlloc() {
        return Commands.literal("alloc")
                .executes(c -> send(c.getSource(), AllocationProbe.report()))
                .then(Commands.literal("start").executes(c -> send(c.getSource(), AllocationProbe.start())))
                .then(Commands.literal("report").executes(c -> send(c.getSource(), AllocationProbe.report())))
                .then(Commands.literal("resources").executes(c -> send(c.getSource(), resources())));
    }

    /** Leak counters for the Create mesh and Oculus program-cache fixes; each only when its mod is loaded. */
    private static String resources() {
        StringBuilder text = new StringBuilder("Resources:");
        ModList mods = ModList.get();
        text.append(mods.isLoaded(ModIds.CREATE) ? " " + CreateCounters.text() : " Create not installed");
        text.append(mods.isLoaded(ModIds.OCULUS) && mods.isLoaded(ModIds.FLYWHEEL) ? "; " + OculusCounters.text() : "; Oculus/Flywheel not installed");
        return text.toString();
    }

    // Holders keep the optional mods' classes from loading unless the mod is present.
    private static final class CreateCounters {
        static String text() {
            return dev.hoyin1600p.vault_render_optimization.client.create.SectionedContraptionRenderer.liveSections();
        }
    }

    private static final class OculusCounters {
        static String text() {
            return dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.compiler.IrisProgramCompilerBase.cachedPipelines();
        }
    }

    private static int list(CommandSourceStack source) {
        StringBuilder text = new StringBuilder("Features:");
        for (Feature feature : FEATURES) {
            text.append(' ').append(feature.name()).append('=').append(feature.get().getAsBoolean() ? "on" : "off");
        }
        text.append("; compare allows=").append(ClientOptimizationConfig.optimizationsEnabled());
        text.append("; ").append(FastloadFrustum.status());
        return send(source, text.toString());
    }

    private static int status(CommandSourceStack source, Feature feature) {
        return send(source, "Feature " + feature.name() + " (" + feature.config() + "): "
                + (feature.get().getAsBoolean() ? "on" : "off")
                + ", compare allows=" + ClientOptimizationConfig.optimizationsEnabled());
    }

    private static int set(CommandSourceStack source, Feature feature, boolean enabled) {
        feature.set().accept(enabled);
        return status(source, feature);
    }

    private static int frustumVerify(CommandSourceStack source) {
        return send(source, "Frustum verify " + CommandText.onOff(FrustumPlanes.verify) + ": boxes checked "
                + FrustumPlanes.verifyChecks + ", mismatches " + FrustumPlanes.verifyMismatches);
    }

    private static int send(CommandSourceStack source, String line) {
        source.sendSuccess(new TextComponent("[VRO] " + line), false);
        CommandText.logCommandLine(line);
        return 1;
    }
}
