package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.mixin;

import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.forgespi.language.IModInfo;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class FlywheelCompatMixinPlugin implements IMixinConfigPlugin {
    private static final String STARTUP_PROPERTY = "vault_render_optimization.flywheelShaderCompat";

    private boolean compatibilityAvailable;

    @Override
    public void onLoad(String mixinPackage) {
        LoadingModList modList = FMLLoader.getLoadingModList();
        boolean enabled = Boolean.parseBoolean(System.getProperty(STARTUP_PROPERTY, "true"));
        String oculusVersion = version(modList, "oculus");
        String flywheelVersion = version(modList, "flywheel");
        boolean rendererLoaded = loaded(modList, "rubidium") || loaded(modList, "embeddium");
        boolean supportedOculus = oculusVersion.startsWith("1.6.");
        boolean supportedFlywheel = flywheelVersion.startsWith("0.6.11");

        compatibilityAvailable = FMLLoader.getDist() == Dist.CLIENT
                && enabled
                && loaded(modList, "create")
                && rendererLoaded
                && supportedOculus
                && supportedFlywheel;

        if (!enabled) {
            VaultRenderOptimization.LOGGER.info(
                    "Create shader instancing compatibility disabled by -D{}=false",
                    STARTUP_PROPERTY
            );
        } else if (loaded(modList, "oculus") && !supportedOculus) {
            VaultRenderOptimization.LOGGER.warn(
                    "Oculus {} is outside the tested 1.6.x line; Create shader instancing compatibility is disabled",
                    oculusVersion
            );
        } else if (loaded(modList, "flywheel") && !supportedFlywheel) {
            VaultRenderOptimization.LOGGER.warn(
                    "Flywheel {} is outside the tested 0.6.11 line; Create shader instancing compatibility is disabled",
                    flywheelVersion
            );
        } else if (compatibilityAvailable) {
            VaultRenderOptimization.LOGGER.info(
                    "Loading Create shader instancing compatibility for Oculus {} and Flywheel {}",
                    oculusVersion,
                    flywheelVersion
            );
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return compatibilityAvailable;
    }

    private static boolean loaded(LoadingModList modList, String modId) {
        return modList != null && modList.getModFileById(modId) != null;
    }

    private static String version(LoadingModList modList, String modId) {
        if (!loaded(modList, modId)) {
            return "";
        }
        return modList.getModFileById(modId).getMods().stream()
                .filter(mod -> modId.equals(mod.getModId()))
                .map(IModInfo::getVersion)
                .map(Object::toString)
                .findFirst()
                .orElse("");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
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
