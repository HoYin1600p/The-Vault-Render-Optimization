package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import dev.hoyin1600p.vault_render_optimization.backport.BootstrapRenderBackportConfig;
import dev.hoyin1600p.vault_render_optimization.backport.ModernFixOwnership;
import dev.hoyin1600p.vault_render_optimization.backport.RenderBackportFeature;
import dev.hoyin1600p.vault_render_optimization.backport.RenderBackportCompatibility;
import dev.hoyin1600p.vault_render_optimization.backport.RenderBackportOwnershipRegistry;
import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleOptimizationState;
import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleMixinSelection;
import dev.hoyin1600p.vault_render_optimization.client.chunk.ChunkUpdateBackend;
import dev.hoyin1600p.vault_render_optimization.client.chunk.ChunkUpdateState;
import dev.hoyin1600p.vault_render_optimization.client.chunk.sorting.IndexSortCompatibility;
import dev.hoyin1600p.vault_render_optimization.client.chunk.sorting.IndexSortState;
import dev.hoyin1600p.vault_render_optimization.renderertransfer.BootstrapRendererTransferConfig;
import dev.hoyin1600p.vault_render_optimization.renderertransfer.RendererFamily;
import dev.hoyin1600p.vault_render_optimization.renderertransfer.RendererFamilyDetector;
import dev.hoyin1600p.vault_render_optimization.renderertransfer.RendererTransferFeature;
import dev.hoyin1600p.vault_render_optimization.renderertransfer.RendererTransferOwnershipRegistry;
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
    private static final String COLLISION_FIX_MOD_ID = "entitycollisionfpsfix";
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
    private static final Set<String> PARTICLE_LIGHT_CACHE_MOD_IDS = Set.of(
            "particle_core",
            "flerovium"
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
            "embeddium",
            "rubidium",
            "sodium"
    );
    private static final Set<String> DYNAMIC_LIGHT_MIXINS = Set.of(
            "EntityDynamicLightMixin",
            "EntityRendererDynamicLightMixin",
            "LevelDynamicLightMixin",
            "LevelRendererDynamicLightMixin",
            "MinecraftDynamicLightMixin"
    );
    private static final Set<String> UNOBTANIUM_EQUIVALENT_MIXINS = Set.of(
            "EmptyItemStackEntityReferenceMixin",
            "ISpawnerRendererMixin",
            "VaultLootBeamsCacheAccessor",
            "VaultLootBeamsLazyTooltipMixin"
    );
    private static final Map<String, String> OPTIONAL_MIXIN_MODS = Map.ofEntries(
            Map.entry("AltarConduitClientCrashGuardMixin", "vaultintegrations"),
            Map.entry("ClientAbilityDataMixin", "the_vault"),
            Map.entry("CreateArmBoundsMixin", "create"),
            Map.entry("CreateBeltBoundsMixin", "create"),
            Map.entry("CreateCachedRenderBoundsAccessor", "create"),
            Map.entry("CreateBlockEntityRenderHelperMixin", "create"),
            Map.entry("CreateContraptionRenderDispatcherMixin", "create"),
            Map.entry("CreateDeployerBoundsMixin", "create"),
            Map.entry("CreateFlwContraptionMixin", "create"),
            Map.entry("CreatePortableStorageInterfaceBoundsMixin", "create"),
            Map.entry("CreateRollerBoundsMixin", "create"),
            Map.entry("CreateSbbContraptionManagerMixin", "create"),
            Map.entry("Matrix4fAccessor", "create"),
            Map.entry("ElixirOrbParticleMixin", "the_vault"),
            Map.entry("ISpawnerRendererMixin", "ispawner"),
            Map.entry("CreateAdditionEnergyNetworkManagerAccessor", "createaddition"),
            Map.entry("PowahCableNetAccessor", "powah"),
            Map.entry("PowahCableNetClientCrashGuardMixin", "powah"),
            Map.entry("ToolItemRendererMixin", "the_vault"),
            Map.entry("VaultArmorItemMixin", "the_vault"),
            Map.entry("VaultArmorRenderPropertiesMixin", "the_vault"),
            Map.entry("VaultDamageNumberRendererMixin", "the_vault"),
            Map.entry("VaultEventMixin", "the_vault"),
            Map.entry("VaultMapKeybindMixin", "the_vault"),
            Map.entry("VaultNativeShaderUniformMixin", "the_vault"),
            Map.entry("VaultLootBeamsCacheAccessor", "vaultlootbeams"),
            Map.entry("VaultLootBeamsLazyTooltipMixin", "vaultlootbeams"),
            Map.entry("XaeroLeveledRegionAccess", "xaeroworldmap"),
            Map.entry("XaeroMapCacheWriteGuardMixin", "xaeroworldmap")
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
    private RendererFamily rendererFamily = RendererFamily.NONE;
    private String rendererVersion;
    private ChunkUpdateBackend chunkUpdateBackend = ChunkUpdateBackend.BLOCKED;
    private boolean indexSortCompatible;

    @Override
    public void onLoad(String mixinPackage) {
        physicalClient = FMLEnvironment.dist == Dist.CLIENT;
        try {
            loadingModList = FMLLoader.getLoadingModList();
            modernFixLoaded = isModLoaded("modernfix");
            fluidloggedLoaded = isModLoaded("fluidlogged");
            isometricRendersLoaded = isModLoaded("isometric-renders");
            witherStormModLoaded = isModLoaded("witherstormmod");
            rubidiumLoaded = isModLoaded("rubidium");
            embeddiumLoaded = isModLoaded("embeddium");
            sodiumLoaded = isModLoaded("sodium");
            fleroviumLoaded = isModLoaded("flerovium");
            ctmCompatible = hasVersion("ctm", "1.18.2-1.1.5+5");
            codeChickenLibLoaded = isModLoaded("codechickenlib");
            rendererFamily = resolveRendererFamily();
            rendererVersion = rendererFamily == RendererFamily.EMBEDDIUM
                    ? modVersion("embeddium")
                    : rendererFamily == RendererFamily.RUBIDIUM ? modVersion("rubidium") : null;
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
        String indexSortBlocker = IndexSortCompatibility.blocker(chunkUpdateBackend, path -> {
            try {
                var resource = loadingModList.findResource(path);
                return resource == null ? null : java.nio.file.Files.readAllBytes(resource);
            } catch (java.io.IOException failure) {
                throw new java.io.UncheckedIOException(failure);
            }
        });
        indexSortCompatible = indexSortBlocker == null;
        IndexSortState.configure(indexSortCompatible, indexSortCompatible
                ? "validated Embeddium index-only path; runtime config/Compare Mode apply" : indexSortBlocker);
        VaultRenderOptimization.LOGGER.info("Index-only sorting hooks: {} - {}",
                indexSortCompatible ? "AVAILABLE" : "BLOCKED", indexSortBlocker == null ? "bytecode verified" : indexSortBlocker);
        VaultRenderOptimization.LOGGER.info("Chunk-update deferral backend: {} (runtime config/Compare Mode apply)",
                chunkUpdateBackend);

        ParticleOptimizationState.configureEnvironment(
                !modDiscoveryFailed && (rubidiumLoaded || embeddiumLoaded || sodiumLoaded),
                !modDiscoveryFailed && fleroviumLoaded
        );

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

        if (BAD_OPTIMIZATIONS_EQUIVALENT_MIXINS.contains(simpleName) && isModLoaded("badoptimizations")) {
            return false;
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
            if (isModLoaded("betterfpsdist")) {
                return false;
            }
            if (SODIUM_SECTION_CULLING_MIXINS.contains(simpleName)) {
                return SODIUM_RENDER_MOD_IDS.stream().anyMatch(this::isModLoaded);
            }
        }

        if (DYNAMIC_LIGHT_MIXINS.contains(simpleName) && isModLoaded("dynamiclightsreforged")) {
            return false;
        }

        if (UNOBTANIUM_EQUIVALENT_MIXINS.contains(simpleName) && isModLoaded("unobtainium")) {
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
        return null;
    }

    private RendererFamily resolveRendererFamily() {
        return RendererFamilyDetector.resolve(
                embeddiumLoaded,
                rubidiumLoaded,
                embeddiumLoaded && rubidiumLoaded
                        && loadingModList.getModFileById("embeddium")
                        == loadingModList.getModFileById("rubidium")
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
    }
}
