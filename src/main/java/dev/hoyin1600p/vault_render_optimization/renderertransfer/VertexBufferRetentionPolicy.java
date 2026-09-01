package dev.hoyin1600p.vault_render_optimization.renderertransfer;

public final class VertexBufferRetentionPolicy {
    private VertexBufferRetentionPolicy() {
    }

    public static int nextCapacity(int currentCapacity, int writerOffset, int requestedLength) {
        long required = (long) writerOffset + requestedLength;
        long doubled = (long) currentCapacity * 2L;
        long next = Math.max(required, doubled);
        if (next > Integer.MAX_VALUE) {
            if (required > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Vertex buffer request exceeds the 32-bit renderer limit");
            }
            next = Integer.MAX_VALUE;
        }
        return (int) next;
    }

    public static int retainedCapacityLimitBytes(int configuredMib, int initialCapacity) {
        long bytes = Math.max(1, configuredMib) * 1024L * 1024L;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(bytes, initialCapacity));
    }
}
