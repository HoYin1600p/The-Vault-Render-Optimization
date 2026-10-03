package dev.hoyin1600p.vault_render_optimization.renderertransfer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Each transfer is owned by VRO only when the renderer class that used to hold the fork's own copy
 * is byte-identical to an inspected build without it. Pre-removal fork jars (some of which carry a
 * stock {@code 0.3.18+mc1.18.2} version string) keep their own implementation, so VRO yields there.
 *
 * <p>Accepted hashes: Embeddium {@code 0.3.18+mc1.18.2} (public), the HoYin1600p forks
 * {@code 0.3.18-git.ced34c84}, {@code 0.3.19} and {@code 0.3.19-git.7b0cf676} (identical for these
 * classes except SodiumWorldRenderer, whose fork build keeps its unrelated GL boundary reset), and
 * Rubidium {@code 0.5.6}.
 */
public final class RendererTransferBytecode {
    private static final String SODIUM = "me/jellysquid/mods/sodium/";

    private static final Map<RendererTransferFeature, Map<String, Set<String>>> CONTRACTS = Map.of(
            RendererTransferFeature.ADJACENT_BLOCK_OCCLUSION, Map.of(
                    SODIUM + "client/render/occlusion/BlockOcclusionCache.class", Set.of(
                            "4E1A7C42C4CED87A94547BC86D36AD571ABE7A24EA81ADEBCF66548AC3B4F114",
                            "98ADB4BDAE0F44D43F38036C09733D22C8804F80142F05F37B3558B6389AC832")),
            RendererTransferFeature.NULL_BUFFER_VERTEX_SINK, Map.of(
                    SODIUM + "mixin/core/pipeline/MixinBufferBuilder.class", Set.of(
                            "004AE5A22A4B0251C67BFED8ECADC0F6E0F6B5079C49093C25043E985349C7C5",
                            "6726B531ED8D09CC62BB67AAA8708C73D275F422AECF6FFE0E9BC39508291443")),
            RendererTransferFeature.DIRECT_CCL_RENDERER_LOOKUP, Map.of(
                    "org/embeddedt/embeddium/compat/ccl/CCLCompat.class", Set.of(
                            "30D8CF376DFA35C4B56D2257BFF242B7A03CF69ED91B353CD300130554648B81")),
            RendererTransferFeature.VERTEX_BUFFER_RETENTION, Map.of(
                    SODIUM + "client/model/vertex/buffer/VertexBufferBuilder.class", Set.of(
                            "A460FFD02623B144C9E21030EE545B7CFEBCF2323BFB144AEBC671C5CF1304DD",
                            "85FCAAF57A872AB44F5B94F3499CC7194F2159E9E7E3FE79E0055CDDCF601FEC")),
            RendererTransferFeature.ASYNC_ARENA_GROWTH, Map.of(
                    SODIUM + "client/gl/arena/AsyncBufferArena.class", Set.of(
                            "5D8C55D448FD22F6BB26A0410B747EEDCA3B6DB655BA9B9AFC136EA497C508F1",
                            "F9F71981F8E717A6FD80628F8459867E515A46325FF7BFEB53B305867163F693")),
            RendererTransferFeature.SMOOTH_FLUID_LIGHTING, Map.of(
                    SODIUM + "client/render/pipeline/FluidRenderer.class", Set.of(
                            "31D28580224C6428F4242B03E5DF904DB4982CEA54D9E094997D544E585FC788",
                            "B24C10FB861280B23BA0819F6A8329B1B2F8EF010D8EA8C6B761C060A79CB639")),
            RendererTransferFeature.CHUNK_LAYER_COLOR_RESET, Map.of(
                    SODIUM + "client/render/SodiumWorldRenderer.class", Set.of(
                            "A80FC4F3378A965F244B5C1113939A949AF111126E596CDDA928255538CDBB6E",
                            "285424F2AC5589D5C15A323A5971B2E01C3B8C3A6B08BD75D53645A9FA6D7828",
                            "BD434DE6228BB3721073BE6C422CF05B8A0B036B0AE334F9A6AAFD7BB8B11295")),
            RendererTransferFeature.CHUNK_REBUILD_DEDUPLICATION, Map.of(
                    SODIUM + "client/render/chunk/RenderSection.class", Set.of(
                            "6D1BBEB821001F5BA180FD4B887BF6A35CE17A0D84320AA31C4DE68BE483BD64",
                            "4BD6EE9446216B6EDB7C0F4F9AACCC498439B220A9D3E50B7B34D92A5A844655"),
                    SODIUM + "client/render/chunk/RenderSectionManager.class", Set.of(
                            "737488F7B04B126642F870483C53F1F1732E331F7533B52F2424925BE4C60C1C",
                            "A37CE91EBA59C8AC90B857A040DCAF4F182AFA629C920F3D1B1A438C20131C30",
                            "64BFEC67DE93B45EA4820F9D9581A233D77D6E5EC16D35D800973C6AFA9E1997"))
    );

    private RendererTransferBytecode() {
    }

    /** Null means every fingerprinted class matches; missing or changed bytes fail closed. */
    public static String blocker(RendererTransferFeature feature, Function<String, byte[]> resources) {
        Map<String, Set<String>> contract = CONTRACTS.get(feature);
        if (contract == null) {
            return "no bytecode contract for " + feature.id();
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (var entry : contract.entrySet()) {
                byte[] bytes = resources.apply(entry.getKey());
                String simple = entry.getKey().substring(entry.getKey().lastIndexOf('/') + 1);
                if (bytes == null) {
                    return "renderer class " + simple + " is missing";
                }
                String hash = HexFormat.of().withUpperCase().formatHex(digest.digest(bytes));
                if (!entry.getValue().contains(hash)) {
                    return "renderer class " + simple + " differs from the validated builds"
                            + " (the renderer may still contain its own implementation)";
                }
            }
        } catch (RuntimeException | NoSuchAlgorithmException failure) {
            return "renderer bytecode verification unavailable";
        }
        return null;
    }

    static Set<RendererTransferFeature> contractedFeatures() {
        return CONTRACTS.keySet();
    }
}
