package dev.hoyin1600p.vault_render_optimization.client.update;

import dev.hoyin1600p.vault_render_optimization.config.model.UpdateNotice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class VroUpdateManifestTest {
    private static final String DOWNLOAD_URL =
            "https://www.curseforge.com/minecraft/mc-mods/vault-render-optimization";

    @Test
    void latestPublicReleaseIsNotAdvertisedAsOutdated() throws IOException {
        assertTrue(parseFor(latestVersion()).isEmpty());
    }

    @Test
    void olderReleaseReceivesTheVroManifestNotice() throws IOException {
        UpdateNotice notice = parseFor("0.4.0").orElseThrow();

        assertEquals("vault_render_optimization", notice.modId());
        assertEquals("VRO", notice.displayName());
        assertEquals(latestVersion(), notice.targetVersion());
        // A release marked critical carries the prefix in update.json; the parser strips it.
        String raw = com.google.gson.JsonParser.parseString(Files.readString(Path.of("update.json")))
                .getAsJsonObject().getAsJsonObject("1.18.2").get(latestVersion()).getAsString();
        boolean critical = raw.regionMatches(true, 0, "[CRITICAL]", 0, "[CRITICAL]".length());
        assertEquals(critical ? UpdateNotice.Severity.CRITICAL : UpdateNotice.Severity.NORMAL, notice.severity());
        assertEquals(critical ? raw.substring("[CRITICAL]".length()).trim() : raw, notice.message());
        assertEquals(DOWNLOAD_URL, notice.downloadUrl());
    }

    private static Optional<UpdateNotice> parseFor(String currentVersion)
            throws IOException {
        String manifest = Files.readString(
                Path.of("update.json"),
                StandardCharsets.UTF_8
        );
        return UpdateManifestParser.parse(
                manifest,
                "vault_render_optimization",
                "VRO",
                currentVersion,
                "1.18.2",
                DOWNLOAD_URL
        );
    }

    private static String latestVersion() throws IOException {
        return com.google.gson.JsonParser.parseString(Files.readString(Path.of("update.json")))
                .getAsJsonObject().getAsJsonObject("promos").get("1.18.2-latest").getAsString();
    }
}
