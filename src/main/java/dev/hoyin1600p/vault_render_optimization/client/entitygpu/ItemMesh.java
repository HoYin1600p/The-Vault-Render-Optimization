package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

/**
 * Immutable per-vertex quad data of one item model face list, captured once.
 *
 * <p>{@link #WORDS_PER_VERTEX} words per vertex: 0-2 position float bits, 3-4 u/v float bits, 5-7 the normal
 * input floats (writer specific, see {@link ItemMeshCapture}), 8 the baked color int and 9 the baked light int,
 * both as stored in the quad. {@code vertexCount} is a multiple of four; {@code quadTint[q]} is quad q's tint
 * index ({@code -1} untinted).
 */
public record ItemMesh(int[] words, int vertexCount, int[] quadTint) {
    public static final int WORDS_PER_VERTEX = 10;
    public static final ItemMesh EMPTY = new ItemMesh(new int[0], 0, new int[0]);
}
