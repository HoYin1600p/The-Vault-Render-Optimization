package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import dev.hoyin1600p.vault_render_optimization.backport.BootstrapRenderBackportConfig;
import dev.hoyin1600p.vault_render_optimization.backport.ModernFixOwnership;
import dev.hoyin1600p.vault_render_optimization.backport.RenderBackportFeature;
import dev.hoyin1600p.vault_render_optimization.backport.RenderBackportCompatibility;
import dev.hoyin1600p.vault_render_optimization.backport.RenderBackportOwnershipRegistry;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuEntityAudit;
import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleOptimizationState;
import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleCollisionState;
import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleMixinSelection;
import dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage.SophisticatedStorageCompatibility;
import dev.hoyin1600p.vault_render_optimization.client.chunk.ChunkUpdateBackend;
import dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast.VroImmediatelyFast;
import dev.hoyin1600p.vault_render_optimization.client.chunk.ChunkUpdateState;
import dev.hoyin1600p.vault_render_optimization.client.chunk.sorting.IndexSortCompatibility;
import dev.hoyin1600p.vault_render_optimization.client.chunk.sorting.IndexSortState;
import dev.hoyin1600p.vault_render_optimization.client.chunk.budget.AdaptiveBudgetState;
import dev.hoyin1600p.vault_render_optimization.client.chunk.budget.AdaptiveBudgetCompatibility;
import dev.hoyin1600p.vault_render_optimization.renderertransfer.BootstrapRendererTransferConfig;
import dev.hoyin1600p.vault_render_optimization.renderertransfer.RendererFamily;
import dev.hoyin1600p.vault_render_optimization.renderertransfer.RendererFamilyDetector;
import dev.hoyin1600p.vault_render_optimization.renderertransfer.RendererTransferBytecode;
import dev.hoyin1600p.vault_render_optimization.renderertransfer.RendererTransferFeature;
import dev.hoyin1600p.vault_render_optimization.renderertransfer.RendererTransferOwnershipRegistry;
import dev.hoyin1600p.vault_render_optimization.util.ClassBytes;
import dev.hoyin1600p.vault_render_optimization.util.ModIds;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class VaultRenderOptimizationMixinPlugin implements IMixinConfigPlugin {
    private static final String COLLISION_FIX_MOD_ID = ModIds.ENTITY_COLLISION_FPS_FIX;
    private static final Set<String> COLLISION_MIXINS = Set.of(
            "ClientEntityCollisionMixin",
            "ClientLivingEntityCollisionMixin"
    );
    private static final Set<String> BAD_OPTIMIZATIONS_EQUIVALENT_MIXINS = Set.of(
            "BlockEntityRenderDispatcherMixin",
            "BlockEntityTypeRendererCacheMixin",
            "DebugRendererMixin",
            "EntityRenderDispatcherMixin",
            "EntityTypeRendererCacheMixin",
            "GameTestDebugRendererMixin",
            "ParticleEngineMixin",
            "ToastComponentMixin",
            "TutorialMixin"
    );
    // Mods that replace or parallelize ParticleEngine ticking own the whole tick loop.
    private static final Set<String> PARTICLE_TICK_OWNER_MOD_IDS = Set.of(
            ModIds.PARTICLE_CORE,
            ModIds.FLEROVIUM,
            ModIds.ASYNC_PARTICLES
    );
    private static final Set<String> PARTICLE_LIGHT_CACHE_MOD_IDS = Set.of(
            ModIds.PARTICLE_CORE,
            ModIds.FLEROVIUM
    );
    private static final Set<String> SECTION_CULLING_MIXINS = Set.of(
            "LevelRendererSectionCullingMixin",
            "RenderChunkSectionCullingMixin",
            "SodiumRenderSectionAccessor",
            "SodiumRenderSectionManagerCullingMixin"
    );
    private static final Set<String> SODIUM_SECTION_CULLING_MIXINS = Set.of(
            "SodiumRenderSectionAccessor",
            "SodiumRenderSectionManagerCullingMixin"
    );
    private static final Set<String> SODIUM_RENDER_MOD_IDS = Set.of(
            ModIds.EMBEDDIUM,
            ModIds.RUBIDIUM,
            ModIds.SODIUM
    );
    private static final Set<String> UNOBTAINIUM_EQUIVALENT_MIXINS = Set.of(
            "EmptyItemStackEntityReferenceMixin",
            "ISpawnerRendererMixin",
            "VaultLootBeamsCacheAccessor",
            "VaultLootBeamsLazyTooltipMixin"
    );
    private static final Map<String, String> OPTIONAL_MIXIN_MODS = Map.ofEntries(
            Map.entry("AltarConduitClientCrashGuardMixin", "vaultintegrations"),
            Map.entry("ClientAbilityDataMixin", ModIds.THE_VAULT),
            Map.entry("ClientPacketListenerFarsightMixin", ModIds.FARSIGHT_VIEW),
            Map.entry("ForgetLevelChunkPacketFarsightMixin", ModIds.FARSIGHT_VIEW),
            Map.entry("CreateArmBoundsMixin", ModIds.CREATE),
            Map.entry("CreateBeltBoundsMixin", ModIds.CREATE),
            Map.entry("CreateCachedRenderBoundsAccessor", ModIds.CREATE),
            Map.entry("CreateBlockEntityRenderHelperMixin", ModIds.CREATE),
            Map.entry("CreateContraptionRenderDispatcherMixin", ModIds.CREATE),
            Map.entry("CreateContraptionRenderingWorldMixin", ModIds.CREATE),
            Map.entry("CreateDeployerBoundsMixin", ModIds.CREATE),
            Map.entry("CreateFlwContraptionMixin", ModIds.CREATE),
            Map.entry("FlywheelBackendOverrideMixin", ModIds.FLYWHEEL),
            Map.entry("CreatePortableStorageInterfaceBoundsMixin", ModIds.CREATE),
            Map.entry("CreateRollerBoundsMixin", ModIds.CREATE),
            Map.entry("CreateSbbContraptionManagerMixin", ModIds.CREATE),
            Map.entry("Matrix4fAccessor", ModIds.CREATE),
            Map.entry("ElixirOrbParticleMixin", ModIds.THE_VAULT),
            Map.entry("NovaCloudParticleRandomMixin", ModIds.THE_VAULT),
            Map.entry("NovaExplosionProviderRandomMixin", ModIds.THE_VAULT),
            Map.entry("NovaSpeedParticleRandomMixin", ModIds.THE_VAULT),
            Map.entry("ISpawnerRendererMixin", ModIds.ISPAWNER),
            Map.entry("CreateAdditionEnergyNetworkManagerAccessor", ModIds.CREATE_ADDITION),
            Map.entry("PowahCableNetAccessor", ModIds.POWAH),
            Map.entry("PowahCableNetClientCrashGuardMixin", ModIds.POWAH),
            Map.entry("ToolItemRendererMixin", ModIds.THE_VAULT),
            Map.entry("VaultArmorItemMixin", ModIds.THE_VAULT),
            Map.entry("VaultArmorRenderPropertiesMixin", ModIds.THE_VAULT),
            Map.entry("VaultDamageNumberRendererMixin", ModIds.THE_VAULT),
            Map.entry("VaultEventMixin", ModIds.THE_VAULT),
            Map.entry("VaultMapKeybindMixin", ModIds.THE_VAULT),
            Map.entry("VaultNativeShaderUniformMixin", ModIds.THE_VAULT),
            Map.entry("VaultLootBeamsCacheAccessor", ModIds.VAULT_LOOT_BEAMS),
            Map.entry("VaultLootBeamsLazyTooltipMixin", ModIds.VAULT_LOOT_BEAMS),
            Map.entry("XaeroLeveledRegionAccess", ModIds.XAERO_WORLD_MAP),
            Map.entry("XaeroMapCacheWriteGuardMixin", ModIds.XAERO_WORLD_MAP)
    );

    private LoadingModList loadingModList;
    private boolean physicalClient;
    private boolean modDiscoveryFailed;
    private boolean modernFixLoaded;
    private boolean fluidloggedLoaded;
    private boolean isometricRendersLoaded;
    private boolean witherStormModLoaded;
    private boolean rubidiumLoaded;
    private boolean embeddiumLoaded;
    private boolean sodiumLoaded;
    private boolean fleroviumLoaded;
    private boolean ctmCompatible;
    private boolean codeChickenLibLoaded;
    private boolean sophisticatedStorageCompatible;
    private RendererFamily rendererFamily = RendererFamily.NONE;
    private String rendererVersion;
    private ChunkUpdateBackend chunkUpdateBackend = ChunkUpdateBackend.BLOCKED;
    private boolean indexSortCompatible;
    private boolean adaptiveBudgetCompatible;

    @Override
    public void onLoad(String mixinPackage) {
        physicalClient = FMLEnvironment.dist == Dist.CLIENT;
        try {
            loadingModList = FMLLoader.getLoadingModList();
            modernFixLoaded = isModLoaded(ModIds.MODERNFIX);
            fluidloggedLoaded = isModLoaded(ModIds.FLUIDLOGGED);
            isometricRendersLoaded = isModLoaded(ModIds.ISOMETRIC_RENDERS);
            witherStormModLoaded = isModLoaded(ModIds.WITHER_STORM_MOD);
            rubidiumLoaded = isModLoaded(ModIds.RUBIDIUM);
            embeddiumLoaded = isModLoaded(ModIds.EMBEDDIUM);
            sodiumLoaded = isModLoaded(ModIds.SODIUM);
            fleroviumLoaded = isModLoaded(ModIds.FLEROVIUM);
            ctmCompatible = hasVersion("ctm", "1.18.2-1.1.5+5");
            codeChickenLibLoaded = isModLoaded(ModIds.CODE_CHICKEN_LIB);
            sophisticatedStorageCompatible = SophisticatedStorageCompatibility.supports(
                    modVersion(ModIds.SOPHISTICATED_STORAGE),
                    modVersion(ModIds.SOPHISTICATED_CORE)
            );
            rendererFamily = resolveRendererFamily();
            rendererVersion = rendererFamily == RendererFamily.EMBEDDIUM
                    ? modVersion(ModIds.EMBEDDIUM)
                    : rendererFamily == RendererFamily.RUBIDIUM ? modVersion(ModIds.RUBIDIUM) : null;
        } catch (RuntimeException | LinkageError failure) {
            modDiscoveryFailed = true;
            loadingModList = null;
            VaultRenderOptimization.LOGGER.debug(
                    "Loaded mods could not be queried during render-backport selection",
                    failure
            );
        }

        chunkUpdateBackend = ChunkUpdateBackend.select(
                physicalClient, modDiscoveryFailed,
                sodiumLoaded || resourceExists("net.optifine.Config")
                        || resourceExists("optifine.OptiFineTransformationService"),
                rendererFamily, rendererVersion
        );
        ChunkUpdateState.configure(chunkUpdateBackend);
        String indexSortBlocker = IndexSortCompatibility.blocker(chunkUpdateBackend, path -> ClassBytes.read(loadingModList, path));
        indexSortCompatible = indexSortBlocker == null;
        String budgetBlocker = AdaptiveBudgetCompatibility.blocker(indexSortBlocker, path -> ClassBytes.read(loadingModList, path));
        adaptiveBudgetCompatible = budgetBlocker == null;
        AdaptiveBudgetState.configure(adaptiveBudgetCompatible, adaptiveBudgetCompatible
                ? "validated Embeddium; requires effective deferred updates" : budgetBlocker);
        VaultRenderOptimization.LOGGER.info("Adaptive chunk budget hooks: {} - {}",
                adaptiveBudgetCompatible ? "AVAILABLE" : "BLOCKED",
                adaptiveBudgetCompatible ? "bytecode verified; runtime config/Compare Mode apply" : budgetBlocker);
        IndexSortState.configure(indexSortCompatible, indexSortCompatible
                ? "validated Embeddium index-only path; runtime config/Compare Mode apply" : indexSortBlocker);
        VaultRenderOptimization.LOGGER.info("Index-only sorting hooks: {} - {}",
                indexSortCompatible ? "AVAILABLE" : "BLOCKED", indexSortBlocker == null ? "bytecode verified" : indexSortBlocker);
        VaultRenderOptimization.LOGGER.info("Chunk-update deferral backend: {} (runtime config/Compare Mode apply)",
                chunkUpdateBackend);
        VaultRenderOptimization.LOGGER.info(
                "Sophisticated Storage rendering hooks: {} - {}",
                sophisticatedStorageCompatible ? "AVAILABLE" : "BLOCKED",
                sophisticatedStorageCompatible
                        ? "validated Sophisticated Storage 1.18.2-0.9.8.915 / Core 1.18.2-0.6.4.604"
                        : "requires the validated Sophisticated Storage 1.18.2-0.9.8.915 / Core 1.18.2-0.6.4.604 pair"
        );

        ParticleOptimizationState.configureEnvironment(
                !modDiscoveryFailed && (rubidiumLoaded || embeddiumLoaded || sodiumLoaded),
                !modDiscoveryFailed && fleroviumLoaded
        );
        boolean particleCollisionCompatible = !modDiscoveryFailed
                && PARTICLE_LIGHT_CACHE_MOD_IDS.stream().noneMatch(this::isModLoaded);
        ParticleCollisionState.configure(particleCollisionCompatible, particleCollisionCompatible
                ? "exact cached block collision; vanilla response retained"
                : "unknown mod discovery or Particle Core/Flerovium ownership");

        BootstrapRenderBackportConfig.capture();
        RenderBackportOwnershipRegistry.initialize(
                physicalClient,
                BootstrapRenderBackportConfig.compareMode(),
                BootstrapRenderBackportConfig::enabled,
                this::vhAcceleratorProvidesFeature,
                this::probeModernFixOwnership,
                this::probeBackportCompatibility
        );
        VaultRenderOptimization.LOGGER.info(
                "ModernFix render-backport ownership: {}",
                RenderBackportOwnershipRegistry.summary()
        );
        BootstrapRendererTransferConfig.capture();
        RendererTransferOwnershipRegistry.initialize(
                physicalClient,
                BootstrapRenderBackportConfig.compareMode(),
                BootstrapRendererTransferConfig::enabled,
                rendererFamily,
                rendererVersion,
                this::probeRendererTransferCompatibility
        );
        VaultRenderOptimization.LOGGER.info(
                "Renderer-transfer ownership: {}",
                RendererTransferOwnershipRegistry.summary()
        );
        RendererTransferOwnershipRegistry.reportLines().forEach(
                line -> VaultRenderOptimization.LOGGER.info("Renderer-transfer decision: {}", line)
        );
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith(".FrustumFastloadMixin")) {
            return physicalClient && !modDiscoveryFailed && isModLoaded(ModIds.FASTLOAD);
        }
        if (mixinClassName.contains(".entitygpu.")) {
            // Passive until the runtime gate (config, capabilities, self-test, audit) turns the path on.
            return physicalClient && !modDiscoveryFailed && !resourceExists("net.optifine.Config")
                    && !resourceExists("optifine.OptiFineTransformationService");
        }
        if (mixinClassName.endsWith(".HudFontGeometryMixin") || mixinClassName.endsWith(".HudFontGenerationMixin")) {
            return physicalClient && !modDiscoveryFailed && !isModLoaded(ModIds.MODERN_UI)
                    && !VroImmediatelyFast.ownsText() && !isModLoaded(ModIds.EXORDIUM)
                    && !isModLoaded(ModIds.SMOOTH_FONT);
        }
        if (mixinClassName.contains(".sophisticatedstorage.")) {
            return physicalClient && !modDiscoveryFailed && sophisticatedStorageCompatible;
        }
        if (mixinClassName.endsWith(".chunk.EmbeddiumAdaptiveBudgetMixin")
                || mixinClassName.endsWith(".chunk.EmbeddiumBudgetBuilderMixin")) return adaptiveBudgetCompatible;
        if (mixinClassName.endsWith(".chunk.EmbeddiumIndexSortTaskMixin")
                || mixinClassName.endsWith(".chunk.EmbeddiumIndexSortUploadMixin")) {
            return indexSortCompatible;
        }
        if (mixinClassName.endsWith(".chunk.VanillaDeferredChunkUpdatesMixin")) {
            return chunkUpdateBackend == ChunkUpdateBackend.VANILLA;
        }
        if (mixinClassName.endsWith(".chunk.EmbeddiumDeferredChunkUpdatesMixin")) {
            return chunkUpdateBackend == ChunkUpdateBackend.EMBEDDIUM;
        }
        if (mixinClassName.endsWith(".chunk.RubidiumDeferredChunkUpdatesMixin")) {
            return chunkUpdateBackend == ChunkUpdateBackend.RUBIDIUM;
        }

        RendererTransferFeature rendererTransfer = RendererTransferFeature.forMixin(mixinClassName);
        if (rendererTransfer != null) {
            return RendererTransferOwnershipRegistry.applies(rendererTransfer, mixinClassName);
        }

        RenderBackportFeature renderBackport = RenderBackportFeature.forMixin(mixinClassName);
        if (renderBackport != null) {
            return RenderBackportOwnershipRegistry.vroOwns(renderBackport);
        }

        String simpleName = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);

        if (COLLISION_MIXINS.contains(simpleName)) {
            return !isModLoaded(COLLISION_FIX_MOD_ID);
        }

        if (BAD_OPTIMIZATIONS_EQUIVALENT_MIXINS.contains(simpleName) && isModLoaded(ModIds.BAD_OPTIMIZATIONS)) {
            return false;
        }

        if (simpleName.equals("ParticleEngineCompactionMixin") || simpleName.equals("ParticleRandomMixin")
                || simpleName.equals("ClientPacketListenerParticleRandomMixin")
                || simpleName.equals("ParticleProviderCacheMixin")) {
            return !modDiscoveryFailed && PARTICLE_TICK_OWNER_MOD_IDS.stream().noneMatch(this::isModLoaded);
        }

        if (simpleName.equals("ParticleCollisionMixin") || simpleName.equals("ParticleEngineCollisionScopeMixin")) {
            return !modDiscoveryFailed && PARTICLE_LIGHT_CACHE_MOD_IDS.stream().noneMatch(this::isModLoaded);
        }

        if (simpleName.equals("ParticleLightCacheMixin")
                && PARTICLE_LIGHT_CACHE_MOD_IDS.stream().anyMatch(this::isModLoaded)) {
            return false;
        }

        if (simpleName.equals("SodiumSingleQuadParticleMixin")) {
            return ParticleMixinSelection.rendererPath(
                    modDiscoveryFailed,
                    rubidiumLoaded || embeddiumLoaded || sodiumLoaded,
                    fleroviumLoaded
            );
        }

        if (simpleName.equals("SingleQuadParticleMixin")) {
            return ParticleMixinSelection.portablePath(
                    modDiscoveryFailed,
                    rubidiumLoaded || embeddiumLoaded || sodiumLoaded,
                    fleroviumLoaded
            );
        }

        if (SECTION_CULLING_MIXINS.contains(simpleName)) {
            if (isModLoaded(ModIds.BETTER_FPS_DIST)) {
                return false;
            }
            if (SODIUM_SECTION_CULLING_MIXINS.contains(simpleName)) {
                return SODIUM_RENDER_MOD_IDS.stream().anyMatch(this::isModLoaded);
            }
        }

        if (UNOBTAINIUM_EQUIVALENT_MIXINS.contains(simpleName) && isModLoaded(ModIds.UNOBTAINIUM)) {
            return false;
        }

        String requiredMod = OPTIONAL_MIXIN_MODS.get(simpleName);
        return requiredMod == null || isModLoaded(requiredMod);
    }

    private boolean isModLoaded(String modId) {
        LoadingModList modList = loadingModList != null ? loadingModList : FMLLoader.getLoadingModList();
        return modList != null && modList.getModFileById(modId) != null;
    }

    private boolean vhAcceleratorProvidesFeature(RenderBackportFeature feature) {
        if (modDiscoveryFailed || loadingModList == null) {
            return false;
        }
        for (String markerClass : feature.vhAcceleratorMarkerClasses()) {
            try {
                if (loadingModList.findResource(markerClass.replace('.', '/') + ".class") != null) {
                    return true;
                }
            } catch (RuntimeException | LinkageError failure) {
                VaultRenderOptimization.LOGGER.debug(
                        "Could not locate the VH Accelerator overlap marker for {}",
                        feature.id(),
                        failure
                );
                return true;
            }
        }
        return false;
    }

    private ModernFixOwnership probeModernFixOwnership(RenderBackportFeature feature) {
        if (modDiscoveryFailed) {
            return ModernFixOwnership.UNKNOWN;
        }
        if (!modernFixLoaded) {
            return ModernFixOwnership.ABSENT;
        }

        if (!feature.modernFixMarkerClasses().isEmpty()) {
            boolean markerPresent = false;
            for (String markerClass : feature.modernFixMarkerClasses()) {
                try {
                    if (loadingModList != null
                            && loadingModList.findResource(
                            markerClass.replace('.', '/') + ".class"
                    ) != null) {
                        markerPresent = true;
                        break;
                    }
                } catch (RuntimeException | LinkageError failure) {
                    VaultRenderOptimization.LOGGER.debug(
                            "Could not locate the ModernFix marker for {}",
                            feature.id(),
                            failure
                    );
                    return ModernFixOwnership.UNKNOWN;
                }
            }
            if (!markerPresent) {
                return ModernFixOwnership.INACTIVE;
            }
        }

        if (feature.modernFixMixinKeys().isEmpty()) {
            return ModernFixOwnership.INACTIVE;
        }
        try {
            Class<?> pluginClass = Class.forName(
                    "org.embeddedt.modernfix.core.ModernFixMixinPlugin",
                    false,
                    VaultRenderOptimizationMixinPlugin.class.getClassLoader()
            );
            Field instanceField = pluginClass.getField("instance");
            Object instance = instanceField.get(null);
            if (instance == null) {
                return ModernFixOwnership.UNKNOWN;
            }
            Method optionMethod = pluginClass.getMethod("isOptionEnabled", String.class);
            for (String option : feature.modernFixMixinKeys()) {
                if (Boolean.TRUE.equals(optionMethod.invoke(instance, option))) {
                    return ModernFixOwnership.ACTIVE;
                }
            }
            return ModernFixOwnership.INACTIVE;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            VaultRenderOptimization.LOGGER.debug(
                    "Could not query ModernFix ownership for {}",
                    feature.id(),
                    failure
            );
            return ModernFixOwnership.UNKNOWN;
        }
    }

    private String probeBackportCompatibility(RenderBackportFeature feature) {
        return RenderBackportCompatibility.blocker(
                feature,
                modDiscoveryFailed,
                fluidloggedLoaded,
                isometricRendersLoaded,
                witherStormModLoaded,
                rubidiumLoaded,
                embeddiumLoaded,
                ctmCompatible
        );
    }

    private String probeRendererTransferCompatibility(RendererTransferFeature feature) {
        if (modDiscoveryFailed || loadingModList == null) {
            return "renderer discovery failed";
        }
        if (feature == RendererTransferFeature.DIRECT_CCL_RENDERER_LOOKUP) {
            if (!codeChickenLibLoaded) {
                return "CodeChickenLib is not installed";
            }
            if (!resourceExists("org.embeddedt.embeddium.compat.ccl.CCLCompat")) {
                return "the validated Embeddium CodeChickenLib bridge is absent";
            }
        }
        return RendererTransferBytecode.blocker(feature, this::resourceBytes);
    }

    private byte[] resourceBytes(String path) {
        return ClassBytes.read(loadingModList, path);
    }

    private RendererFamily resolveRendererFamily() {
        return RendererFamilyDetector.resolve(
                embeddiumLoaded,
                rubidiumLoaded,
                embeddiumLoaded && rubidiumLoaded
                        && loadingModList.getModFileById(ModIds.EMBEDDIUM)
                        == loadingModList.getModFileById(ModIds.RUBIDIUM)
        );
    }

    private String modVersion(String modId) {
        if (loadingModList == null) {
            return null;
        }
        return loadingModList.getMods().stream()
                .filter(mod -> modId.equals(mod.getModId()))
                .map(mod -> mod.getVersion().toString())
                .findFirst()
                .orElse(null);
    }

    private boolean resourceExists(String className) {
        try {
            return loadingModList != null
                    && loadingModList.findResource(className.replace('.', '/') + ".class") != null;
        } catch (RuntimeException | LinkageError failure) {
            VaultRenderOptimization.LOGGER.debug("Could not probe optional renderer class {}", className, failure);
            return false;
        }
    }

    private boolean hasVersion(String modId, String expectedVersion) {
        if (loadingModList == null || loadingModList.getModFileById(modId) == null) {
            return false;
        }
        return loadingModList.getMods().stream()
                .filter(mod -> modId.equals(mod.getModId()))
                .anyMatch(mod -> expectedVersion.equals(mod.getVersion().toString()));
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (mixinClassName.endsWith(".FrustumFastloadMixin")) {
            dev.hoyin1600p.vault_render_optimization.client.render.FastloadFrustum.audit(targetClass);
        }
        if (mixinClassName.contains(".entitygpu.") && !mixinClassName.endsWith("Accessor")
                && !mixinClassName.endsWith(".LevelRendererGpuMixin")
                && !mixinClassName.endsWith(".BufferSourceGpuHintMixin")
                && !mixinClassName.endsWith(".OculusBufferSourceGpuHintMixin")
                && !mixinClassName.endsWith(".ImmediatelyFastBufferSourceGpuHintMixin")
                && !mixinClassName.endsWith(".OculusSegmentedBufferBuilderGpuMixin")
                && !mixinClassName.endsWith(".GeoEntityRendererGpuMixin")
                && !mixinClassName.endsWith(".ArsGeoEntityRendererGpuMixin")
                && !mixinClassName.endsWith(".GeoCubeGpuSlotMixin")) {
            GpuEntityAudit.audit(targetClass.name, targetClass, mixinClassName);
        }
    }
}
