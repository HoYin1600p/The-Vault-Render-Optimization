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
    private static final ForgeConfigSpec.BooleanValue DEFER_CHUNK_UPDATES;
    private static final ForgeConfigSpec.BooleanValue INDEX_ONLY_SORTING;
    private static final ForgeConfigSpec.BooleanValue SORT_GEOMETRY_CACHE;
    private static final ForgeConfigSpec.BooleanValue FARSIGHT_CHUNK_BOUND;
    private static final ForgeConfigSpec.BooleanValue ADAPTIVE_CHUNK_BUDGET;
    private static final EnumMap<RenderBackportFeature, ForgeConfigSpec.BooleanValue>
            RENDER_BACKPORT_OPTIONS = new EnumMap<>(RenderBackportFeature.class);
    private static final EnumMap<RendererTransferFeature, ForgeConfigSpec.BooleanValue>
            RENDERER_TRANSFER_OPTIONS = new EnumMap<>(RendererTransferFeature.class);
    private static final ForgeConfigSpec.IntValue VERTEX_BUFFER_MAX_RETAINED_MIB;
    private static final ForgeConfigSpec.BooleanValue VERTEX_BUFFER_ADAPTIVE_TRIMMING;
    private static final ForgeConfigSpec.IntValue VERTEX_BUFFER_AGGREGATE_RETAINED_MIB;
    private static final ForgeConfigSpec.BooleanValue HUD_TEXT_GEOMETRY;
    private static final ForgeConfigSpec.IntValue ASYNC_ARENA_GROWTH_DIVISOR;
    private static final ForgeConfigSpec.IntValue ASYNC_ARENA_MAX_HEADROOM_MIB;
    private static final ForgeConfigSpec.BooleanValue UPDATE_CHECKS;
    private static final ForgeConfigSpec.EnumValue<UpdateNoticeFilter> UPDATE_NOTICE_FILTER;
    private static final ForgeConfigSpec.BooleanValue PARTICLE_LIGHT_CACHE;
    private static final ForgeConfigSpec.BooleanValue PARTICLE_COLLISION_CACHE;
    private static final ForgeConfigSpec.BooleanValue PARTICLE_SHARED_LIGHT_CACHE;
    private static final ForgeConfigSpec.BooleanValue PARTICLE_BILLBOARD_FAST_PATH;
    private static final ForgeConfigSpec.EnumValue<ParticleBillboardOwner> PARTICLE_BILLBOARD_OWNER;
    private static final ForgeConfigSpec.BooleanValue PARTICLE_DIAGNOSTICS;
    private static final ForgeConfigSpec.BooleanValue EMPTY_PARTICLE_RENDER_SKIP;
    private static final ForgeConfigSpec.BooleanValue ALLOCATION_FREE_FRUSTUM;
    private static final ForgeConfigSpec.BooleanValue FASTLOAD_FRUSTUM_BYPASS;
    private static final ForgeConfigSpec.BooleanValue PARTICLE_TICK_COMPACTION;
    private static final ForgeConfigSpec.BooleanValue PARTICLE_SHARED_RANDOM;
    private static final ForgeConfigSpec.BooleanValue PARTICLE_PROVIDER_CACHE;
    private static final ForgeConfigSpec.BooleanValue GPU_ENTITY_MODELS;
    private static final ForgeConfigSpec.BooleanValue GPU_PARTICLES;
    private static final ForgeConfigSpec.BooleanValue GPU_ENTITY_MODELS_WITH_SHADERS;
    private static final ForgeConfigSpec.BooleanValue GPU_PARTICLES_WITH_SHADERS;
    private static final ForgeConfigSpec.BooleanValue GPU_ITEMS;
    private static final ForgeConfigSpec.BooleanValue EMPTY_TOAST_RENDER_SKIP;
    private static final ForgeConfigSpec.BooleanValue INACTIVE_TUTORIAL_SKIP;
    private static final ForgeConfigSpec.BooleanValue EMPTY_DEBUG_RENDER_SKIP;
    private static final ForgeConfigSpec.BooleanValue ENTITY_RENDERER_CACHE;
    private static final ForgeConfigSpec.BooleanValue BLOCK_ENTITY_RENDERER_CACHE;
    private static final ForgeConfigSpec.BooleanValue SOPHISTICATED_STORAGE_FACE_CULLING;
    private static final ForgeConfigSpec.BooleanValue SOPHISTICATED_STORAGE_COUNT_CACHE;
    private static final ForgeConfigSpec.BooleanValue SOPHISTICATED_STORAGE_FILL_FAST_PATH;
    private static final ForgeConfigSpec.BooleanValue SOPHISTICATED_STORAGE_RENDER_UPDATE_FILTER;
    private static final ForgeConfigSpec.BooleanValue SOPHISTICATED_STORAGE_DIAGNOSTICS;
    private static final ForgeConfigSpec.BooleanValue VERTICAL_SECTION_CULLING;
    private static final ForgeConfigSpec.IntValue VERTICAL_SECTION_DISTANCE;
    private static final ForgeConfigSpec.BooleanValue HORIZONTAL_SECTION_CULLING;
    private static final ForgeConfigSpec.IntValue HORIZONTAL_SECTION_DISTANCE;
    private static final ForgeConfigSpec.BooleanValue CREATE_EMPTY_BUFFER_FLUSH_SKIP;
    private static final ForgeConfigSpec.BooleanValue CREATE_BLOCK_ENTITY_CULLING;
    private static final ForgeConfigSpec.BooleanValue CREATE_ACTOR_CULLING;
    private static final ForgeConfigSpec.BooleanValue CREATE_SECTIONED_CONTRAPTION_MESHES;
    private static final ForgeConfigSpec.IntValue CREATE_SECTIONED_MESH_THRESHOLD;
    private static final ForgeConfigSpec.BooleanValue CREATE_SMART_RENDER_BOUNDS;
    private static final ForgeConfigSpec.BooleanValue CREATE_FLYWHEEL_AUTO_ENABLE;
    private static final ForgeConfigSpec.BooleanValue CREATE_FLYWHEEL_SHADER_COMPAT;

    private static volatile boolean compareMode;
    public static volatile boolean deferChunkUpdates = true;
    public static volatile boolean indexOnlySorting = true;
    public static volatile boolean adaptiveChunkBudget = true;
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
    public static volatile boolean particleCollisionCache = true;
    public static volatile boolean sortGeometryCache = false;
    public static volatile boolean farsightChunkBound = true;
    public static volatile boolean emptyParticleRenderSkip = true;
    public static volatile boolean allocationFreeFrustum = true;
    public static volatile boolean fastloadFrustumBypass = true;
    public static volatile boolean particleTickCompaction = true;
    public static volatile boolean particleSharedRandom = true;
    public static volatile boolean particleProviderCache = true;
    public static volatile boolean gpuEntityModels = true;
    public static volatile boolean gpuParticles = true;
    public static volatile boolean gpuEntityModelsWithShaders = false;
    public static volatile boolean gpuParticlesWithShaders = false;
    public static volatile boolean gpuItems = true;
    public static volatile boolean emptyToastRenderSkip = true;
    public static volatile boolean inactiveTutorialSkip = true;
    public static volatile boolean emptyDebugRenderSkip = true;
    public static volatile boolean entityRendererCache = true;
    public static volatile boolean blockEntityRendererCache = true;
    public static volatile boolean sophisticatedStorageFaceCulling = true;
    public static volatile boolean sophisticatedStorageCountCache = true;
    public static volatile boolean sophisticatedStorageFillFastPath = true;
    public static volatile boolean sophisticatedStorageRenderUpdateFilter = true;
    public static volatile boolean sophisticatedStorageDiagnostics = false;
    public static volatile boolean verticalSectionCulling = true;
    public static volatile int verticalSectionDistance = 12;
    public static volatile boolean horizontalSectionCulling = false;
    public static volatile int horizontalSectionDistance = 24;
    public static volatile boolean createEmptyBufferFlushSkip = true;
    public static volatile boolean createBlockEntityCulling = true;
    public static volatile boolean createActorCulling = true;
    public static volatile boolean createSectionedContraptionMeshes = true;
    public static volatile int createSectionedMeshThreshold = 512;
    public static volatile boolean createSmartRenderBounds = true;
    public static volatile boolean createFlywheelAutoEnable = true;
    public static volatile boolean createFlywheelShaderCompat = true;
    public static volatile int vertexBufferMaxRetainedMib = 16;
    public static volatile boolean vertexBufferAdaptiveTrimming = false;
    public static volatile int vertexBufferAggregateRetainedMib = 128;
    public static volatile boolean hudTextGeometry = false;
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

        builder.push("chunk_updates");
        DEFER_CHUNK_UPDATES = builder.comment(
                "Use the renderer's existing asynchronous chunk-update path to reduce render-thread stalls.",
                "Default-on for vanilla Forge and validated Embeddium/Rubidium versions; no renderer mod is required.",
                "Visible block changes can be delayed under load. No render distance or world data is changed.",
                "Does not edit vanilla/renderer settings. Off and Compare Mode yield to those settings.",
                "The /vro chunks defer on|off command applies immediately to new scheduling decisions."
        ).define("defer_updates", true);
        INDEX_ONLY_SORTING = builder.comment(
                "Reuse unchanged terrain vertices during translucent sorting on validated Embeddium builds.",
                "Only drawing-order indices are uploaded. Vanilla and Rubidium 0.5.6 are unchanged.",
                "Off/Compare Mode affects new jobs; already queued index-only jobs finish safely.",
                "Use /vro chunks sorting on|off|status without a restart."
        ).define("index_only_sorting", true);
        SORT_GEOMETRY_CACHE = builder.comment(
                "Experimental: cache immutable triangle centers for validated Embeddium index-only sorting.",
                "Bounded to 16 MiB globally; preserves native stable distance order and generation checks.",
                "Defaults off pending in-game water/glass and shader comparisons."
        ).define("sort_geometry_cache", false);
        ADAPTIVE_CHUNK_BUDGET = builder.comment(
                "Experimental v2: dynamically pace updates to already-built terrain using measured costs.",
                "Default-on on supported renderers; an explicit adaptive_budget_v2=false remains respected.",
                "Initial terrain and delayed/overloaded queues use native scheduling/draining automatically.",
                "Requires validated Embeddium and effective asynchronous chunk updates; vanilla/Rubidium unchanged.",
                "Uses conservative startup budgets, bounded feedback and queue backpressure; not an FPS guarantee.",
                "Off/Compare Mode restores native draining and scheduling; /vro chunks budget on|off|status is hot."
        ).define("adaptive_budget_v2", true);
        FARSIGHT_CHUNK_BOUND = builder.comment(
                "Only with Farsight installed: forget client chunks farther than max(server view distance,",
                "render distance) + 1 chunks, once per second, exactly as the forget packet Farsight cancels would",
                "(chunk drop, light release, Embeddium/Rubidium render sections). Chunks within render distance stay.",
                "This is a leak fix: Compare Mode does not affect it, and VH Accelerator leaves this job to VRO,",
                "so turning it off means nothing bounds Farsight's retained chunks until the level changes."
        ).define("farsight_chunk_bound", true);
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
        VERTEX_BUFFER_ADAPTIVE_TRIMMING = builder.comment(
                "Experimental: trim unused CPU-native vertex capacity at safe worker start points.",
                "Keeps a 30-second demand window; idle workers trim only when used again, never remotely.",
                "Existing per-buffer ceiling and native destroy remain unchanged. Off pending validation."
        ).define("vertexBufferAdaptiveTrimming", false);
        VERTEX_BUFFER_AGGREGATE_RETAINED_MIB = builder.comment(
                "Aggregate excess-capacity pressure threshold, not a hard process/native/VRAM limit.",
                "Above this, active workers release old peaks at their next safe start; required writes are never capped."
        ).defineInRange("vertexBufferAggregateRetainedMib", 128, 16, 2048);
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
        HUD_TEXT_GEOMETRY = builder.comment(
                "Experimental: reuse unchanged plain HUD text glyph geometry while drawing every frame.",
                "Formatted/animated/bidirectional/custom font paths fall back; no gear data or HUD state is cached.",
                "Defaults off pending visual validation. Compare Mode disables reuse."
        ).define("hud_text_geometry", false);
        PARTICLE_COLLISION_CACHE = builder.comment(
                "Exact particle block collision with a per-tick cache: visits the same cells and shapes as",
                "vanilla and uses vanilla's collision response, but reads each block once per particle tick",
                "instead of once per particle. Every particle still collides; nothing is skipped.",
                "Yields to Particle Core/Flerovium. /vro particles collision on|off|verify toggles at runtime."
        ).define("particle_collision_cache", true);
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
        ALLOCATION_FREE_FRUSTUM = builder.comment(
                "Test each frustum plane against the box corner farthest along its normal instead of allocating eight",
                "vectors per plane. Same float operations in the same order, so the answer is vanilla's exactly."
        ).define("allocation_free_frustum", true);
        FASTLOAD_FRUSTUM_BYPASS = builder.comment(
                "With Fastload installed: while its frustum event has no listeners (all gameplay after world load),",
                "visibility checks skip its synchronized per-check event lookup. Same answers; only applies when",
                "Fastload's hook is the only change to that frustum method."
        ).define("fastload_frustum_bypass", true);
        PARTICLE_TICK_COMPACTION = builder.comment(
                "Remove particles that died this tick in one ordered pass instead of one queue shift each.",
                "Every particle still ticks in vanilla order; only the removal of dead ones is batched."
        ).define("particle_tick_compaction", true);
        PARTICLE_SHARED_RANDOM = builder.comment(
                "Give particles a per-thread generator with java.util.Random's exact algorithm instead of a new",
                "Random each (a CAS, nanoTime and AtomicLong per particle, and a CAS per draw). Same distribution."
        ).define("particle_shared_random", true);
        PARTICLE_PROVIDER_CACHE = builder.comment(
                "Resolve each particle type's provider once instead of a registry key plus hash lookup per spawn.",
                "Invalidated whenever a provider is registered. Returns exactly the provider vanilla would."
        ).define("particle_provider_cache", true);
        GPU_ENTITY_MODELS = builder.comment(
                "Expand entity model cubes on the GPU. Each model part reserves its vertices in the",
                "vanilla entity buffer and a compute shader writes them into the uploaded vertex buffer right before",
                "vanilla's own draw, with vanilla's exact float arithmetic (bit-for-bit self-test at startup).",
                "Needs OpenGL 4.3 or the ARB compute extensions; pauses with an Oculus shader pack (see",
                "gpu_entity_models_with_shaders), in Compare",
                "Mode, on a failed self-test or mixin audit. /vro gpuentity status explains the current state."
        ).define("gpu_entity_models", true);
        GPU_PARTICLES = builder.comment(
                "With gpu_entity_models active: billboard particles reserve their four vertices and the same compute",
                "path writes them with VRO's exact billboard arithmetic (own startup self-test). Every particle is",
                "still drawn; anything unusual stays on the CPU writer."
        ).define("gpu_particles", true);
        GPU_ENTITY_MODELS_WITH_SHADERS = builder.comment(
                "Experimental; mainly for slower CPUs. With gpu_entity_models active and an Oculus shader pack on,",
                "entity models and particles",
                "stay on the GPU path: a second program writes Oculus' extended entity vertices (face normal, tangent,",
                "mid UV, entity IDs) exactly as Oculus computes them. It saves CPU time, but shader packs usually make",
                "the game GPU-bound, where the extra compute passes cost more than they save; off by default."
        ).define("gpu_entity_models_with_shaders", false);
        GPU_PARTICLES_WITH_SHADERS = builder.comment(
                "With gpu_particles active and an Oculus shader pack on, keep only particles on the GPU (they keep",
                "the vanilla particle format under shader packs). gpu_entity_models_with_shaders includes this."
        ).define("gpu_particles_with_shaders", false);
        GPU_ITEMS = builder.comment(
                "With gpu_entity_models active: solid and cutout block items (dropped, in frames, held) reserve their",
                "vertices and the compute path writes them exactly as the installed item writer (Forge or Embeddium)",
                "would (own self-test). Flat, translucent and glinting items always stay on the CPU."
        ).define("gpu_items", true);
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

        builder.push("sophisticated_storage");
        SOPHISTICATED_STORAGE_FACE_CULLING = builder
                .comment(
                        "Skip barrel front-display rendering when its display plane faces away from the camera",
                        "or is completely covered by the immediately adjacent block. Visible displays are unchanged."
                )
                .define("front_display_culling", true);
        SOPHISTICATED_STORAGE_COUNT_CACHE = builder
                .comment(
                        "Cache limited-barrel count formatting and glyph layout until the displayed value changes.",
                        "The cache is bounded and cleared by resource reloads."
                )
                .define("quantity_text_cache", true);
        SOPHISTICATED_STORAGE_FILL_FAST_PATH = builder
                .comment(
                        "Render limited-barrel fill indicators without per-vertex temporary objects",
                        "while preserving the original fill values, texture, lighting, and transparency."
                )
                .define("fill_level_fast_path", true);
        SOPHISTICATED_STORAGE_RENDER_UPDATE_FILTER = builder
                .comment(
                        "Avoid rebuilding barrel chunk geometry for client updates that only change quantities",
                        "or fill levels. Model-affecting display changes still rebuild immediately."
                )
                .define("count_only_render_update_filter", true);
        SOPHISTICATED_STORAGE_DIAGNOSTICS = builder
                .comment(
                        "Collect Sophisticated Storage render and cache counters for /vro storage.",
                        "Disabled by default to keep the normal render path as small as possible."
                )
                .define("diagnostics", false);
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
                        "Turns on Flywheel's instancing renderer for this session when the pack has it set to OFF.",
                        "Flywheel's own config file is never changed; turning this off returns to the pack's setting",
                        "immediately. Unsupported GPUs and shader integration failures still fall back safely."
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

    public static void setDeferChunkUpdates(boolean enabled) {
        DEFER_CHUNK_UPDATES.set(enabled);
        DEFER_CHUNK_UPDATES.save();
        deferChunkUpdates = enabled;
    }

    public static void setIndexOnlySorting(boolean enabled) {
        INDEX_ONLY_SORTING.set(enabled);
        INDEX_ONLY_SORTING.save();
        indexOnlySorting = enabled;
    }

    public static void setAdaptiveChunkBudget(boolean enabled) {
        ADAPTIVE_CHUNK_BUDGET.set(enabled);
        ADAPTIVE_CHUNK_BUDGET.save();
        adaptiveChunkBudget = enabled;
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

    /** Rebuilds Create's contraption and Flywheel renderers so changed Create options reach existing ones. */
    public static void reloadCreateRenderers() {
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

    public static void setFarsightChunkBound(boolean enabled) {
        FARSIGHT_CHUNK_BOUND.set(enabled);
        FARSIGHT_CHUNK_BOUND.save();
        farsightChunkBound = enabled;
    }

    public static void setSortGeometryCache(boolean enabled) {
        SORT_GEOMETRY_CACHE.set(enabled);
        SORT_GEOMETRY_CACHE.save();
        sortGeometryCache = enabled;
    }

    public static void setHudTextGeometry(boolean enabled) {
        HUD_TEXT_GEOMETRY.set(enabled);
        HUD_TEXT_GEOMETRY.save();
        hudTextGeometry = enabled;
        dev.hoyin1600p.vault_render_optimization.client.hud.HudTextGeometry.clear();
    }

    public static void setVertexBufferAdaptiveTrimming(boolean enabled) {
        VERTEX_BUFFER_ADAPTIVE_TRIMMING.set(enabled);
        VERTEX_BUFFER_ADAPTIVE_TRIMMING.save();
        vertexBufferAdaptiveTrimming = enabled;
    }

    public static void setGpuEntityModels(boolean enabled) {
        GPU_ENTITY_MODELS.set(enabled);
        GPU_ENTITY_MODELS.save();
        gpuEntityModels = enabled;
    }

    /** The 0.5.0 particle set: collision cache, tick compaction, shared random and provider cache. */
    public static void setNewParticleOptimizations(boolean enabled) {
        PARTICLE_COLLISION_CACHE.set(enabled);
        PARTICLE_TICK_COMPACTION.set(enabled);
        PARTICLE_SHARED_RANDOM.set(enabled);
        PARTICLE_PROVIDER_CACHE.set(enabled);
        PARTICLE_COLLISION_CACHE.save();
        particleCollisionCache = enabled;
        particleTickCompaction = enabled;
        particleSharedRandom = enabled;
        particleProviderCache = enabled;
    }

    public static void setParticleCollisionCache(boolean enabled) {
        PARTICLE_COLLISION_CACHE.set(enabled);
        PARTICLE_COLLISION_CACHE.save();
        particleCollisionCache = enabled;
    }

    public static void setParticleTickCompaction(boolean enabled) {
        PARTICLE_TICK_COMPACTION.set(enabled);
        PARTICLE_TICK_COMPACTION.save();
        particleTickCompaction = enabled;
    }

    public static void setParticleSharedRandom(boolean enabled) {
        PARTICLE_SHARED_RANDOM.set(enabled);
        PARTICLE_SHARED_RANDOM.save();
        particleSharedRandom = enabled;
    }

    public static void setParticleProviderCache(boolean enabled) {
        PARTICLE_PROVIDER_CACHE.set(enabled);
        PARTICLE_PROVIDER_CACHE.save();
        particleProviderCache = enabled;
    }

    public static void setGpuEntityModelsWithShaders(boolean enabled) {
        GPU_ENTITY_MODELS_WITH_SHADERS.set(enabled);
        GPU_ENTITY_MODELS_WITH_SHADERS.save();
        gpuEntityModelsWithShaders = enabled;
    }

    public static void setGpuParticlesWithShaders(boolean enabled) {
        GPU_PARTICLES_WITH_SHADERS.set(enabled);
        GPU_PARTICLES_WITH_SHADERS.save();
        gpuParticlesWithShaders = enabled;
    }

    public static void setGpuItems(boolean enabled) {
        GPU_ITEMS.set(enabled);
        GPU_ITEMS.save();
        gpuItems = enabled;
    }

    public static void setGpuParticles(boolean enabled) {
        GPU_PARTICLES.set(enabled);
        GPU_PARTICLES.save();
        gpuParticles = enabled;
    }

    public static void setFastloadFrustumBypass(boolean enabled) {
        FASTLOAD_FRUSTUM_BYPASS.set(enabled);
        FASTLOAD_FRUSTUM_BYPASS.save();
        fastloadFrustumBypass = enabled;
    }

    public static void setAllocationFreeFrustum(boolean enabled) {
        ALLOCATION_FREE_FRUSTUM.set(enabled);
        ALLOCATION_FREE_FRUSTUM.save();
        allocationFreeFrustum = enabled;
    }

    public static void setSophisticatedStorageFaceCulling(boolean enabled) {
        SOPHISTICATED_STORAGE_FACE_CULLING.set(enabled);
        SOPHISTICATED_STORAGE_FACE_CULLING.save();
        sophisticatedStorageFaceCulling = enabled;
    }

    public static void setSophisticatedStorageCountCache(boolean enabled) {
        SOPHISTICATED_STORAGE_COUNT_CACHE.set(enabled);
        SOPHISTICATED_STORAGE_COUNT_CACHE.save();
        sophisticatedStorageCountCache = enabled;
    }

    public static void setSophisticatedStorageFillFastPath(boolean enabled) {
        SOPHISTICATED_STORAGE_FILL_FAST_PATH.set(enabled);
        SOPHISTICATED_STORAGE_FILL_FAST_PATH.save();
        sophisticatedStorageFillFastPath = enabled;
    }

    public static void setSophisticatedStorageRenderUpdateFilter(boolean enabled) {
        SOPHISTICATED_STORAGE_RENDER_UPDATE_FILTER.set(enabled);
        SOPHISTICATED_STORAGE_RENDER_UPDATE_FILTER.save();
        sophisticatedStorageRenderUpdateFilter = enabled;
    }

    public static void setSophisticatedStorageDiagnostics(boolean enabled) {
        SOPHISTICATED_STORAGE_DIAGNOSTICS.set(enabled);
        SOPHISTICATED_STORAGE_DIAGNOSTICS.save();
        sophisticatedStorageDiagnostics = enabled;
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
        applySavedValues();
    }

    /** Re-reads every value from the spec into the live fields, exactly as a config reload does. */
    public static void applySavedValues() {
        compareMode = COMPARE_MODE.get();
        deferChunkUpdates = DEFER_CHUNK_UPDATES.get();
        indexOnlySorting = INDEX_ONLY_SORTING.get();
        sortGeometryCache = SORT_GEOMETRY_CACHE.get();
        farsightChunkBound = FARSIGHT_CHUNK_BOUND.get();
        adaptiveChunkBudget = ADAPTIVE_CHUNK_BUDGET.get();
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
        vertexBufferAdaptiveTrimming = VERTEX_BUFFER_ADAPTIVE_TRIMMING.get();
        vertexBufferAggregateRetainedMib = VERTEX_BUFFER_AGGREGATE_RETAINED_MIB.get();
        hudTextGeometry = HUD_TEXT_GEOMETRY.get();
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
        particleCollisionCache = PARTICLE_COLLISION_CACHE.get();
        emptyParticleRenderSkip = EMPTY_PARTICLE_RENDER_SKIP.get();
        allocationFreeFrustum = ALLOCATION_FREE_FRUSTUM.get();
        fastloadFrustumBypass = FASTLOAD_FRUSTUM_BYPASS.get();
        particleTickCompaction = PARTICLE_TICK_COMPACTION.get();
        particleSharedRandom = PARTICLE_SHARED_RANDOM.get();
        particleProviderCache = PARTICLE_PROVIDER_CACHE.get();
        gpuEntityModels = GPU_ENTITY_MODELS.get();
        gpuParticles = GPU_PARTICLES.get();
        gpuEntityModelsWithShaders = GPU_ENTITY_MODELS_WITH_SHADERS.get();
        gpuParticlesWithShaders = GPU_PARTICLES_WITH_SHADERS.get();
        gpuItems = GPU_ITEMS.get();
        emptyToastRenderSkip = EMPTY_TOAST_RENDER_SKIP.get();
        inactiveTutorialSkip = INACTIVE_TUTORIAL_SKIP.get();
        emptyDebugRenderSkip = EMPTY_DEBUG_RENDER_SKIP.get();
        entityRendererCache = ENTITY_RENDERER_CACHE.get();
        blockEntityRendererCache = BLOCK_ENTITY_RENDERER_CACHE.get();
        sophisticatedStorageFaceCulling = SOPHISTICATED_STORAGE_FACE_CULLING.get();
        sophisticatedStorageCountCache = SOPHISTICATED_STORAGE_COUNT_CACHE.get();
        sophisticatedStorageFillFastPath = SOPHISTICATED_STORAGE_FILL_FAST_PATH.get();
        sophisticatedStorageRenderUpdateFilter = SOPHISTICATED_STORAGE_RENDER_UPDATE_FILTER.get();
        sophisticatedStorageDiagnostics = SOPHISTICATED_STORAGE_DIAGNOSTICS.get();
        verticalSectionCulling = VERTICAL_SECTION_CULLING.get();
        verticalSectionDistance = VERTICAL_SECTION_DISTANCE.get();
        horizontalSectionCulling = HORIZONTAL_SECTION_CULLING.get();
        horizontalSectionDistance = HORIZONTAL_SECTION_DISTANCE.get();
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
