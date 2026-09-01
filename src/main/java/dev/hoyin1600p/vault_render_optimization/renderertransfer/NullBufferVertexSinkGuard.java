package dev.hoyin1600p.vault_render_optimization.renderertransfer;

/** Keeps the null check testable without loading renderer internals. */
public final class NullBufferVertexSinkGuard {
    private NullBufferVertexSinkGuard() {
    }

    public static boolean requiresFallback(Object backingBuffer) {
        return backingBuffer == null;
    }
}
