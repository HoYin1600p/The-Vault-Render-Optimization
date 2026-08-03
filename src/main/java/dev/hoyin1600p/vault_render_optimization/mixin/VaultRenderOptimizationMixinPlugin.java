package dev.hoyin1600p.vault_render_optimization.mixin;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
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
    private static final Map<String, String> OPTIONAL_MIXIN_MODS = Map.ofEntries(
            Map.entry("AltarConduitClientCrashGuardMixin", "vaultintegrations"),
            Map.entry("ClientAbilityDataMixin", "the_vault"),
            Map.entry("ElixirOrbParticleMixin", "the_vault"),
            Map.entry("CreateAdditionEnergyNetworkManagerAccessor", "createaddition"),
            Map.entry("PowahCableNetAccessor", "powah"),
            Map.entry("PowahCableNetClientCrashGuardMixin", "powah"),
            Map.entry("ToolItemRendererMixin", "the_vault"),
            Map.entry("VaultArmorItemMixin", "the_vault"),
            Map.entry("VaultArmorRenderPropertiesMixin", "the_vault"),
            Map.entry("VaultDamageNumberRendererMixin", "the_vault"),
            Map.entry("VaultEventMixin", "the_vault"),
            Map.entry("VaultMapKeybindMixin", "the_vault")
    );

    private LoadingModList loadingModList;

    @Override
    public void onLoad(String mixinPackage) {
        loadingModList = FMLLoader.getLoadingModList();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
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

        if (SECTION_CULLING_MIXINS.contains(simpleName)) {
            if (isModLoaded("betterfpsdist")) {
                return false;
            }
            if (SODIUM_SECTION_CULLING_MIXINS.contains(simpleName)) {
                return SODIUM_RENDER_MOD_IDS.stream().anyMatch(this::isModLoaded);
            }
        }

        String requiredMod = OPTIONAL_MIXIN_MODS.get(simpleName);
        return requiredMod == null || isModLoaded(requiredMod);
    }

    private boolean isModLoaded(String modId) {
        LoadingModList modList = loadingModList != null ? loadingModList : FMLLoader.getLoadingModList();
        return modList != null && modList.getModFileById(modId) != null;
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
