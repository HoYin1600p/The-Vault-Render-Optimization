package dev.hoyin1600p.vault_render_optimization.config.spec;

import dev.hoyin1600p.vault_render_optimization.backport.RenderBackportFeature;
import dev.hoyin1600p.vault_render_optimization.util.ConfigSections;
import java.util.EnumMap;
import net.minecraftforge.common.ForgeConfigSpec;

/** Defines the ModernFix backport toggles, registered in {@code options}. Called once, in order, by {@code ClientOptimizationConfig}. */
public final class BackportSpec {

    public BackportSpec(ForgeConfigSpec.Builder builder, EnumMap<RenderBackportFeature, ForgeConfigSpec.BooleanValue> options) {
        builder.push(ConfigSections.MODERNFIX_BACKPORTS);
        for (RenderBackportFeature feature : RenderBackportFeature.values()) {
            options.put(
                    feature,
                    builder.comment(
                            "Enable VRO's " + feature.displayName() + " backport when VRO owns it.",
                            "This option is evaluated during startup and requires a game restart.",
                            "VRO yields to the current VH Accelerator implementation and to an active ModernFix implementation."
                    ).define(feature.configKey(), true)
            );
        }
        builder.pop();
    }
}
