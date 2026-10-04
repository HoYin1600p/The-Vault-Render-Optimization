package dev.hoyin1600p.vault_render_optimization.config.spec;

import net.minecraftforge.common.ForgeConfigSpec;

/** Defines the empty toast and debug render skips (defined inside the render_fast_paths section). Called once, in order, by {@code ClientOptimizationConfig}. */
public final class EmptyRenderSkipSpec {
    public final ForgeConfigSpec.BooleanValue emptyToastRenderSkip;
    public final ForgeConfigSpec.BooleanValue emptyDebugRenderSkip;

    public EmptyRenderSkipSpec(ForgeConfigSpec.Builder builder) {
        this.emptyToastRenderSkip = builder
                .comment("Skip toast renderer work when no toast is queued or visible.")
                .define("skip_empty_toast_render", true);
        this.emptyDebugRenderSkip = builder
                .comment("Skip debug renderer work when no supported debug overlay is active.")
                .define("skip_empty_debug_render", true);
    }
}
