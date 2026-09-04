package dev.hoyin1600p.vault_render_optimization.client.chunk.sorting;

import java.nio.ByteBuffer;

/** Per-job cursors; immutable heap vertex storage may be shared, indices never are. */
public final class SortBufferViews {
    private SortBufferViews() { }

    public static ByteBuffer vertices(ByteBuffer source) {
        if (source.isDirect()) {
            throw new IllegalArgumentException("Only renderer-owned heap snapshots may be shared");
        }
        ByteBuffer view = source.asReadOnlyBuffer().order(source.order());
        view.clear();
        return view;
    }

    public static ByteBuffer indices(ByteBuffer source) {
        ByteBuffer input = source.duplicate().order(source.order());
        input.clear();
        ByteBuffer copy = ByteBuffer.allocate(input.remaining()).order(source.order());
        copy.put(input).flip();
        return copy;
    }

    public static boolean sameGeneration(boolean accepted, Object expected, Object current) {
        return accepted && expected != null && expected == current;
    }
}
