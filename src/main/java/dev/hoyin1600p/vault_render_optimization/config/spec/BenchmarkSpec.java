package dev.hoyin1600p.vault_render_optimization.config.spec;

import dev.hoyin1600p.vault_render_optimization.util.ConfigKeys;
import dev.hoyin1600p.vault_render_optimization.util.ConfigSections;
import net.minecraftforge.common.ForgeConfigSpec;

/** Defines the benchmark (Compare Mode) setting. Called once, in order, by {@code ClientOptimizationConfig}. */
public final class BenchmarkSpec {
    public final ForgeConfigSpec.BooleanValue compareMode;

    public BenchmarkSpec(ForgeConfigSpec.Builder builder) {
        builder.push(ConfigSections.BENCHMARK);
        this.compareMode = builder
                .comment(
                        "Disable every VRO performance optimization for an in-game comparison baseline.",
                        "Client crash guards, cleanup, and key compatibility remain active.",
                        "The /vro compare command changes and saves this setting."
                )
                .define(ConfigKeys.COMPARE_MODE, false);
        builder.pop();
    }
}
