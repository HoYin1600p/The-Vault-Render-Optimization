package dev.hoyin1600p.vault_render_optimization.renderertransfer;

import java.util.Set;

/**
 * Exact renderer builds VRO supports: the newest public Embeddium ({@code 0.3.18}) and Rubidium
 * ({@code 0.5.6}) for 1.18.2, plus HoYin1600p's post-removal Embeddium fork builds. Prefix matching
 * is deliberately not used, and a version string is never trusted on its own: chunk features are
 * also gated by {@code IndexSortCompatibility}/{@code AdaptiveBudgetCompatibility} class hashes and
 * renderer transfers by {@link RendererTransferBytecode}, because some local fork jars carry a
 * stock version string.
 */
public final class ValidatedRendererVersions {
    /** Post-removal fork (Embeddium {@code ced34c84}); the single-owner control ran on it. */
    public static final String EMBEDDIUM_CED34C84 = "0.3.18-git.ced34c84+mc1.18.2";
    /** Fork consolidation (Embeddium {@code d95f90d1}); not a public release. */
    public static final String EMBEDDIUM_FORK_0319 = "0.3.19+mc1.18.2";
    /** Exact inspected custom build; only ChunkBuilder's worker wake-up differs from the 0.3.19 fork. */
    public static final String EMBEDDIUM_7B0CF676 = "0.3.19-git.7b0cf676+mc1.18.2";

    private static final Set<String> EMBEDDIUM = Set.of(
            "0.3.18+mc1.18.2",
            EMBEDDIUM_CED34C84,
            EMBEDDIUM_FORK_0319,
            EMBEDDIUM_7B0CF676
    );
    private static final Set<String> RUBIDIUM = Set.of("0.5.6");

    private ValidatedRendererVersions() {
    }

    public static boolean supports(RendererFamily family, String version) {
        if (version == null) {
            return false;
        }
        return family == RendererFamily.EMBEDDIUM && EMBEDDIUM.contains(version)
                || family == RendererFamily.RUBIDIUM && RUBIDIUM.contains(version);
    }
}
