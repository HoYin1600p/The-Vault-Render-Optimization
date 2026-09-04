package dev.hoyin1600p.vault_render_optimization.client.chunk.sorting;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Transfer only index ownership. A failed installation keeps the old state intact. */
public final class IndexSegmentCommit {
    private IndexSegmentCommit() { }

    public static <S> void replace(S vertices, S oldIndices, Supplier<S> upload,
            BiConsumer<S, S> install, Consumer<S> freeIndex) {
        S newIndices = upload.get();
        boolean installed = false;
        try {
            install.accept(vertices, newIndices);
            installed = true;
        } finally {
            if (!installed) {
                freeIndex.accept(newIndices);
            }
        }
        freeIndex.accept(oldIndices);
    }
}
