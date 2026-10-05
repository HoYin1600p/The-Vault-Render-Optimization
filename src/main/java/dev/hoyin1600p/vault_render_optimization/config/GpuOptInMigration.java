package dev.hoyin1600p.vault_render_optimization.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.BiConsumer;

/** Decision and IO isolated from Forge so the one-time reset can be tested on disk. */
public final class GpuOptInMigration {
    public static final String FILE_NAME = "vault_render_optimization-migrations.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private GpuOptInMigration() { }

    public static synchronized void apply(Path configDirectory, Runnable reset,
                                          BiConsumer<String, Exception> warn) {
        Path marker = configDirectory.resolve(FILE_NAME);
        JsonObject state = new JsonObject();
        try {
            state = GSON.fromJson(Files.readString(marker), JsonObject.class);
            if (state == null) throw new IllegalArgumentException("Null migration marker");
            if (state.has("gpuOptIn") && (!state.get("gpuOptIn").isJsonPrimitive()
                    || !state.getAsJsonPrimitive("gpuOptIn").isBoolean())) {
                throw new IllegalArgumentException("gpuOptIn must be a boolean");
            }
            if (state.has("gpuOptIn") && state.get("gpuOptIn").isJsonPrimitive()
                    && state.getAsJsonPrimitive("gpuOptIn").isBoolean()
                    && state.get("gpuOptIn").getAsBoolean()) return;
        } catch (NoSuchFileException absent) {
            // Fresh installs and older releases have no marker.
        } catch (IOException | RuntimeException failure) {
            warn.accept("Could not read GPU opt-in migration marker; resetting GPU rendering to off", failure);
            state = new JsonObject();
        }
        Path temporary = null;
        try {
            reset.run(); // Persist all switches before marking the migration complete.
            state.addProperty("gpuOptIn", true);
            Files.createDirectories(configDirectory);
            temporary = Files.createTempFile(configDirectory, FILE_NAME, ".tmp");
            Files.writeString(temporary, GSON.toJson(state));
            try {
                Files.move(temporary, marker, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, marker, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException failure) {
            warn.accept("Could not complete GPU opt-in migration; it will be retried next launch", failure);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException | RuntimeException failure) {
                    warn.accept("Could not remove temporary GPU opt-in migration marker", failure);
                }
            }
        }
    }
}
