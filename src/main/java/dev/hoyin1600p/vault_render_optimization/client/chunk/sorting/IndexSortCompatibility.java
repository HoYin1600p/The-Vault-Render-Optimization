package dev.hoyin1600p.vault_render_optimization.client.chunk.sorting;

import dev.hoyin1600p.vault_render_optimization.client.chunk.ChunkUpdateBackend;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Exact inspected bytecode, not just a version string. No optional classes are loaded. */
public final class IndexSortCompatibility {
    public static final String PREFIX = "me/jellysquid/mods/sodium/client/";
    private static final Map<String, Set<String>> HASHES = Map.ofEntries(
            Map.entry("render/chunk/RenderSectionManager", Set.of(
                    "737488F7B04B126642F870483C53F1F1732E331F7533B52F2424925BE4C60C1C",
                    "A37CE91EBA59C8AC90B857A040DCAF4F182AFA629C920F3D1B1A438C20131C30")),
            Map.entry("render/chunk/RenderSection", Set.of("6D1BBEB821001F5BA180FD4B887BF6A35CE17A0D84320AA31C4DE68BE483BD64")),
            Map.entry("render/chunk/region/RenderRegionManager", Set.of("BE16A18274525636D48D4F5BAEEB6103A7472BF12D34C8B92152E4FE95A22AB0")),
            Map.entry("render/chunk/ChunkGraphicsState", Set.of("2223B538999B19418F80E80ECB0F642B3A15009C836B2EAEA1E3AA2CBA08895F")),
            Map.entry("render/chunk/compile/ChunkBufferSorter", Set.of("290922AB29DAF3502FC982BDF780196DE016315646FF6C11EBB53144E98B2A4E")),
            Map.entry("render/chunk/compile/ChunkBufferSorter$SortBuffer", Set.of("8D2A35DA6B99B741AB10FF9C02FF8DE8B36C030910E358930D3E1A0569B72C27")),
            Map.entry("render/chunk/compile/ChunkBuildResult", Set.of("99834D162F87542A57AD25FECF4886EA63F48E50CFDE1E74D0FC1D9F752A85BB")),
            Map.entry("render/chunk/compile/ChunkBuilder", Set.of("78D5245193022E2E4B722DFD49E1AB6523C3D8F1FE7D7D4E43960A39C96C4540")),
            Map.entry("render/chunk/data/ChunkMeshData", Set.of("8F5559B51505027B4703C17FB0DC327F0C83A38850243E71CEB1C6D76B791038"))
    );

    private IndexSortCompatibility() { }

    /** Null means compatible; any missing/changed resource fails closed. */
    public static String blocker(ChunkUpdateBackend backend, Function<String, byte[]> resources) {
        if (backend != ChunkUpdateBackend.EMBEDDIUM) return "requires validated Embeddium 0.3.18/0.3.19";
        try {
            for (var entry : HASHES.entrySet()) {
                byte[] bytes = resources.apply(PREFIX + entry.getKey() + ".class");
                if (bytes == null) return "missing renderer class " + entry.getKey();
                String sha = HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
                if (!entry.getValue().contains(sha)) return "unvalidated renderer bytecode " + entry.getKey();
            }
        } catch (RuntimeException | NoSuchAlgorithmException failure) {
            return "renderer bytecode verification unavailable";
        }
        return null;
    }
}
