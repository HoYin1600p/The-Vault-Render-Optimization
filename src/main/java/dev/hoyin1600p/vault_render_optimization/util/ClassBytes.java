package dev.hoyin1600p.vault_render_optimization.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import net.minecraftforge.fml.loading.LoadingModList;

/** Reads class files of discovered mods before any of them is loaded. */
public final class ClassBytes {
    private ClassBytes() {
    }

    /** The bytes of {@code path} in the first mod file that has it, or {@code null} when no mod file does. */
    public static byte[] read(LoadingModList modList, String path) {
        try {
            var resource = modList.findResource(path);
            return resource == null ? null : Files.readAllBytes(resource);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
