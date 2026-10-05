package dev.hoyin1600p.vault_render_optimization.config.spec;

import dev.hoyin1600p.vault_render_optimization.util.ConfigKeys;
import net.minecraftforge.common.ForgeConfigSpec;

/** Defines the GPU entity, particle and item settings (defined inside the render_fast_paths section). Called once, in order, by {@code ClientOptimizationConfig}. */
public final class GpuSpec {
    public final ForgeConfigSpec.BooleanValue gpuEntityModels;
    public final ForgeConfigSpec.BooleanValue gpuParticles;
    public final ForgeConfigSpec.BooleanValue gpuEntityModelsWithShaders;
    public final ForgeConfigSpec.BooleanValue gpuParticlesWithShaders;
    public final ForgeConfigSpec.BooleanValue gpuItems;

    public GpuSpec(ForgeConfigSpec.Builder builder) {
        this.gpuEntityModels = builder.comment(
                "Expand entity model cubes on the GPU. Each model part reserves its vertices in the",
                "vanilla entity buffer and a compute shader writes them into the uploaded vertex buffer right before",
                "vanilla's own draw, with vanilla's exact float arithmetic (bit-for-bit self-test at startup).",
                "Needs OpenGL 4.3 or the ARB compute extensions; pauses with an Oculus shader pack (see",
                "gpu_entity_models_with_shaders), in Compare",
                "Mode, on a failed self-test or mixin audit. /vro gpuentity status explains the current state."
        ).define(ConfigKeys.GPU_ENTITY_MODELS, false);
        this.gpuParticles = builder.comment(
                "With gpu_entity_models active: billboard particles reserve their four vertices and the same compute",
                "path writes them with VRO's exact billboard arithmetic (own startup self-test). Every particle is",
                "still drawn; anything unusual stays on the CPU writer."
        ).define(ConfigKeys.GPU_PARTICLES, false);
        this.gpuEntityModelsWithShaders = builder.comment(
                "Experimental; mainly for slower CPUs. With gpu_entity_models active and an Oculus shader pack on,",
                "entity models and particles",
                "stay on the GPU path: a second program writes Oculus' extended entity vertices (face normal, tangent,",
                "mid UV, entity IDs) exactly as Oculus computes them. It saves CPU time, but shader packs usually make",
                "the game GPU-bound, where the extra compute passes cost more than they save; off by default."
        ).define(ConfigKeys.GPU_ENTITY_MODELS_WITH_SHADERS, false);
        this.gpuParticlesWithShaders = builder.comment(
                "With gpu_particles active and an Oculus shader pack on, keep only particles on the GPU (they keep",
                "the vanilla particle format under shader packs). gpu_entity_models_with_shaders includes this."
        ).define(ConfigKeys.GPU_PARTICLES_WITH_SHADERS, false);
        this.gpuItems = builder.comment(
                "With gpu_entity_models active: solid and cutout block items (dropped, in frames, held) reserve their",
                "vertices and the compute path writes them exactly as the installed item writer (Forge or Embeddium)",
                "would (own self-test). Flat, translucent and glinting items always stay on the CPU."
        ).define(ConfigKeys.GPU_ITEMS, false);
    }
}
