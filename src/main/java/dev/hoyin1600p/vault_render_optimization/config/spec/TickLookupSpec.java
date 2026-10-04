package dev.hoyin1600p.vault_render_optimization.config.spec;

import dev.hoyin1600p.vault_render_optimization.util.ConfigSections;
import net.minecraftforge.common.ForgeConfigSpec;

/** Defines the client tick fast path and renderer lookup cache settings. Called once, in order, by {@code ClientOptimizationConfig}. */
public final class TickLookupSpec {
    public final ForgeConfigSpec.BooleanValue inactiveTutorialSkip;
    public final ForgeConfigSpec.BooleanValue entityRendererCache;
    public final ForgeConfigSpec.BooleanValue blockEntityRendererCache;

    public TickLookupSpec(ForgeConfigSpec.Builder builder) {
        builder.push(ConfigSections.CLIENT_TICK_FAST_PATHS);
        this.inactiveTutorialSkip = builder
                .comment("Skip the completed tutorial's no-op tick when no timed tutorial toast exists.")
                .define("skip_inactive_tutorial", true);
        builder.pop();

        builder.push(ConfigSections.RENDERER_LOOKUP_CACHES);
        this.entityRendererCache = builder
                .comment("Cache non-player entity renderers on their EntityType and refresh on resource reload.")
                .define("entity_renderer_cache", true);
        this.blockEntityRendererCache = builder
                .comment("Cache block entity renderers on their BlockEntityType and refresh on resource reload.")
                .define("block_entity_renderer_cache", true);
        builder.pop();
    }
}
