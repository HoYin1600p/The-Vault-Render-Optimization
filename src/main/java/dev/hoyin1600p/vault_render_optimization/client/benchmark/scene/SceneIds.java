package dev.hoyin1600p.vault_render_optimization.client.benchmark.scene;

/** Reserved client ids. Keep the allocator for the lifetime of the client, including between runs. */
public final class SceneIds {
    public static final int FIRST = -1_000_000;
    private long next = FIRST;

    public int next() {
        if (next < Integer.MIN_VALUE) throw new IllegalStateException("Virtual scene ids exhausted");
        return (int) next--;
    }
}
