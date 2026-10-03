package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.mixin;

import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.OculusCompatContract;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.forgespi.language.IModInfo;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class FlywheelCompatMixinPlugin implements IMixinConfigPlugin {
    private static final String STARTUP_PROPERTY = "vault_render_optimization.flywheelShaderCompat";
    private static final String COMPAT_PACKAGE = "dev/hoyin1600p/vault_render_optimization/compat/flywheelshader";

    // One decision for all three shader-compat mixin configs, which share this plugin class.
    private static Boolean compatibilityAvailable;

    @Override
    public void onLoad(String mixinPackage) {
        decide();
    }

    private static synchronized boolean decide() {
        if (compatibilityAvailable == null) {
            compatibilityAvailable = evaluate(FMLLoader.getLoadingModList());
        }
        return compatibilityAvailable;
    }

    private static boolean evaluate(LoadingModList modList) {
        boolean enabled = Boolean.parseBoolean(System.getProperty(STARTUP_PROPERTY, "true"));
        String oculusVersion = version(modList, "oculus");
        String flywheelVersion = version(modList, "flywheel");
        boolean rendererLoaded = loaded(modList, "rubidium") || loaded(modList, "embeddium");
        boolean supportedOculus = oculusVersion.startsWith("1.6.");
        boolean supportedFlywheel = flywheelVersion.startsWith("0.6.11");

        if (!enabled) {
            VaultRenderOptimization.LOGGER.info(
                    "Create shader instancing compatibility disabled by -D{}=false", STARTUP_PROPERTY);
            return false;
        }
        if (loaded(modList, "oculus") && !supportedOculus) {
            VaultRenderOptimization.LOGGER.warn(
                    "Oculus {} is outside the tested 1.6.x line; Create shader instancing compatibility is disabled",
                    oculusVersion);
            return false;
        }
        if (loaded(modList, "flywheel") && !supportedFlywheel) {
            VaultRenderOptimization.LOGGER.warn(
                    "Flywheel {} is outside the tested 0.6.11 line; Create shader instancing compatibility is disabled",
                    flywheelVersion);
            return false;
        }
        if (FMLLoader.getDist() != Dist.CLIENT || !loaded(modList, "create") || !rendererLoaded
                || !loaded(modList, "oculus") || !loaded(modList, "flywheel")) {
            return false;
        }

        // Several Oculus builds share the 1.6.x line (and some a version string), so check that every
        // Oculus class and member VRO's compat code uses exists before any of its mixins apply.
        List<String> problems;
        try {
            problems = OculusCompatContract.problems(vroCompatClasses(modList), name -> classBytes(modList, "oculus", name));
        } catch (RuntimeException failure) {
            VaultRenderOptimization.LOGGER.warn(
                    "Could not verify Oculus {} for Create shader instancing compatibility; it is disabled",
                    oculusVersion, failure);
            return false;
        }
        if (!problems.isEmpty()) {
            VaultRenderOptimization.LOGGER.warn(
                    "Oculus {} does not provide what Create shader instancing compatibility needs; it is disabled. "
                            + "Create uses its standard renderer. First problems: {}",
                    oculusVersion, problems.subList(0, Math.min(5, problems.size())));
            return false;
        }

        if (loaded(modList, "irisflw")) {
            IrisFlwOverride.Result override = IrisFlwOverride.disableIrisFlwMixins();
            if (!override.success()) {
                VaultRenderOptimization.LOGGER.warn(
                        "Oculus Flywheel Compat (irisflw) is installed and could not be replaced ({}). VRO's Create "
                                + "shader instancing compatibility is disabled so the two do not both run. Remove "
                                + "oculus-flywheel-compat to use VRO's newer implementation.",
                        override.detail());
                return false;
            }
            VaultRenderOptimization.LOGGER.warn(
                    "Oculus Flywheel Compat (irisflw) is installed; VRO's newer Create shader instancing compatibility "
                            + "replaces it and its mixins were not applied ({}). The oculus-flywheel-compat jar can be removed.",
                    override.detail());
        }

        VaultRenderOptimization.LOGGER.info(
                "Loading Create shader instancing compatibility for Oculus {} and Flywheel {}",
                oculusVersion, flywheelVersion);
        return true;
    }

    private static List<byte[]> vroCompatClasses(LoadingModList modList) {
        var modFile = modList.getModFileById(VaultRenderOptimization.MOD_ID);
        if (modFile == null) {
            throw new IllegalStateException("VRO mod file not found");
        }
        Path root = modFile.getFile().findResource(COMPAT_PACKAGE);
        List<byte[]> classes = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : (Iterable<Path>) files.filter(path -> path.toString().endsWith(".class"))::iterator) {
                classes.add(Files.readAllBytes(file));
            }
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        if (classes.isEmpty()) {
            throw new IllegalStateException("no shader-compat classes found in the VRO jar");
        }
        return classes;
    }

    private static byte[] classBytes(LoadingModList modList, String modId, String internalName) {
        try {
            Path path = modList.getModFileById(modId).getFile().findResource(internalName + ".class");
            return Files.exists(path) ? Files.readAllBytes(path) : null;
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return decide();
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
