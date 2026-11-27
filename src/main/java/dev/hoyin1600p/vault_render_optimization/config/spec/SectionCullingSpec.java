package dev.hoyin1600p.vault_render_optimization.config.spec;

import dev.hoyin1600p.vault_render_optimization.util.ConfigKeys;
import dev.hoyin1600p.vault_render_optimization.util.ConfigSections;
import net.minecraftforge.common.ForgeConfigSpec;

/** Defines the section distance culling settings. Called once, in order, by {@code ClientOptimizationConfig}. */
public final class SectionCullingSpec {
    public final ForgeConfigSpec.BooleanValue verticalSectionCulling;
    public final ForgeConfigSpec.IntValue verticalSectionDistance;
    public final ForgeConfigSpec.BooleanValue horizontalSectionCulling;
    public final ForgeConfigSpec.IntValue horizontalSectionDistance;
    public final ForgeConfigSpec.BooleanValue angleSectionOcclusion;

    public SectionCullingSpec(ForgeConfigSpec.Builder builder) {
        builder.push(ConfigSections.SECTION_DISTANCE_CULLING);
        this.verticalSectionCulling = builder
                .comment(
                        "Skip terrain sections outside the vertical distance while rendering.",
                        "This does not unload chunks or alter Distant Horizons storage."
                )
                .define("vertical_enabled", true);
        this.verticalSectionDistance = builder
                .comment("Vertical terrain distance in 16-block sections above and below the camera.")
                .defineInRange("vertical_distance", 12, 1, 64);
        this.horizontalSectionCulling = builder
                .comment(
                        "Skip terrain sections outside a circular horizontal distance.",
                        "Disabled by default to preserve the configured vanilla render distance."
                )
                .define(ConfigKeys.HORIZONTAL_ENABLED, false);
        this.horizontalSectionDistance = builder
                .comment("Horizontal terrain radius in 16-block sections when enabled.")
                .defineInRange("horizontal_distance", 24, 1, 64);
        this.angleSectionOcclusion = builder
                .comment(
                        "On validated Embeddium builds, mask opposing-face chunk visibility paths that are",
                        "geometrically unreachable from the perspective camera angle (Sodium PR #2811 / Flerovium).",
                        "Zero visual difference; skipped in spectator mode, Oculus shadow passes and Compare Mode."
                )
                .define(ConfigKeys.ANGLE_SECTION_OCCLUSION, true);
        builder.pop();
    }
}
