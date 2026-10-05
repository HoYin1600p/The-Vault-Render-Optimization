package dev.hoyin1600p.vault_render_optimization.config;

import dev.hoyin1600p.vault_render_optimization.config.model.ParticleBillboardOwner;

import dev.hoyin1600p.vault_render_optimization.config.model.UpdateNoticeFilter;

import dev.hoyin1600p.vault_render_optimization.config.spec.BackportSpec;
import dev.hoyin1600p.vault_render_optimization.config.spec.BenchmarkSpec;
import dev.hoyin1600p.vault_render_optimization.config.spec.ChunkSpec;
import dev.hoyin1600p.vault_render_optimization.config.spec.CreateSpec;
import dev.hoyin1600p.vault_render_optimization.config.spec.EmptyRenderSkipSpec;
import dev.hoyin1600p.vault_render_optimization.config.spec.GpuSpec;
import dev.hoyin1600p.vault_render_optimization.config.spec.RenderFastPathSpec;
import dev.hoyin1600p.vault_render_optimization.config.spec.RendererSpec;
import dev.hoyin1600p.vault_render_optimization.config.spec.SectionCullingSpec;
import dev.hoyin1600p.vault_render_optimization.config.spec.StorageSpec;
import dev.hoyin1600p.vault_render_optimization.config.spec.TickLookupSpec;
import dev.hoyin1600p.vault_render_optimization.config.spec.UpdateSpec;
import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import dev.hoyin1600p.vault_render_optimization.backport.RenderBackportFeature;
import dev.hoyin1600p.vault_render_optimization.renderertransfer.RendererTransferFeature;
import dev.hoyin1600p.vault_render_optimization.cache.VaultGearRenderCache;
import dev.hoyin1600p.vault_render_optimization.cache.VaultToolRenderCache;
import dev.hoyin1600p.vault_render_optimization.util.ConfigSections;
import dev.hoyin1600p.vault_render_optimization.util.ModIds;
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
    public static volatile boolean gpuEntityModels = false;
    public static volatile boolean gpuParticles = false;
    public static volatile boolean gpuEntityModelsWithShaders = false;
    public static volatile boolean gpuParticlesWithShaders = false;
    public static volatile boolean gpuItems = false;
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
        UpdateSpec updates = new UpdateSpec(builder);
        UPDATE_CHECKS = updates.updateChecks;
        UPDATE_NOTICE_FILTER = updates.updateNoticeFilter;

        BenchmarkSpec benchmark = new BenchmarkSpec(builder);
        COMPARE_MODE = benchmark.compareMode;

        ChunkSpec chunks = new ChunkSpec(builder);
        DEFER_CHUNK_UPDATES = chunks.deferChunkUpdates;
        INDEX_ONLY_SORTING = chunks.indexOnlySorting;
        SORT_GEOMETRY_CACHE = chunks.sortGeometryCache;
        ADAPTIVE_CHUNK_BUDGET = chunks.adaptiveChunkBudget;
        FARSIGHT_CHUNK_BOUND = chunks.farsightChunkBound;

        BackportSpec backports = new BackportSpec(builder, RENDER_BACKPORT_OPTIONS);

        RendererSpec renderer = new RendererSpec(builder, RENDERER_TRANSFER_OPTIONS);
        VERTEX_BUFFER_MAX_RETAINED_MIB = renderer.vertexBufferMaxRetainedMib;
        VERTEX_BUFFER_ADAPTIVE_TRIMMING = renderer.vertexBufferAdaptiveTrimming;
        VERTEX_BUFFER_AGGREGATE_RETAINED_MIB = renderer.vertexBufferAggregateRetainedMib;
        ASYNC_ARENA_GROWTH_DIVISOR = renderer.asyncArenaGrowthDivisor;
        ASYNC_ARENA_MAX_HEADROOM_MIB = renderer.asyncArenaMaxHeadroomMib;

        builder.push(ConfigSections.RENDER_FAST_PATHS);
        RenderFastPathSpec fastPaths = new RenderFastPathSpec(builder);
        HUD_TEXT_GEOMETRY = fastPaths.hudTextGeometry;
        PARTICLE_COLLISION_CACHE = fastPaths.particleCollisionCache;
        PARTICLE_LIGHT_CACHE = fastPaths.particleLightCache;
        PARTICLE_SHARED_LIGHT_CACHE = fastPaths.particleSharedLightCache;
        PARTICLE_BILLBOARD_FAST_PATH = fastPaths.particleBillboardFastPath;
        PARTICLE_BILLBOARD_OWNER = fastPaths.particleBillboardOwner;
        PARTICLE_DIAGNOSTICS = fastPaths.particleDiagnostics;
        EMPTY_PARTICLE_RENDER_SKIP = fastPaths.emptyParticleRenderSkip;
        ALLOCATION_FREE_FRUSTUM = fastPaths.allocationFreeFrustum;
        FASTLOAD_FRUSTUM_BYPASS = fastPaths.fastloadFrustumBypass;
        PARTICLE_TICK_COMPACTION = fastPaths.particleTickCompaction;
        PARTICLE_SHARED_RANDOM = fastPaths.particleSharedRandom;
        PARTICLE_PROVIDER_CACHE = fastPaths.particleProviderCache;
        GpuSpec gpu = new GpuSpec(builder);
        GPU_ENTITY_MODELS = gpu.gpuEntityModels;
        GPU_PARTICLES = gpu.gpuParticles;
        GPU_ENTITY_MODELS_WITH_SHADERS = gpu.gpuEntityModelsWithShaders;
        GPU_PARTICLES_WITH_SHADERS = gpu.gpuParticlesWithShaders;
        GPU_ITEMS = gpu.gpuItems;
        EmptyRenderSkipSpec emptySkips = new EmptyRenderSkipSpec(builder);
        EMPTY_TOAST_RENDER_SKIP = emptySkips.emptyToastRenderSkip;
        EMPTY_DEBUG_RENDER_SKIP = emptySkips.emptyDebugRenderSkip;
        builder.pop();

        TickLookupSpec tickLookup = new TickLookupSpec(builder);
        INACTIVE_TUTORIAL_SKIP = tickLookup.inactiveTutorialSkip;
        ENTITY_RENDERER_CACHE = tickLookup.entityRendererCache;
        BLOCK_ENTITY_RENDERER_CACHE = tickLookup.blockEntityRendererCache;

        StorageSpec storage = new StorageSpec(builder);
        SOPHISTICATED_STORAGE_FACE_CULLING = storage.sophisticatedStorageFaceCulling;
        SOPHISTICATED_STORAGE_COUNT_CACHE = storage.sophisticatedStorageCountCache;
        SOPHISTICATED_STORAGE_FILL_FAST_PATH = storage.sophisticatedStorageFillFastPath;
        SOPHISTICATED_STORAGE_RENDER_UPDATE_FILTER = storage.sophisticatedStorageRenderUpdateFilter;
        SOPHISTICATED_STORAGE_DIAGNOSTICS = storage.sophisticatedStorageDiagnostics;

        SectionCullingSpec culling = new SectionCullingSpec(builder);
        VERTICAL_SECTION_CULLING = culling.verticalSectionCulling;
        VERTICAL_SECTION_DISTANCE = culling.verticalSectionDistance;
        HORIZONTAL_SECTION_CULLING = culling.horizontalSectionCulling;
        HORIZONTAL_SECTION_DISTANCE = culling.horizontalSectionDistance;

        CreateSpec create = new CreateSpec(builder);
        CREATE_EMPTY_BUFFER_FLUSH_SKIP = create.createEmptyBufferFlushSkip;
        CREATE_BLOCK_ENTITY_CULLING = create.createBlockEntityCulling;
        CREATE_ACTOR_CULLING = create.createActorCulling;
        CREATE_SECTIONED_CONTRAPTION_MESHES = create.createSectionedContraptionMeshes;
        CREATE_SECTIONED_MESH_THRESHOLD = create.createSectionedMeshThreshold;
        CREATE_SMART_RENDER_BOUNDS = create.createSmartRenderBounds;
        CREATE_FLYWHEEL_AUTO_ENABLE = create.createFlywheelAutoEnable;
        CREATE_FLYWHEEL_SHADER_COMPAT = create.createFlywheelShaderCompat;
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
        ConfigChangeListeners.updateChecksChanged(enabled);
    }

    public static void setUpdateNoticeFilter(UpdateNoticeFilter filter) {
        UpdateNoticeFilter safeFilter = UpdateNoticeFilter.fromConfigValue(
                filter,
                UpdateNoticeFilter.CRITICAL
        );
        UPDATE_NOTICE_FILTER.set(safeFilter);
        UPDATE_NOTICE_FILTER.save();
        updateFilter = safeFilter;
        ConfigChangeListeners.updateFilterChanged(safeFilter);
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
        if (!ModList.get().isLoaded(ModIds.CREATE)) {
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
        ConfigChangeListeners.hudTextGeometryChanged();
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

    /** The new particle set: collision cache, tick compaction, shared random and provider cache. */
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
        if (ModList.get().isLoaded(ModIds.FLYWHEEL) && ModList.get().isLoaded(ModIds.OCULUS)) {
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
        if (event.getConfig().getSpec() == SPEC) {
            GpuOptInMigration.apply(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get(), () -> {
                setGpuEntityModels(false);
                setGpuItems(false);
                setGpuParticles(false);
            }, (message, failure) -> VaultRenderOptimization.LOGGER.warn(message, failure));
        }
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
        ConfigChangeListeners.updateChecksChanged(updateChecks);
        ConfigChangeListeners.updateFilterChanged(updateFilter);
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
