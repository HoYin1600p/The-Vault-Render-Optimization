package dev.hoyin1600p.vault_render_optimization.client.chunk.sorting;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;
import java.util.Map;
import java.util.Random;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBufferSorter;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBufferSorter.SortBuffer;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkModelVertexFormats;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import net.minecraftforge.fml.ModList;

class SortBufferViewsTest {
    @Test
    void viewsHaveIndependentCursorsAndPrivateMutableIndices() {
        ByteBuffer source = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
        source.putInt(0, 42).position(8).limit(16);
        ByteBuffer vertices = SortBufferViews.vertices(source);
        assertEquals(0, vertices.position());
        assertEquals(24, vertices.limit());
        assertEquals(ByteOrder.LITTLE_ENDIAN, vertices.order());
        assertThrows(ReadOnlyBufferException.class, () -> vertices.putInt(0, 4));
        ByteBuffer copy = SortBufferViews.indices(source);
        copy.putInt(0, 123);
        assertEquals(42, source.getInt(0));
        assertEquals(8, source.position());
        assertEquals(16, source.limit());
        assertThrows(IllegalArgumentException.class, () -> SortBufferViews.vertices(ByteBuffer.allocateDirect(4)));
    }

    @Test
    void generationIdentityRejectsRebuildUnloadAndUnacceptedResults() {
        Object generation = new Object();
        assertTrue(SortBufferViews.sameGeneration(true, generation, generation));
        assertFalse(SortBufferViews.sameGeneration(true, generation, new Object()));
        assertFalse(SortBufferViews.sameGeneration(true, generation, null));
        assertFalse(SortBufferViews.sameGeneration(true, null, null));
        assertFalse(SortBufferViews.sameGeneration(false, generation, generation));
    }

    @Test
    void readOnlyVertexSortingMatchesNativeCopiesForCompactAndFloatFormats() {
        try (var mods = mockStatic(ModList.class)) {
            mods.when(ModList::get).thenReturn(mock(ModList.class));
            compareNativeSorts();
        }
    }

    private static void compareNativeSorts() {
        var formats = new GlVertexFormat<?>[]{
                (GlVertexFormat<?>) ChunkModelVertexFormats.DEFAULT.getBufferVertexFormat(),
                (GlVertexFormat<?>) ChunkModelVertexFormats.VANILLA_LIKE.getBufferVertexFormat()};
        Random random = new Random(180203);
        for (int formatIndex = 0; formatIndex < formats.length; formatIndex++) {
            var format = formats[formatIndex];
            for (int sample = 0; sample < 20; sample++) {
                int count = 60;
                ByteBuffer vertices = ByteBuffer.allocate(count * format.getStride()).order(ByteOrder.LITTLE_ENDIAN);
                random.nextBytes(vertices.array());
                for (int vertex = 0; vertex < count; vertex++) {
                    int offset = vertex * format.getStride();
                    for (int axis = 0; axis < 3; axis++) {
                        if (formatIndex == 0) vertices.putShort(offset + axis * 2, (short) random.nextInt(65536));
                        else vertices.putFloat(offset + axis * 4, random.nextFloat() * 16);
                    }
                }
                ByteBuffer indices = ByteBuffer.allocate(count * 4).order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < count; i++) indices.putInt(i * 4, i);
                SortBuffer source = new SortBuffer(vertices, indices, format, Map.of());
                byte[] originalVertices = vertices.array().clone();
                byte[] originalIndices = indices.array().clone();
                for (int camera = 0; camera < 6; camera++) {
                    SortBuffer nativeCopy = source.duplicate();
                    SortBuffer shared = new SortBuffer(SortBufferViews.vertices(vertices),
                            SortBufferViews.indices(indices), format, Map.of());
                    float x = random.nextFloat() * 80 - 40;
                    float y = random.nextFloat() * 80 - 40;
                    float z = random.nextFloat() * 80 - 40;
                    ChunkBufferSorter.sort(nativeCopy, x, y, z);
                    ChunkBufferSorter.sort(shared, x, y, z);
                    assertArrayEquals(nativeCopy.indexBuffer().array(), shared.indexBuffer().array());
                    assertArrayEquals(originalVertices, vertices.array());
                    assertArrayEquals(originalIndices, indices.array());
                }
            }
        }
    }
}
