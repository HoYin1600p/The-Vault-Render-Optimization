package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

/** Added to {@code BufferBuilder} by VRO's GPU entity mixin. */
public interface GpuHoleBuilder {
    /**
     * True when the builder is building a plain {@code NEW_ENTITY} quad buffer between vertices (or Oculus'
     * extended entity format between quads, while its GPU program is usable), with a little-endian buffer,
     * no fixed colour and no sorting in progress.
     */
    boolean vro$canReserve();

    /**
     * {@link #vro$canReserve()} without the sorting hint: item runs may be reserved into sorted buffers, since
     * only their sort positions are needed on the CPU ({@code HoleBatch.writeSortPositions}).
     */
    boolean vro$canReserveItem();

    /** Records whether the batch this builder just handed off was too small for a dispatch. */
    void vro$noteBatch(boolean small);

    /**
     * Set by the buffer source that hands this builder out: true when its render type sorts quads on
     * upload, so reserved vertices would only be filled on the CPU again before sorting.
     */
    void vro$setSortingHint(boolean sorting);

    /**
     * Advances the builder past {@code mesh}'s vertices without writing them and records how to write
     * them later. Only valid right after {@link #vro$canReserve()} returned true.
     */
    void vro$reserve(ModelMesh mesh, int meshFirst, float[] pose, float[] normal, int color, int overlay, int light,
                     float[] sprite, int flips);

    /** The builder's current vertex format (Oculus may have switched {@code NEW_ENTITY} to its own). */
    com.mojang.blaze3d.vertex.VertexFormat vro$format();

    /**
     * Reserves {@code vertexCount} vertices of an item mesh (starting at {@code firstVertex} of the mesh) with
     * one tint word. Only valid right after {@link #vro$canReserve()} returned true.
     *
     * @return false when nothing was reserved (the current batch holds another kind of holes)
     */
    boolean vro$reserveItem(ItemMesh mesh, int meshFirst, int firstVertex, int vertexCount, float[] pose,
                            float[] normal, int tint, int overlay, int light, boolean embeddium);

    /** True when the builder is building a plain {@code PARTICLE} quad buffer between vertices. */
    boolean vro$canReserveParticle();

    /**
     * Advances the builder past one particle's four vertices and records how to write them later.
     *
     * @return false when nothing was reserved (the caller writes the particle as usual)
     */
    boolean vro$reserveParticle(float x, float y, float z, float size, boolean rolled, float sin, float cos,
                                float minU, float maxU, float minV, float maxV, int color, int light,
                                float leftX, float leftY, float leftZ, float upX, float upY, float upZ);
}
