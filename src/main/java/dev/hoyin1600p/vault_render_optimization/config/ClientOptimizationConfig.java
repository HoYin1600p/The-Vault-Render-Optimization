package dev.hoyin1600p.vault_render_optimization.config;

import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import dev.hoyin1600p.vault_render_optimization.backport.RenderBackportFeature;
import dev.hoyin1600p.vault_render_optimization.renderertransfer.RendererTransferFeature;
import dev.hoyin1600p.vault_render_optimization.cache.VaultGearRenderCache;
import dev.hoyin1600p.vault_render_optimization.cache.VaultToolRenderCache;
import dev.hoyin1600p.vault_render_optimization.client.update.UpdateNoticeFilter;
import dev.hoyin1600p.vault_render_optimization.client.update.UpdateNoticeService;
import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleBillboardOwner;
import java.util.EnumMap;
import java.util.Map;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public final class ClientOptimizationConfig {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue COMPARE_MODE;
    private static final EnumMap<RenderBackportFeature, ForgeConfigSpec.BooleanValue>
            RENDER_BACKPORT_OPTIONS = new EnumMap<>(RenderBackportFeature.class);
    private static final EnumMap<RendererTransferFeature, ForgeConfigSpec.BooleanValue>
            RENDERER_TRANSFER_OPTIONS = new EnumMap<>(RendererTransferFeature.class);
    private static final ForgeConfigSpec.IntValue VERTEX_BUFFER_MAX_RETAINED_MIB;
    private static final ForgeConfigSpec.IntValue ASYNC_ARENA_GROWTH_DIVISOR;
    private static final ForgeConfigSpec.IntValue ASYNC_ARENA_MAX_HEADROOM_MIB;
    private static final ForgeConfigSpec.BooleanValue UPDATE_CHECKS;
    private static final ForgeConfigSpec.EnumValue<UpdateNoticeFilter> UPDATE_NOTICE_FILTER;
    private static final ForgeConfigSpec.BooleanValue PARTICLE_LIGHT_CACHE;
    private static final ForgeConfigSpec.BooleanValue PARTICLE_SHARED_LIGHT_CACHE;
    private static final ForgeConfigSpec.BooleanValue PARTICLE_BILLBOARD_FAST_PATH;
    private static final ForgeConfigSpec.EnumValue<ParticleBillboardOwner> PARTICLE_BILLBOARD_OWNER;
    private static final ForgeConfigSpec.BooleanValue PARTICLE_DIAGNOSTICS;
    private static final ForgeConfigSpec.BooleanValue EMPTY_PARTICLE_RENDER_SKIP;
    private static final ForgeConfigSpec.BooleanValue EMPTY_TOAST_RENDER_SKIP;
    private static final ForgeConfigSpec.BooleanValue INACTIVE_TUTORIAL_SKIP;
    private static final ForgeConfigSpec.BooleanValue EMPTY_DEBUG_RENDER_SKIP;
    private static final ForgeConfigSpec.BooleanValue ENTITY_RENDERER_CACHE;
    private static final ForgeConfigSpec.BooleanValue BLOCK_ENTITY_RENDERER_CACHE;
    private static final ForgeConfigSpec.BooleanValue VERTICAL_SECTION_CULLING;
    private static final ForgeConfigSpec.IntValue VERTICAL_SECTION_DISTANCE;
    private static final ForgeConfigSpec.BooleanValue HORIZONTAL_SECTION_CULLING;
    private static final ForgeConfigSpec.IntValue HORIZONTAL_SECTION_DISTANCE;
    private static final ForgeConfigSpec.BooleanValue DYNAMIC_LIGHTS;
    private static final ForgeConfigSpec.BooleanValue DYNAMIC_LIGHT_ENTITIES;
    private static final ForgeConfigSpec.BooleanValue DYNAMIC_LIGHT_BLOCK_ENTITIES;
    private static final ForgeConfigSpec.BooleanValue DYNAMIC_LIGHTS_WITH_SHADERS;
    private static final ForgeConfigSpec.IntValue DYNAMIC_LIGHT_UPDATE_INTERVAL;
    private static final ForgeConfigSpec.BooleanValue CREATE_EMPTY_BUFFER_FLUSH_SKIP;
    private static final ForgeConfigSpec.BooleanValue CREATE_BLOCK_ENTITY_CULLING;
    private static final ForgeConfigSpec.BooleanValue CREATE_ACTOR_CULLING;
    private static final ForgeConfigSpec.BooleanValue CREATE_SECTIONED_CONTRAPTION_MESHES;
    private static final ForgeConfigSpec.IntValue CREATE_SECTIONED_MESH_THRESHOLD;
    private static final ForgeConfigSpec.BooleanValue CREATE_SMART_RENDER_BOUNDS;
    private static final ForgeConfigSpec.BooleanValue CREATE_FLYWHEEL_AUTO_ENABLE;
    private static final ForgeConfigSpec.BooleanValue CREATE_FLYWHEEL_SHADER_COMPAT;

    private static volatile boolean compareMode;
    private static volatile Map<RenderBackportFeature, Boolean> renderBackportOptions =
            defaultRenderBackportOptions();
    private static volatile Map<RendererTransferFeature, Boolean> rendererTransferOptions =
            defaultRendererTransferOptions();
    private static volatile boolean updateChecks = true;
    private static volatile UpdateNoticeFilter updateFilter = UpdateNoticeFilter.CRITICAL;

    public static volatile boolean particleLightCache = true;
    public static volatile boolean particleSharedLightCache = true;
    public static volatile boolean particleBillboardFastPath = true;
    public static volatile ParticleBillboardOwner particleBillboardOwner = ParticleBillboardOwner.AUTO;
    public static volatile boolean particleDiagnostics = false;
    public static volatile boolean emptyParticleRenderSkip = true;
    public static volatile boolean emptyToastRenderSkip = true;
    public static volatile boolean inactiveTutorialSkip = true;
    public static volatile boolean emptyDebugRenderSkip = true;
    public static volatile boolean entityRendererCache = true;
    public static volatile boolean blockEntityRendererCache = true;
    public static volatile boolean verticalSectionCulling = true;
    public static volatile int verticalSectionDistance = 12;
    public static volatile boolean horizontalSectionCulling = false;
    public static volatile int horizontalSectionDistance = 24;
    public static volatile boolean dynamicLights = false;
    public static volatile boolean dynamicLightEntities = true;
    public static volatile boolean dynamicLightBlockEntities = true;
    public static volatile boolean dynamicLightsWithShaders = false;
    public static volatile int dynamicLightUpdateInterval = 1;
    public static volatile boolean createEmptyBufferFlushSkip = true;
    public static volatile boolean createBlockEntityCulling = true;
    public static volatile boolean createActorCulling = true;
    public static volatile boolean createSectionedContraptionMeshes = true;
    public static volatile int createSectionedMeshThreshold = 512;
    public static volatile boolean createSmartRenderBounds = true;
    public static volatile boolean createFlywheelAutoEnable = true;
    public static volatile boolean createFlywheelShaderCompat = true;
    public static volatile int vertexBufferMaxRetainedMib = 16;
    public static volatile int asyncArenaGrowthDivisor = 6;
    public static volatile int asyncArenaMaxHeadroomMib = 64;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("updates");
        UPDATE_CHECKS = builder
                .comment(
                        "Check VRO's raw GitHub update manifest asynchronously.",
                        "The /vro updates command changes and saves this setting."
                )
                .define("check_for_updates", true);
        UPDATE_NOTICE_FILTER = builder
                .comment(
                        "Choose which update types VRO may show: CRITICAL or ALL.",
                        "CRITICAL is the safe default; ALL also permits normal update notices."
                )
                .defineEnum("update_types", UpdateNoticeFilter.CRITICAL);
        builder.pop();

        builder.push("benchmark");
        COMPARE_MODE = builder
                .comment(
                        "Disable every VRO performance optimization for an in-game comparison baseline.",
                        "Client crash guards, cleanup, and key compatibility remain active.",
                        "The /vro compare command changes and saves this setting."
                )
                .define("compare_mode", false);
        builder.pop();

        builder.push("modernfix_backports");
        for (RenderBackportFeature feature : RenderBackportFeature.values()) {
            RENDER_BACKPORT_OPTIONS.put(
                    feature,
                    builder.comment(
                            "Enable VRO's " + feature.displayName() + " backport when VRO owns it.",
                            "This option is evaluated during startup and requires a game restart.",
                            "VRO yields to the current VH Accelerator implementation and to an active ModernFix implementation."
                    ).define(feature.configKey(), true)
            );
        }
        builder.pop();

        builder.push("embeddium_transfers");
        for (RendererTransferFeature feature : RendererTransferFeature.values()) {
            RENDERER_TRANSFER_OPTIONS.put(
                    feature,
                    builder.comment(
                            "Enable " + feature.id() + ": " + feature.displayName() + ".",
                            "This startup option requires a client restart.",
                            feature.activeInCompareMode()
                                    ? "This correctness guard remains active in Compare Mode."
                                    : "Compare Mode yields this performance or compatibility-sensitive path."
                    ).define(feature.configKey(), true)
            );
        }
        VERTEX_BUFFER_MAX_RETAINED_MIB = builder.comment(
                "Maximum native capacity retained by one renderer vertex buffer between builds.",
                "Larger one-off buffers are trimmed at the next start; destroy still frees them deterministically."
        ).defineInRange("vertexBufferMaxRetainedMib", 16, 1, 256);
        ASYNC_ARENA_GROWTH_DIVISOR = builder.comment(
                "Reserve roughly current arena capacity divided by this value during a resize.",
                "Smaller values trade more speculative VRAM for fewer resize/compaction events."
        ).defineInRange("asyncArenaGrowthDivisor", 6, 2, 64);
        ASYNC_ARENA_MAX_HEADROOM_MIB = builder.comment(
                "Maximum speculative VRAM headroom added by one arena growth.",
                "Memory required by the actual upload is never capped by this value."
        ).defineInRange("asyncArenaMaxHeadroomMib", 64, 1, 512);
        builder.pop();

        builder.push("render_fast_paths");
        PARTICLE_LIGHT_CACHE = builder
                .comment("Cache unchanged particle light lookups for one client tick.")
                .define("particle_light_cache", true);
        PARTICLE_SHARED_LIGHT_CACHE = builder
                .comment(
                        "Share light results between particles occupying the same block during one client tick.",
                        "This remains bounded and is cleared when the client level or game tick changes."
                )
                .define("particle_shared_light_cache", true);
        PARTICLE_BILLBOARD_FAST_PATH = builder
                .comment(
                        "Build ordinary particle billboards from the camera's left/up basis instead of rotating four corners.",
                        "Particles with custom render methods are unchanged. This option can be changed while the game is running."
                )
                .define("particle_billboard_fast_path", true);
        PARTICLE_BILLBOARD_OWNER = builder
                .comment(
                        "Select who renders ordinary particle billboards: AUTO, RENDERER, or VRO.",
                        "AUTO selects VRO's geometry and uses an installed renderer's packed writer.",
                        "RENDERER yields to Rubidium/Embeddium when available; VRO forces VRO's compatible path."
                )
                .defineEnum("particle_billboard_owner", ParticleBillboardOwner.AUTO);
        PARTICLE_DIAGNOSTICS = builder
                .comment(
                        "Collect particle queue, render/tick timing, writer, and light-cache counters.",
                        "Disabled by default because class-level diagnostics add measurement overhead."
                )
                .define("particle_diagnostics", false);
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

        builder.push("section_distance_culling");
        VERTICAL_SECTION_CULLING = builder
                .comment(
                        "Skip terrain sections outside the vertical distance while rendering.",
                        "This does not unload chunks or alter Distant Horizons storage."
                )
                .define("vertical_enabled", true);
        VERTICAL_SECTION_DISTANCE = builder
                .comment("Vertical terrain distance in 16-block sections above and below the camera.")
                .defineInRange("vertical_distance", 12, 1, 64);
        HORIZONTAL_SECTION_CULLING = builder
                .comment(
                        "Skip terrain sections outside a circular horizontal distance.",
                        "Disabled by default to preserve the configured vanilla render distance."
                )
                .define("horizontal_enabled", false);
        HORIZONTAL_SECTION_DISTANCE = builder
                .comment("Horizontal terrain radius in 16-block sections when enabled.")
                .defineInRange("horizontal_distance", 24, 1, 64);
        builder.pop();

        builder.push("dynamic_lights");
        DYNAMIC_LIGHTS = builder
                .comment(
                        "Enable VRO's client-side dynamic-light engine.",
                        "Disabled by default and ignored when Dynamic Lights Reforged is installed."
                )
                .define("enabled", false);
        DYNAMIC_LIGHT_ENTITIES = builder
                .comment("Allow entities, held items, dropped items, fire, TNT, and supported projectiles to emit light.")
                .define("entities", true);
        DYNAMIC_LIGHT_BLOCK_ENTITIES = builder
                .comment("Allow resource-defined block entity types to emit dynamic light.")
                .define("block_entities", true);
        DYNAMIC_LIGHTS_WITH_SHADERS = builder
                .comment("Keep VRO dynamic lights active while an Oculus shader pack is enabled.")
                .define("enable_with_shaders", false);
        DYNAMIC_LIGHT_UPDATE_INTERVAL = builder
                .comment("Per-source update interval in client ticks. Each source keeps an independent schedule.")
                .defineInRange("update_interval_ticks", 1, 1, 20);
        builder.pop();

        builder.push("create_rendering");
        CREATE_EMPTY_BUFFER_FLUSH_SKIP = builder
                .comment("Avoid flushing Minecraft's shared render buffers for Create contraptions that rendered no special block entities.")
                .define("skip_empty_contraption_buffer_flush", true);
        CREATE_BLOCK_ENTITY_CULLING = builder
                .comment("Frustum-cull special block entities inside visible Create contraptions.")
                .define("contraption_block_entity_culling", true);
        CREATE_ACTOR_CULLING = builder
                .comment("Frustum-cull movement actors inside visible Create contraptions.")
                .define("contraption_actor_culling", true);
        CREATE_SECTIONED_CONTRAPTION_MESHES = builder
                .comment(
                        "Split large Create contraption meshes into local 16-block sections for frustum culling.",
                        "This is geometric culling only; it does not reduce detail or render distance."
                )
                .define("sectioned_contraption_meshes", true);
        CREATE_SECTIONED_MESH_THRESHOLD = builder
                .comment("Minimum rendered block count before a contraption uses sectioned meshes.")
                .defineInRange("sectioned_mesh_block_threshold", 512, 128, 16384);
        CREATE_SMART_RENDER_BOUNDS = builder
                .comment("Use directional cached render bounds for supported Create machinery.")
                .define("smart_machinery_render_bounds", true);
        CREATE_FLYWHEEL_AUTO_ENABLE = builder
                .comment(
                        "Restore Flywheel's upstream-default instancing backend when a pack disables it.",
                        "Unsupported GPUs and shader integration failures still fall back safely.",
                        "Disable this option to preserve a manually selected OFF backend."
                )
                .define("auto_enable_flywheel_instancing", true);
        CREATE_FLYWHEEL_SHADER_COMPAT = builder
                .comment(
                        "Keep Flywheel's instancing backend available with Oculus shaders.",
                        "Tested with Oculus 1.6.x, Rubidium/Embeddium, and Flywheel 0.6.11.",
                        "Unsupported or incomplete mod stacks ignore this option."
                )
                .define("flywheel_shader_compat", true);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientOptimizationConfig() {
    }

    public static boolean optimizationsEnabled() {
        return !compareMode;
    }

    public static boolean compareModeEnabled() {
        return compareMode;
    }

    public static boolean renderBackportConfigured(RenderBackportFeature feature) {
        return renderBackportOptions.getOrDefault(feature, true);
    }

    public static boolean rendererTransferConfigured(RendererTransferFeature feature) {
        return rendererTransferOptions.getOrDefault(feature, true);
    }

    public static boolean updateChecksEnabled() {
        return updateChecks;
    }

    public static UpdateNoticeFilter updateNoticeFilter() {
        return updateFilter;
    }

    public static void setUpdateChecks(boolean enabled) {
        UPDATE_CHECKS.set(enabled);
        UPDATE_CHECKS.save();
        updateChecks = enabled;
        UpdateNoticeService.setEnabled(enabled);
    }

    public static void setUpdateNoticeFilter(UpdateNoticeFilter filter) {
        UpdateNoticeFilter safeFilter = UpdateNoticeFilter.fromConfigValue(
                filter,
                UpdateNoticeFilter.CRITICAL
        );
        UPDATE_NOTICE_FILTER.set(safeFilter);
        UPDATE_NOTICE_FILTER.save();
        updateFilter = safeFilter;
        UpdateNoticeService.setFilter(safeFilter);
    }

    public static void setCompareMode(boolean enabled) {
        COMPARE_MODE.set(enabled);
        COMPARE_MODE.save();
        compareMode = enabled;
        VaultGearRenderCache.clear();
        VaultToolRenderCache.clear();
        reloadCreateRenderers();
        VaultRenderOptimization.LOGGER.info(
                "Compare Mode {} and saved",
                enabled ? "enabled" : "disabled"
        );
    }

    private static void reloadCreateRenderers() {
        if (!ModList.get().isLoaded("create")) {
            return;
        }
        try {
            Class<?> backend = Class.forName("com.jozufozu.flywheel.backend.Backend");
            backend.getMethod("refresh").invoke(null);
            backend.getMethod("reloadWorldRenderers").invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            VaultRenderOptimization.LOGGER.warn(
                    "Could not refresh Create renderers after changing VRO configuration",
                    exception
            );
        }
    }

    public static void setVerticalSectionCulling(boolean enabled) {
        VERTICAL_SECTION_CULLING.set(enabled);
        VERTICAL_SECTION_CULLING.save();
        verticalSectionCulling = enabled;
    }

    public static void setHorizontalSectionCulling(boolean enabled) {
        HORIZONTAL_SECTION_CULLING.set(enabled);
        HORIZONTAL_SECTION_CULLING.save();
        horizontalSectionCulling = enabled;
    }

    public static void setVerticalSectionDistance(int distance) {
        VERTICAL_SECTION_DISTANCE.set(distance);
        VERTICAL_SECTION_DISTANCE.save();
        verticalSectionDistance = distance;
    }

    public static void setHorizontalSectionDistance(int distance) {
        HORIZONTAL_SECTION_DISTANCE.set(distance);
        HORIZONTAL_SECTION_DISTANCE.save();
        horizontalSectionDistance = distance;
    }

    public static void setParticleBillboardFastPath(boolean enabled) {
        PARTICLE_BILLBOARD_FAST_PATH.set(enabled);
        PARTICLE_BILLBOARD_FAST_PATH.save();
        particleBillboardFastPath = enabled;
    }

    public static void setParticleBillboardOwner(ParticleBillboardOwner owner) {
        PARTICLE_BILLBOARD_OWNER.set(owner);
        PARTICLE_BILLBOARD_OWNER.save();
        particleBillboardOwner = owner;
    }

    public static void setParticleSharedLightCache(boolean enabled) {
        PARTICLE_SHARED_LIGHT_CACHE.set(enabled);
        PARTICLE_SHARED_LIGHT_CACHE.save();
        particleSharedLightCache = enabled;
    }

    public static void setParticleDiagnostics(boolean enabled) {
        PARTICLE_DIAGNOSTICS.set(enabled);
        PARTICLE_DIAGNOSTICS.save();
        particleDiagnostics = enabled;
    }

    public static void setDynamicLights(boolean enabled) {
        DYNAMIC_LIGHTS.set(enabled);
        DYNAMIC_LIGHTS.save();
        dynamicLights = enabled;
    }

    public static void setDynamicLightEntities(boolean enabled) {
        DYNAMIC_LIGHT_ENTITIES.set(enabled);
        DYNAMIC_LIGHT_ENTITIES.save();
        dynamicLightEntities = enabled;
    }

    public static void setDynamicLightBlockEntities(boolean enabled) {
        DYNAMIC_LIGHT_BLOCK_ENTITIES.set(enabled);
        DYNAMIC_LIGHT_BLOCK_ENTITIES.save();
        dynamicLightBlockEntities = enabled;
    }

    public static void setDynamicLightsWithShaders(boolean enabled) {
        DYNAMIC_LIGHTS_WITH_SHADERS.set(enabled);
        DYNAMIC_LIGHTS_WITH_SHADERS.save();
        dynamicLightsWithShaders = enabled;
    }

    public static void setDynamicLightUpdateInterval(int ticks) {
        DYNAMIC_LIGHT_UPDATE_INTERVAL.set(ticks);
        DYNAMIC_LIGHT_UPDATE_INTERVAL.save();
        dynamicLightUpdateInterval = ticks;
    }

    public static void setCreateFlywheelShaderCompat(boolean enabled) {
        CREATE_FLYWHEEL_SHADER_COMPAT.set(enabled);
        CREATE_FLYWHEEL_SHADER_COMPAT.save();
        createFlywheelShaderCompat = enabled;
        if (ModList.get().isLoaded("flywheel") && ModList.get().isLoaded("oculus")) {
            try {
                Class<?> state = Class.forName(
                        "dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.FlywheelShaderCompatState"
                );
                state.getMethod("resetForConfigurationChange").invoke(null);
            } catch (ReflectiveOperationException | LinkageError exception) {
                VaultRenderOptimization.LOGGER.warn("Could not reset Create shader compatibility state", exception);
            }
        }
        reloadCreateRenderers();
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

        compareMode = COMPARE_MODE.get();
        EnumMap<RenderBackportFeature, Boolean> backportValues =
                new EnumMap<>(RenderBackportFeature.class);
        RENDER_BACKPORT_OPTIONS.forEach((feature, value) -> backportValues.put(feature, value.get()));
        renderBackportOptions = Map.copyOf(backportValues);
        EnumMap<RendererTransferFeature, Boolean> rendererTransferValues =
                new EnumMap<>(RendererTransferFeature.class);
        RENDERER_TRANSFER_OPTIONS.forEach(
                (feature, value) -> rendererTransferValues.put(feature, value.get())
        );
        rendererTransferOptions = Map.copyOf(rendererTransferValues);
        vertexBufferMaxRetainedMib = VERTEX_BUFFER_MAX_RETAINED_MIB.get();
        asyncArenaGrowthDivisor = ASYNC_ARENA_GROWTH_DIVISOR.get();
        asyncArenaMaxHeadroomMib = ASYNC_ARENA_MAX_HEADROOM_MIB.get();
        updateChecks = UPDATE_CHECKS.get();
        updateFilter = UpdateNoticeFilter.fromConfigValue(
                UPDATE_NOTICE_FILTER.get(),
                UpdateNoticeFilter.CRITICAL
        );
        UpdateNoticeService.setEnabled(updateChecks);
        UpdateNoticeService.setFilter(updateFilter);
        particleLightCache = PARTICLE_LIGHT_CACHE.get();
        particleSharedLightCache = PARTICLE_SHARED_LIGHT_CACHE.get();
        particleBillboardFastPath = PARTICLE_BILLBOARD_FAST_PATH.get();
        particleBillboardOwner = PARTICLE_BILLBOARD_OWNER.get();
        particleDiagnostics = PARTICLE_DIAGNOSTICS.get();
        emptyParticleRenderSkip = EMPTY_PARTICLE_RENDER_SKIP.get();
        emptyToastRenderSkip = EMPTY_TOAST_RENDER_SKIP.get();
        inactiveTutorialSkip = INACTIVE_TUTORIAL_SKIP.get();
        emptyDebugRenderSkip = EMPTY_DEBUG_RENDER_SKIP.get();
        entityRendererCache = ENTITY_RENDERER_CACHE.get();
        blockEntityRendererCache = BLOCK_ENTITY_RENDERER_CACHE.get();
        verticalSectionCulling = VERTICAL_SECTION_CULLING.get();
        verticalSectionDistance = VERTICAL_SECTION_DISTANCE.get();
        horizontalSectionCulling = HORIZONTAL_SECTION_CULLING.get();
        horizontalSectionDistance = HORIZONTAL_SECTION_DISTANCE.get();
        dynamicLights = DYNAMIC_LIGHTS.get();
        dynamicLightEntities = DYNAMIC_LIGHT_ENTITIES.get();
        dynamicLightBlockEntities = DYNAMIC_LIGHT_BLOCK_ENTITIES.get();
        dynamicLightsWithShaders = DYNAMIC_LIGHTS_WITH_SHADERS.get();
        dynamicLightUpdateInterval = DYNAMIC_LIGHT_UPDATE_INTERVAL.get();
        createEmptyBufferFlushSkip = CREATE_EMPTY_BUFFER_FLUSH_SKIP.get();
        createBlockEntityCulling = CREATE_BLOCK_ENTITY_CULLING.get();
        createActorCulling = CREATE_ACTOR_CULLING.get();
        createSectionedContraptionMeshes = CREATE_SECTIONED_CONTRAPTION_MESHES.get();
        createSectionedMeshThreshold = CREATE_SECTIONED_MESH_THRESHOLD.get();
        createSmartRenderBounds = CREATE_SMART_RENDER_BOUNDS.get();
        createFlywheelAutoEnable = CREATE_FLYWHEEL_AUTO_ENABLE.get();
        createFlywheelShaderCompat = CREATE_FLYWHEEL_SHADER_COMPAT.get();
    }

    private static Map<RenderBackportFeature, Boolean> defaultRenderBackportOptions() {
        EnumMap<RenderBackportFeature, Boolean> defaults =
                new EnumMap<>(RenderBackportFeature.class);
        for (RenderBackportFeature feature : RenderBackportFeature.values()) {
            defaults.put(feature, true);
        }
        return Map.copyOf(defaults);
    }

    private static Map<RendererTransferFeature, Boolean> defaultRendererTransferOptions() {
        EnumMap<RendererTransferFeature, Boolean> defaults =
                new EnumMap<>(RendererTransferFeature.class);
        for (RendererTransferFeature feature : RendererTransferFeature.values()) {
            defaults.put(feature, true);
        }
        return Map.copyOf(defaults);
    }
}
