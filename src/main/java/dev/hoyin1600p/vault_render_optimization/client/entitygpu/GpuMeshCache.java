package dev.hoyin1600p.vault_render_optimization.client.entitygpu;


import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import static dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuEntityModels.*;

/** Captured model meshes and their arena locations: capture, upload and reuse of equal geometry. */
final class GpuMeshCache {
    private GpuMeshCache() {
    }

    /** Returns the part's mesh for this generation, capturing and uploading it on first use. */
    public static CachedMesh mesh(ModelPart part, CachedMesh cached) {
        if (cached != null && cached.generation() == generation && cached.cubes() == part.cubes
                && cached.cubeCount() == part.cubes.size()) {
            return cached;
        }
        ModelMesh mesh;
        int first;
        try {
            mesh = ModelMeshCapture.capture(part);
            first = mesh == null ? -1 : uploadMesh(mesh);
        } catch (RuntimeException | OutOfMemoryError failure) {
            // This part stays on the CPU for this generation (the caller caches the result); rendering goes on.
            if (!meshFailureLogged) {
                meshFailureLogged = true;
                VaultRenderOptimization.LOGGER.warn("GPU entity models: a model part could not be captured; it stays on the CPU", failure);
            }
            mesh = null;
            first = -1;
        }
        return new CachedMesh(generation, part.cubes, part.cubes.size(), mesh == null ? ModelMesh.EMPTY : mesh, first);
    }

    static boolean meshFailureLogged;

    /** Arena contents by geometry (float or int words compared exactly), for reuse across equal meshes. */
    static final Map<MeshKey, Integer> UPLOADED = new HashMap<>();
    static final Map<MeshKey, Integer> UPLOADED_ITEMS = new HashMap<>();

    /** Exact content key: equal only when every word's bits are equal. */
    static final class MeshKey {
        private final int[] bits;
        private final int hash;

        MeshKey(float[] data, int length) {
            bits = new int[length];
            for (int i = 0; i < length; i++) bits[i] = Float.floatToRawIntBits(data[i]);
            hash = Arrays.hashCode(bits);
        }

        MeshKey(int[] data, int length) {
            bits = Arrays.copyOf(data, length);
            hash = Arrays.hashCode(bits);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof MeshKey key && key.hash == hash && Arrays.equals(key.bits, bits);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    /**
     * Uploads a captured mesh to the arena (only while the path is active on the render thread).
     *
     * @return its first arena vertex, 0 for an empty mesh, or -1 when the arena is full
     */
    public static int uploadMesh(ModelMesh mesh) {
        if (mesh.vertexCount() == 0) return 0;
        // Identical geometry reuses its arena range: renderers that build a new ModelPart every frame (or many
        // parts with the same cubes) would otherwise grow the arena without bound.
        MeshKey key = new MeshKey(mesh.data(), mesh.vertexCount() * ModelMesh.FLOATS_PER_VERTEX);
        Integer known = UPLOADED.get(key);
        if (known != null) {
            MESHES_SHARED.incrementAndGet();
            return known;
        }
        int first = backend.upload(mesh);
        if (first >= 0) UPLOADED.put(key, first);
        if (first < 0 && !arenaFullLogged) {
            arenaFullLogged = true;
            VaultRenderOptimization.LOGGER.warn("GPU entity models: mesh arena is full ({} vertices); "
                    + "further new models use the CPU path until the next resource reload", backend.arenaVertices());
        }
        return first;
    }

    /**
     * Uploads a captured item mesh to the item arena.
     *
     * @return its first arena vertex, 0 for an empty mesh, or -1 when the arena is full
     */
    public static int uploadItemMesh(ItemMesh mesh) {
        if (mesh.vertexCount() == 0) return 0;
        MeshKey key = new MeshKey(mesh.words(), mesh.vertexCount() * ItemMesh.WORDS_PER_VERTEX);
        Integer known = UPLOADED_ITEMS.get(key);
        if (known != null) {
            MESHES_SHARED.incrementAndGet();
            return known;
        }
        int first = backend.uploadItem(mesh);
        if (first >= 0) UPLOADED_ITEMS.put(key, first);
        if (first < 0 && !arenaFullLogged) {
            arenaFullLogged = true;
            VaultRenderOptimization.LOGGER.warn("GPU entity models: item arena is full; further new item models use the CPU path "
                    + "until the next resource reload");
        }
        return first;
    }
}
