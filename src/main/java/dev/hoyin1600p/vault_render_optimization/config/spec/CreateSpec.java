package dev.hoyin1600p.vault_render_optimization.config.spec;

import dev.hoyin1600p.vault_render_optimization.util.ConfigSections;
import net.minecraftforge.common.ForgeConfigSpec;

/** Defines the Create rendering settings. Called once, in order, by {@code ClientOptimizationConfig}. */
public final class CreateSpec {
    public final ForgeConfigSpec.BooleanValue createEmptyBufferFlushSkip;
    public final ForgeConfigSpec.BooleanValue createBlockEntityCulling;
    public final ForgeConfigSpec.BooleanValue createActorCulling;
    public final ForgeConfigSpec.BooleanValue createSectionedContraptionMeshes;
    public final ForgeConfigSpec.IntValue createSectionedMeshThreshold;
    public final ForgeConfigSpec.BooleanValue createSmartRenderBounds;
    public final ForgeConfigSpec.BooleanValue createFlywheelAutoEnable;
    public final ForgeConfigSpec.BooleanValue createFlywheelShaderCompat;

    public CreateSpec(ForgeConfigSpec.Builder builder) {
        builder.push(ConfigSections.CREATE_RENDERING);
        this.createEmptyBufferFlushSkip = builder
                .comment("Avoid flushing Minecraft's shared render buffers for Create contraptions that rendered no special block entities.")
                .define("skip_empty_contraption_buffer_flush", true);
        this.createBlockEntityCulling = builder
                .comment("Frustum-cull special block entities inside visible Create contraptions.")
                .define("contraption_block_entity_culling", true);
        this.createActorCulling = builder
                .comment("Frustum-cull movement actors inside visible Create contraptions.")
                .define("contraption_actor_culling", true);
        this.createSectionedContraptionMeshes = builder
                .comment(
                        "Split large Create contraption meshes into local 16-block sections for frustum culling.",
                        "This is geometric culling only; it does not reduce detail or render distance."
                )
                .define("sectioned_contraption_meshes", true);
        this.createSectionedMeshThreshold = builder
                .comment("Minimum rendered block count before a contraption uses sectioned meshes.")
                .defineInRange("sectioned_mesh_block_threshold", 512, 128, 16384);
        this.createSmartRenderBounds = builder
                .comment("Use directional cached render bounds for supported Create machinery.")
                .define("smart_machinery_render_bounds", true);
        this.createFlywheelAutoEnable = builder
                .comment(
                        "Turns on Flywheel's instancing renderer for this session when the pack has it set to OFF.",
                        "Flywheel's own config file is never changed; turning this off returns to the pack's setting",
                        "immediately. Unsupported GPUs and shader integration failures still fall back safely."
                )
                .define("auto_enable_flywheel_instancing", true);
        this.createFlywheelShaderCompat = builder
                .comment(
                        "Keep Flywheel's instancing backend available with Oculus shaders.",
                        "Tested with Oculus 1.6.x, Rubidium/Embeddium, and Flywheel 0.6.11.",
                        "Unsupported or incomplete mod stacks ignore this option."
                )
                .define("flywheel_shader_compat", true);
        builder.pop();
    }
}
