package dev.hoyin1600p.vault_render_optimization.client.update;

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
    void currentReleaseIsNotAdvertisedAsOutdated() throws IOException {
        assertTrue(parseFor("0.4.0").isEmpty());
    }

    @Test
    void olderReleaseReceivesTheVroManifestNotice() throws IOException {
        UpdateNotice notice = parseFor("0.3.5").orElseThrow();

        assertEquals("vault_render_optimization", notice.modId());
        assertEquals("VRO", notice.displayName());
        assertEquals("0.4.0", notice.targetVersion());
        assertEquals(UpdateNotice.Severity.NORMAL, notice.severity());
        assertEquals("Configurable Update Notices", notice.message());
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
}
