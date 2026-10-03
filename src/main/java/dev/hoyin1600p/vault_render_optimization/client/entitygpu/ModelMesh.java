package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

/**
 * One {@code ModelPart}'s own cubes (not its children), captured once in model space. Per vertex:
 * {@code x/16, y/16, z/16, u, v, nx, ny, nz} as floats, where the normal is the polygon's normal.
 * Quads only, in vanilla's polygon and vertex order.
 */
public record ModelMesh(float[] data, int vertexCount) {
    public static final int FLOATS_PER_VERTEX = 8;
    public static final ModelMesh EMPTY = new ModelMesh(new float[0], 0);
}
