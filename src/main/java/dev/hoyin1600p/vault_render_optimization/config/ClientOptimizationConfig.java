package dev.hoyin1600p.vault_render_optimization.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public final class ClientOptimizationConfig {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue PARTICLE_LIGHT_CACHE;
    private static final ForgeConfigSpec.BooleanValue EMPTY_PARTICLE_RENDER_SKIP;
    private static final ForgeConfigSpec.BooleanValue EMPTY_TOAST_RENDER_SKIP;
    private static final ForgeConfigSpec.BooleanValue INACTIVE_TUTORIAL_SKIP;
    private static final ForgeConfigSpec.BooleanValue EMPTY_DEBUG_RENDER_SKIP;
    private static final ForgeConfigSpec.BooleanValue ENTITY_RENDERER_CACHE;
    private static final ForgeConfigSpec.BooleanValue BLOCK_ENTITY_RENDERER_CACHE;

    public static volatile boolean particleLightCache = true;
    public static volatile boolean emptyParticleRenderSkip = true;
    public static volatile boolean emptyToastRenderSkip = true;
    public static volatile boolean inactiveTutorialSkip = true;
    public static volatile boolean emptyDebugRenderSkip = true;
    public static volatile boolean entityRendererCache = true;
    public static volatile boolean blockEntityRendererCache = true;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("render_fast_paths");
        PARTICLE_LIGHT_CACHE = builder
                .comment("Cache unchanged particle light lookups for one client tick.")
                .define("particle_light_cache", true);
        EMPTY_PARTICLE_RENDER_SKIP = builder
                .comment("Skip particle renderer setup when every particle queue is empty.")
                .define("skip_empty_particle_render", true);
        EMPTY_TOAST_RENDER_SKIP = builder
                .comment("Skip toast renderer work when no toast is queued or visible.")
                .define("skip_empty_toast_render", true);
        EMPTY_DEBUG_RENDER_SKIP = builder
                .comment("Skip debug renderer work when no supported debug overlay is active.")
                .define("skip_empty_debug_render", true);
        builder.pop();

        builder.push("client_tick_fast_paths");
        INACTIVE_TUTORIAL_SKIP = builder
                .comment("Skip the completed tutorial's no-op tick when no timed tutorial toast exists.")
                .define("skip_inactive_tutorial", true);
        builder.pop();

        builder.push("renderer_lookup_caches");
        ENTITY_RENDERER_CACHE = builder
                .comment("Cache non-player entity renderers on their EntityType and refresh on resource reload.")
                .define("entity_renderer_cache", true);
        BLOCK_ENTITY_RENDERER_CACHE = builder
                .comment("Cache block entity renderers on their BlockEntityType and refresh on resource reload.")
                .define("block_entity_renderer_cache", true);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientOptimizationConfig() {
    }

    public static void onLoading(ModConfigEvent.Loading event) {
        bake(event.getConfig());
    }

    public static void onReloading(ModConfigEvent.Reloading event) {
        bake(event.getConfig());
    }

    private static void bake(ModConfig config) {
        if (config.getSpec() != SPEC) {
            return;
        }

        particleLightCache = PARTICLE_LIGHT_CACHE.get();
        emptyParticleRenderSkip = EMPTY_PARTICLE_RENDER_SKIP.get();
        emptyToastRenderSkip = EMPTY_TOAST_RENDER_SKIP.get();
        inactiveTutorialSkip = INACTIVE_TUTORIAL_SKIP.get();
        emptyDebugRenderSkip = EMPTY_DEBUG_RENDER_SKIP.get();
        entityRendererCache = ENTITY_RENDERER_CACHE.get();
        blockEntityRendererCache = BLOCK_ENTITY_RENDERER_CACHE.get();
    }
}
