/* SPDX-License-Identifier: LGPL-3.0-only
 * Triangle-center rule adapted from Embeddium 0.3.18 ChunkBufferSorter.
 * VRO-owned bounded weak-identity cache and output construction.
 * See THIRD_PARTY_NOTICES.md. No newer Sodium source is used.
 */
package dev.hoyin1600p.vault_render_optimization.client.chunk.sorting;

import it.unimi.dsi.fastutil.ints.IntArrays;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBufferSorter.SortBuffer;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.format.VanillaLikeChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.format.sfp.ModelVertexType;

/** Workers share immutable decoded geometry, never world/section references or mutable cursors. */
public final class SortGeometryCache {
    private static final int MAX_BYTES = 16 * 1024 * 1024, MAX_ENTRY_BYTES = 1024 * 1024;
    private static final int MAX_ENTRIES = 256;
    private static final ReferenceQueue<SortBuffer> COLLECTED = new ReferenceQueue<>();
    private static final LinkedHashMap<Key, Geometry> CACHE = new LinkedHashMap<>(16, .75f, true);
    private static int retainedBytes;
    private record Geometry(float[] centers, int[] indices) {
        int weight() { return centers.length * 4 + indices.length * 4; }
    }
    private static final class Key extends WeakReference<SortBuffer> {
        private final int hash;
        Key(SortBuffer source, boolean retained) {
            super(source, retained ? COLLECTED : null);
            hash = System.identityHashCode(source);
        }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object other) {
            return this == other || other instanceof Key key && get() != null && get() == key.get();
        }
    }
    private SortGeometryCache() { }

    public static ByteBuffer sort(SortBuffer source, float x, float y, float z) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) return null;
        Geometry geometry;
        synchronized (CACHE) {
            reap();
            geometry = CACHE.get(new Key(source, false));
        }
        if (geometry == null) {
            geometry = decode(source);
            if (geometry == null) return null;
            synchronized (CACHE) {
                reap();
                Geometry raced = CACHE.get(new Key(source, false));
                if (raced != null) geometry = raced;
                else {
                    while (!CACHE.isEmpty() && (CACHE.size() >= MAX_ENTRIES
                            || retainedBytes + geometry.weight() > MAX_BYTES)) {
                        var entries = CACHE.entrySet().iterator();
                        retainedBytes -= entries.next().getValue().weight();
                        entries.remove();
                    }
                    CACHE.put(new Key(source, true), geometry);
                    retainedBytes += geometry.weight();
                }
            }
            IndexSortState.geometryDecoded.increment();
        } else IndexSortState.geometryReused.increment();

        int triangles = geometry.indices.length / 3;
        float[] distances = new float[triangles];
        int[] order = new int[triangles];
        boolean sorted = true;
        for (int i = 0; i < triangles; i++) {
            int k = i * 3;
            float dx = geometry.centers[k] - x, dy = geometry.centers[k+1] - y, dz = geometry.centers[k+2] - z;
            distances[i] = dx*dx + dy*dy + dz*dz;
            order[i] = i;
            if (i > 0 && Float.compare(distances[i-1], distances[i]) < 0) sorted = false;
        }
        // Stable order remains the native triangle order for exact ties.
        if (!sorted) IntArrays.mergeSort(order, (a, b) -> Float.compare(distances[b], distances[a]));
        else IndexSortState.alreadyOrdered.increment();
        ByteBuffer output = ByteBuffer.allocate(source.indexBuffer().capacity()).order(source.indexBuffer().order());
        for (int triangle : order) {
            int k = triangle * 3;
            output.putInt(geometry.indices[k]).putInt(geometry.indices[k+1]).putInt(geometry.indices[k+2]);
        }
        output.clear();
        return output;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Geometry decode(SortBuffer source) {
        int bytes = source.indexBuffer().capacity();
        if (bytes == 0 || bytes % 12 != 0 || 2L * bytes > MAX_ENTRY_BYTES
                || source.indexBuffer().isDirect() || source.vertexBuffer().isDirect()) return null;
        boolean compact;
        me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat format = source.vertexFormat();
        try { format.getAttribute(VanillaLikeChunkMeshAttribute.POSITION); compact = false; }
        catch (NullPointerException absent) {
            try { format.getAttribute(ChunkMeshAttribute.POSITION_ID); compact = true; }
            catch (NullPointerException unknown) { return null; }
        }
        int stride = source.vertexFormat().getStride();
        int positionSize = compact ? 6 : 12;
        if (stride < positionSize) return null;
        ByteBuffer vertices = source.vertexBuffer().duplicate().order(source.vertexBuffer().order());
        ByteBuffer input = source.indexBuffer().duplicate().order(source.indexBuffer().order());
        vertices.clear(); input.clear();
        int[] indices = new int[bytes / 4];
        float[] centers = new float[indices.length];
        float[] triangle = new float[9];
        for (int i = 0; i < indices.length; i += 3) {
            for (int v = 0; v < 3; v++) {
                int index = input.getInt();
                long base = (long) index * stride;
                if (index < 0 || base + positionSize > vertices.capacity()) return null;
                indices[i + v] = index;
                for (int axis = 0; axis < 3; axis++) {
                    int address = (int) base + axis * (compact ? 2 : 4);
                    float value = compact ? ModelVertexType.decodePosition(vertices.getShort(address)) : vertices.getFloat(address);
                    if (!Float.isFinite(value)) return null;
                    triangle[v * 3 + axis] = value;
                }
            }
            float ab = distance(triangle, 0, 3), bc = distance(triangle, 3, 6), ca = distance(triangle, 6, 0);
            int first, second;
            if (ab > bc && ab > ca) { first = 0; second = 3; }
            else if (ab <= bc && bc > ca) { first = 3; second = 6; }
            else { first = 6; second = 0; }
            for (int axis = 0; axis < 3; axis++) centers[i + axis] = (triangle[first+axis] + triangle[second+axis]) / 2;
        }
        return new Geometry(centers, indices);
    }

    private static float distance(float[] p, int a, int b) {
        float x = p[b] - p[a], y = p[b+1] - p[a+1], z = p[b+2] - p[a+2];
        return x*x + y*y + z*z;
    }
    private static void reap() {
        Key key;
        while ((key = (Key) COLLECTED.poll()) != null) {
            Geometry removed = CACHE.remove(key);
            if (removed != null) retainedBytes -= removed.weight();
        }
    }
    public static void clear() {
        synchronized (CACHE) { CACHE.clear(); retainedBytes = 0; while (COLLECTED.poll() != null) { } }
    }
    static int retainedBytes() { synchronized (CACHE) { reap(); return retainedBytes; } }
}
