package dev.hoyin1600p.vault_render_optimization.client.bugreport;

import java.util.Objects;
import java.util.function.Function;

/**
 * Builds the "Embeddium / Rubidium" line of the bug report. Embeddium also registers a
 * {@code rubidium} compatibility mod ID from its own jar, so that ID only counts as Rubidium when a
 * different file provides it.
 */
final class RendererStackLine {
    static final String NOT_INSTALLED = "not installed";

    private RendererStackLine() {
    }

    /**
     * @param versionOf version of a mod ID, or {@link #NOT_INSTALLED}
     * @param fileOf    identity of the file providing a mod ID (a path, for example), or null
     */
    static String build(Function<String, String> versionOf, Function<String, Object> fileOf) {
        String embeddium = versionOf.apply("embeddium");
        String rubidium = versionOf.apply("rubidium");
        if (!NOT_INSTALLED.equals(embeddium) && !NOT_INSTALLED.equals(rubidium)) {
            Object embeddiumFile = fileOf.apply("embeddium");
            if (embeddiumFile != null && Objects.equals(embeddiumFile, fileOf.apply("rubidium"))) {
                rubidium = NOT_INSTALLED;
            }
        }
        return "Embeddium: " + embeddium + "; Rubidium: " + rubidium;
    }
}
