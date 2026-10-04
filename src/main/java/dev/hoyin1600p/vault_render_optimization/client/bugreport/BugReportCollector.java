package dev.hoyin1600p.vault_render_optimization.client.bugreport;

import com.mojang.blaze3d.platform.GlUtil;
import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuEntityModels;
import dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast.VroImmediatelyFast;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import dev.hoyin1600p.vault_render_optimization.config.ConfigSettingCatalog.Setting;
import dev.hoyin1600p.vault_render_optimization.config.ConfigSettingCatalog.Storage;
import dev.hoyin1600p.vault_render_optimization.config.ConfigSettingStore;
import dev.hoyin1600p.vault_render_optimization.util.ModIds;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.versions.forge.ForgeVersion;

/** Gathers what a bug report needs on the render thread and scrubs every piece of it. */
public final class BugReportCollector {
    /** A crash report found on disk, already scrubbed. */
    public record CrashFile(String fileName, String text, String exceptionLine) {
    }

    public record Collected(GitHubIssue.Report report, Optional<CrashFile> crash) {
    }

    private BugReportCollector() {
    }

    public static Collected collect() {
        Minecraft minecraft = Minecraft.getInstance();
        User user = minecraft.getUser();
        BugReportScrubber scrubber = new BugReportScrubber(
                user == null ? null : user.getName(),
                user == null ? null : user.getUuid(),
                System.getProperty("user.name"));

        List<String> versions = List.of(
                "VRO: " + modVersion(VaultRenderOptimization.MOD_ID),
                "Minecraft: " + SharedConstants.getCurrentVersion().getName(),
                "Forge: " + ForgeVersion.getVersion(),
                "Java: " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")",
                "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version")
                        + " (" + System.getProperty("os.arch") + ")"
        );
        List<String> stack = List.of(
                "GPU: " + GlUtil.getRenderer() + " (" + GlUtil.getVendor() + ")",
                "OpenGL and driver: " + GlUtil.getOpenGLVersion(),
                RendererStackLine.build(BugReportCollector::modVersion, BugReportCollector::modFile),
                "Oculus: " + modVersion(ModIds.OCULUS) + oculusShaderPack(),
                "Flywheel: " + modVersion(ModIds.FLYWHEEL),
                "ImmediatelyFast: " + immediatelyFast()
        );
        List<String> state = List.of(
                "Compare Mode: " + (ClientOptimizationConfig.compareModeEnabled() ? "ON" : "off")
        );
        List<String> gpu = Arrays.asList(GpuEntityModels.status().split("\\R"));

        GitHubIssue.Report report = new GitHubIssue.Report(
                "Bug: ",
                scrub(scrubber, versions),
                scrub(scrubber, stack),
                scrub(scrubber, state),
                scrub(scrubber, nonDefaultSettings()),
                scrub(scrubber, gpu)
        );
        return new Collected(report, crash(minecraft.gameDirectory.toPath().resolve("crash-reports"), scrubber));
    }

    private static Optional<CrashFile> crash(Path directory, BugReportScrubber scrubber) {
        return CrashReports.newest(directory).flatMap(file -> {
            try {
                String text = scrubber.scrub(CrashReports.read(file).replace("\t", "    "));
                return Optional.of(new CrashFile(file.getFileName().toString(), text, CrashReports.exceptionLine(text)));
            } catch (IOException failure) {
                VaultRenderOptimization.LOGGER.warn("Could not read crash report {}", file.getFileName(), failure);
                return Optional.empty();
            }
        });
    }

    private static List<String> nonDefaultSettings() {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<Setting, Object> entry : ConfigSettingStore.currentValues().entrySet()) {
            Setting setting = entry.getKey();
            if (!Objects.equals(entry.getValue(), ConfigSettingStore.defaultValue(setting))) {
                String where = setting.storage() == Storage.IMMEDIATELY_FAST_JSON
                        ? "immediatelyfast." + setting.path().get(0)
                        : String.join(".", setting.path());
                lines.add(where + " = " + entry.getValue());
            }
        }
        return lines;
    }

    private static String immediatelyFast() {
        String standalone = modVersion(VroImmediatelyFast.STANDALONE_MOD_ID);
        if (!standalone.equals("not installed")) {
            return "standalone " + standalone;
        }
        return "built into VRO, " + VroImmediatelyFast.status();
    }

    private static String oculusShaderPack() {
        if (!ModList.get().isLoaded(ModIds.OCULUS)) {
            return "";
        }
        try {
            Class<?> iris = Class.forName("net.coderbot.iris.Iris");
            Object config = iris.getMethod("getIrisConfig").invoke(null);
            boolean enabled = (Boolean) config.getClass().getMethod("areShadersEnabled").invoke(config);
            Optional<?> name = (Optional<?>) config.getClass().getMethod("getShaderPackName").invoke(config);
            return "; shader pack: " + (enabled ? name.map(Object::toString).orElse("none") : "off");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            return "; shader pack: unknown";
        }
    }

    private static String modVersion(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse(RendererStackLine.NOT_INSTALLED);
    }

    /** The file providing a mod ID, so IDs that one jar registers twice can be told apart from two mods. */
    private static Object modFile(String modId) {
        IModFileInfo info = ModList.get().getModFileById(modId);
        return info == null ? null : info.getFile().getFilePath();
    }

    private static List<String> scrub(BugReportScrubber scrubber, List<String> lines) {
        return lines.stream().map(scrubber::scrub).toList();
    }
}
