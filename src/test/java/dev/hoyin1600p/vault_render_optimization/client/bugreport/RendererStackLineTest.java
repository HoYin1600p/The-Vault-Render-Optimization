package dev.hoyin1600p.vault_render_optimization.client.bugreport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class RendererStackLineTest {
    private static String line(Map<String, String> versions, Map<String, Object> files) {
        return RendererStackLine.build(id -> versions.getOrDefault(id, "not installed"), files::get);
    }

    @Test
    void embeddiumsRubidiumAliasIsNotReported() {
        assertEquals("Embeddium: 0.3.18; Rubidium: not installed", line(
                Map.of("embeddium", "0.3.18", "rubidium", "0.5.6"),
                Map.of("embeddium", "embeddium.jar", "rubidium", "embeddium.jar")));
    }

    @Test
    void aSeparateRubidiumJarIsReported() {
        assertEquals("Embeddium: 0.3.18; Rubidium: 0.5.6", line(
                Map.of("embeddium", "0.3.18", "rubidium", "0.5.6"),
                Map.of("embeddium", "embeddium.jar", "rubidium", "rubidium.jar")));
    }

    @Test
    void rubidiumAloneAndNeitherArePassedThrough() {
        assertEquals("Embeddium: not installed; Rubidium: 0.5.6",
                line(Map.of("rubidium", "0.5.6"), Map.of("rubidium", "rubidium.jar")));
        assertEquals("Embeddium: not installed; Rubidium: not installed", line(Map.of(), Map.of()));
    }
}
