package dev.hoyin1600p.vault_render_optimization.config;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GpuOptInMigrationTest {
    @TempDir Path directory;
    private final List<String> warnings = new ArrayList<>();
    private final AtomicInteger resets = new AtomicInteger();

    private void apply() {
        GpuOptInMigration.apply(directory, resets::incrementAndGet, (message, failure) -> warnings.add(message));
    }

    @Test void absentMarkerResetsOnceAndLaterOptInIsUntouched() throws Exception {
        apply();
        assertEquals(1, resets.get());
        Path marker = directory.resolve(GpuOptInMigration.FILE_NAME);
        assertTrue(JsonParser.parseString(Files.readString(marker)).getAsJsonObject().get("gpuOptIn").getAsBoolean());
        String saved = Files.readString(marker);
        apply();
        assertEquals(1, resets.get());
        assertEquals(saved, Files.readString(marker));
        assertTrue(warnings.isEmpty());
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }

    @Test void completedMarkerDoesNotResetOrRewriteAnything() throws Exception {
        Path marker = directory.resolve(GpuOptInMigration.FILE_NAME);
        String saved = "{\"gpuOptIn\":true,\"otherMigration\":true}";
        Files.writeString(marker, saved);
        apply();
        assertEquals(0, resets.get());
        assertEquals(saved, Files.readString(marker));
        assertTrue(warnings.isEmpty());
    }

    @Test void incompleteMarkerPreservesOtherMigrations() throws Exception {
        Path marker = directory.resolve(GpuOptInMigration.FILE_NAME);
        Files.writeString(marker, "{\"gpuOptIn\":false,\"otherMigration\":true}");
        apply();
        assertEquals(1, resets.get());
        assertTrue(JsonParser.parseString(Files.readString(marker)).getAsJsonObject().get("otherMigration").getAsBoolean());
    }

    @Test void corruptMarkersWarnAndAreReplacedWithoutCrashing() throws Exception {
        Path marker = directory.resolve(GpuOptInMigration.FILE_NAME);
        for (String corrupt : List.of("{broken", "null", "[]", "{\"gpuOptIn\":\"true\"}")) {
            Files.writeString(marker, corrupt);
            int before = resets.get();
            int warningCount = warnings.size();
            assertDoesNotThrow(this::apply);
            assertEquals(before + 1, resets.get());
            assertEquals(warningCount + 1, warnings.size());
            assertTrue(JsonParser.parseString(Files.readString(marker)).getAsJsonObject().get("gpuOptIn").getAsBoolean());
        }
    }

    @Test void unreadableMarkerAndFailedWriteWarnWithoutCrashing() throws Exception {
        Files.createDirectory(directory.resolve(GpuOptInMigration.FILE_NAME));
        assertDoesNotThrow(this::apply);
        assertEquals(1, resets.get());
        assertTrue(warnings.size() >= 2);
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }

    @Test void failedConfigSaveDoesNotMarkMigrationComplete() {
        GpuOptInMigration.apply(directory, () -> { throw new IllegalStateException("save failed"); },
                (message, failure) -> warnings.add(message));
        assertFalse(Files.exists(directory.resolve(GpuOptInMigration.FILE_NAME)));
        assertEquals(1, warnings.size());
    }
}
