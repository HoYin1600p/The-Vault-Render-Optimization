package dev.hoyin1600p.vault_render_optimization.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import dev.hoyin1600p.vault_render_optimization.client.render.AllocationProbe;
import dev.hoyin1600p.vault_render_optimization.client.render.FrustumPlanes;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;

/**
 * {@code /vro feature <name> on|off|status} switches single 0.5.0 optimizations live, for one-launch A/B
 * checks, and {@code /vro alloc start|report} measures render-thread allocation per frame.
 */
public final class FeatureToggleCommand {
    private record Feature(String name, String config, BooleanSupplier get, Consumer<Boolean> set) {
    }

    private static final Feature[] FEATURES = {
            new Feature("collision", "particle_collision_cache",
                    () -> ClientOptimizationConfig.particleCollisionCache, ClientOptimizationConfig::setParticleCollisionCache),
            new Feature("compaction", "particle_tick_compaction",
                    () -> ClientOptimizationConfig.particleTickCompaction, ClientOptimizationConfig::setParticleTickCompaction),
            new Feature("random", "particle_shared_random",
                    () -> ClientOptimizationConfig.particleSharedRandom, ClientOptimizationConfig::setParticleSharedRandom),
            new Feature("provider", "particle_provider_cache",
                    () -> ClientOptimizationConfig.particleProviderCache, ClientOptimizationConfig::setParticleProviderCache),
            new Feature("frustum", "allocation_free_frustum",
                    () -> ClientOptimizationConfig.allocationFreeFrustum, ClientOptimizationConfig::setAllocationFreeFrustum),
            new Feature("gpushaders", "gpu_entity_models_with_shaders",
                    () -> ClientOptimizationConfig.gpuEntityModelsWithShaders, ClientOptimizationConfig::setGpuEntityModelsWithShaders),
            new Feature("gpuitems", "gpu_items",
                    () -> ClientOptimizationConfig.gpuItems, ClientOptimizationConfig::setGpuItems),
            new Feature("gpushaderparticles", "gpu_particles_with_shaders",
                    () -> ClientOptimizationConfig.gpuParticlesWithShaders, ClientOptimizationConfig::setGpuParticlesWithShaders),
            new Feature("gpuparticles", "gpu_particles",
                    () -> ClientOptimizationConfig.gpuParticles, ClientOptimizationConfig::setGpuParticles),
            new Feature("fastload", "fastload_frustum_bypass",
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
        net.minecraftforge.fml.ModList mods = net.minecraftforge.fml.ModList.get();
        text.append(mods.isLoaded("create") ? " " + CreateCounters.text() : " Create not installed");
        text.append(mods.isLoaded("oculus") && mods.isLoaded("flywheel") ? "; " + OculusCounters.text() : "; Oculus/Flywheel not installed");
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
        text.append("; ").append(dev.hoyin1600p.vault_render_optimization.client.render.FastloadFrustum.status());
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
        return send(source, "Frustum verify " + (FrustumPlanes.verify ? "ON" : "OFF") + ": boxes checked "
                + FrustumPlanes.verifyChecks + ", mismatches " + FrustumPlanes.verifyMismatches);
    }

    private static int send(CommandSourceStack source, String line) {
        source.sendSuccess(new TextComponent("[VRO] " + line), false);
        VaultRenderOptimization.LOGGER.info("[command] {}", line);
        return 1;
    }
}
