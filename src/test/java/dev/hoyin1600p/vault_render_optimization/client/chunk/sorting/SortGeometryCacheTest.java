package dev.hoyin1600p.vault_render_optimization.client.chunk.sorting;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Map;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBufferSorter.SortBuffer;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkModelVertexFormats;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import net.minecraftforge.fml.ModList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SortGeometryCacheTest {
    @Test void tiesAreStableAndGenerationIdentityIsNotRecordEquality() {
        try (var mods = mockStatic(ModList.class)) {
            mods.when(ModList::get).thenReturn(mock(ModList.class));
            SortGeometryCache.clear();
            var source = mesh(2);
            assertArrayEquals(source.indexBuffer().array(), SortGeometryCache.sort(source, 0, 0, 0).array());
            assertEquals(48, SortGeometryCache.retainedBytes());
            var sameFieldsDifferentGeneration = new SortBuffer(source.vertexBuffer(), source.indexBuffer(), source.vertexFormat(), source.parts());
            assertEquals(source, sameFieldsDifferentGeneration);
            SortGeometryCache.sort(sameFieldsDifferentGeneration, 0, 0, 0);
            assertEquals(96, SortGeometryCache.retainedBytes());
            assertNull(SortGeometryCache.sort(source, Float.NaN, 0, 0));
            SortGeometryCache.clear();
            assertEquals(0, SortGeometryCache.retainedBytes());
        }
    }

    @Test void retainedGeometryIsGloballyBoundedAndDoesNotRetainNativeBuffers() {
        try (var mods = mockStatic(ModList.class)) {
            mods.when(ModList::get).thenReturn(mock(ModList.class));
            SortGeometryCache.clear();
            var alive = new ArrayList<SortBuffer>();
            for (int i = 0; i < 100; i++) {
                var source = mesh(8192);
                alive.add(source);
                assertNotNull(SortGeometryCache.sort(source, i, 0, 0));
                assertTrue(SortGeometryCache.retainedBytes() <= 16 * 1024 * 1024);
            }
            assertNull(SortGeometryCache.sort(mesh(50_000), 0, 0, 0));
            var direct = new SortBuffer(ByteBuffer.allocateDirect(12), ByteBuffer.allocateDirect(12), alive.get(0).vertexFormat(), Map.of());
            assertNull(SortGeometryCache.sort(direct, 0, 0, 0));
            SortGeometryCache.clear();
        }
    }

    private static SortBuffer mesh(int triangles) {
        var format = (GlVertexFormat<?>) ChunkModelVertexFormats.VANILLA_LIKE.getBufferVertexFormat();
        var vertices = ByteBuffer.allocate(3 * format.getStride()).order(ByteOrder.nativeOrder());
        var indices = ByteBuffer.allocate(triangles * 12).order(ByteOrder.nativeOrder());
        for (int i = 0; i < triangles; i++) indices.putInt(0).putInt(1).putInt(2);
        indices.clear();
        return new SortBuffer(vertices, indices, format, Map.of());
    }
}
