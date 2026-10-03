package dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast;

import static org.junit.jupiter.api.Assertions.*;

import dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast.compat.IrisCompat;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

class IrisCompatPreflightTest {
    private static List<String> oculusJars() {
        return Arrays.stream(System.getProperty("vro.oculus.contractJars", "").split(File.pathSeparator))
                .filter(path -> !path.isBlank()).toList();
    }

    private static Function<String, byte[]> jar(String path) {
        return name -> {
            try (var zip = new ZipFile(path)) {
                var entry = zip.getEntry(name + ".class");
                if (entry == null) return null;
                try (var stream = zip.getInputStream(entry)) { return stream.readAllBytes(); }
            } catch (IOException failure) { throw new UncheckedIOException(failure); }
        };
    }

    @Test
    void everyDiscoveredOculusBuildProvidesTheMembersImmediatelyFastUses() {
        assertFalse(oculusJars().isEmpty());
        for (String path : oculusJars()) {
            assertNull(IrisCompat.preflight(jar(path)), path);
        }
    }

    @Test
    void missingOculusClassesFailClosed() {
        assertNotNull(IrisCompat.preflight(name -> null));
        var stock = jar(oculusJars().get(0));
        assertNotNull(IrisCompat.preflight(name -> name.endsWith("ExtendingBufferBuilder") ? null : stock.apply(name)));
    }
}
