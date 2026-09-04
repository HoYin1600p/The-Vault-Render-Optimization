package dev.hoyin1600p.vault_render_optimization.client.chunk.budget;

import dev.hoyin1600p.vault_render_optimization.client.chunk.sorting.IndexSortCompatibility;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.Function;

/** Additional contracts beyond the shared nine-class manager/builder/region/result gate. */
public final class AdaptiveBudgetCompatibility {
    private static final Map<String, String> HASHES = Map.of(
            "render/chunk/ChunkUpdateType", "2AE056F0BA14623D00050F75C1FD9058DB977660CC9695D2E1E49724BBC0CF4B",
            "render/chunk/tasks/ChunkRenderBuildTask", "FBEACF784A33B79F6AA8DAF8667F72374A1A52ED69B3E82E8E348501DC523703");
    private AdaptiveBudgetCompatibility() { }
    public static String blocker(String sharedBlocker, Function<String, byte[]> resources) {
        if (sharedBlocker != null) return sharedBlocker;
        try {
            for (var entry : HASHES.entrySet()) {
                byte[] bytes = resources.apply(IndexSortCompatibility.PREFIX + entry.getKey() + ".class");
                if (bytes == null) return "missing renderer class " + entry.getKey();
                String hash = HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
                if (!hash.equals(entry.getValue())) return "unvalidated renderer bytecode " + entry.getKey();
            }
        } catch (RuntimeException | NoSuchAlgorithmException failure) {
            return "renderer bytecode verification unavailable";
        }
        return null;
    }
}
