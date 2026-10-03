package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

/**
 * Added by VRO's mixins to GeckoLib cubes and Citadel model boxes: one field holding the GPU mesh cache entry,
 * so the per-frame lookup is a field read instead of a {@code WeakHashMap} lookup.
 */
public interface GpuMeshSlot {
    Object vro$gpuMesh();

    void vro$setGpuMesh(Object entry);
}
