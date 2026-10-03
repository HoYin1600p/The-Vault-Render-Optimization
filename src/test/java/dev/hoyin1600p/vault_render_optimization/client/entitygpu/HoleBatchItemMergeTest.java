package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

/**
 * Consecutive item runs that continue the arena range and the output of the last record with the same instance
 * words share one GPU record; the CPU fill still writes every run from its own mesh.
 */
class HoleBatchItemMergeTest {
    @Test
    void adjacentRunsMergeAndFillExactly() {
        for (boolean iris : new boolean[]{false, true}) {
            Random random = new Random(iris ? 7 : 3);
            ItemMesh a = ItemMeshCapture.capture(quads(random, 2), ItemWriter.EMBEDDIUM);
            ItemMesh b = ItemMeshCapture.capture(quads(random, 1), ItemWriter.EMBEDDIUM);
            ItemMesh c = ItemMeshCapture.capture(quads(random, 3), ItemWriter.EMBEDDIUM);
            float[] pose = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0.25F, -1.5F, 3.0F, 1};
            float[] normal = {1, 0, 0, 0, 1, 0, 0, 0, 1};
            int stride = (iris ? IrisEntityExtension.WORDS_PER_VERTEX : EntityVertexPacking.WORDS_PER_VERTEX) * 4;
            int ids = iris ? IrisEntityExtension.idsWord(3, 1) : 0;
            int item = iris ? IrisEntityExtension.itemWord(77) : 0;
            HoleBatch batch = new HoleBatch();
            // Arena: a at 0 (8 vertices), b at 8 (4), c at 12 (12). Output: a, b, then a CPU-written quad, then c.
            assertTrue(batch.addItem(a, 0, 0, 8, pose, normal, 0, -1, 655360, 0xF000F0, true, iris, ids, item));
            assertTrue(batch.addItem(b, 8, 0, 4, pose, normal, 8 * stride, -1, 655360, 0xF000F0, true, iris, ids, item));
            assertEquals(1, batch.count(), "b continues a");
            int gap = 12 * stride;
            int cOut = gap + 4 * stride;
            assertTrue(batch.addItem(c, 12, 0, 8, pose, normal, cOut, -1, 655360, 0xF000F0, true, iris, ids, item));
            assertEquals(2, batch.count(), "c does not continue the output");
            assertTrue(batch.addItem(c, 12, 8, 4, pose, normal, cOut + 8 * stride, 0xFF3366CC, 655360, 0xF000F0, true,
                    iris, ids, item));
            assertEquals(3, batch.count(), "a different tint starts a record");
            assertEquals(12, batch.words()[InstanceRecord.VERTEX_COUNT]);
            assertFalse(batch.addParticle(0, 0, 0, 1, false, 0, 1, 0, 1, 0, 1, -1, 0, 0, 1, 0, 0, 0, 1, 0));

            int bytes = cOut + 12 * stride;
            ByteBuffer buffer = ByteBuffer.allocate(bytes).order(ByteOrder.LITTLE_ENDIAN);
            batch.setBase(0);
            batch.fillOnCpu(buffer);
            int[] expected = new int[bytes / 4];
            put(expected, 0, a, 0, 8, pose, normal, -1, iris, ids, item);
            put(expected, 8 * stride / 4, b, 0, 4, pose, normal, -1, iris, ids, item);
            put(expected, cOut / 4, c, 0, 8, pose, normal, -1, iris, ids, item);
            put(expected, (cOut + 8 * stride) / 4, c, 8, 4, pose, normal, 0xFF3366CC, iris, ids, item);
            for (int w = 0; w < expected.length; w++) {
                assertEquals(expected[w], buffer.getInt(w * 4), (iris ? "iris " : "") + "word " + w);
            }

            // Sorted buffers: only the positions of each quad's vertices 0 and 2 are written, bit-exactly.
            ByteBuffer sort = ByteBuffer.allocate(bytes).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < bytes; i += 4) sort.putInt(i, 0x5EED5EED);
            batch.writeSortPositions(sort);
            assertEquals(null, batch.checkSortPositions(sort));
            int words = stride / 4;
            for (int w = 0; w < expected.length; w++) {
                int inVertex = w % words;
                int vertex = w / words;
                boolean holeVertex = w < 12 * words || w >= cOut / 4;
                boolean sortWord = holeVertex && inVertex < 3 && (vertex % 4 == 0 || vertex % 4 == 2);
                assertEquals(sortWord ? expected[w] : 0x5EED5EED, sort.getInt(w * 4), "sort word " + w);
            }
        }
    }

    private static void put(int[] out, int at, ItemMesh mesh, int first, int count, float[] pose, float[] normal,
                            int tint, boolean iris, int ids, int item) {
        int[] nine = new int[count * EntityVertexPacking.WORDS_PER_VERTEX];
        ItemReference.expand(mesh, first, count, pose, normal, tint, 655360, 0xF000F0, true, nine, 0);
        if (!iris) {
            System.arraycopy(nine, 0, out, at, nine.length);
            return;
        }
        int[] fourteen = new int[count * IrisEntityExtension.WORDS_PER_VERTEX];
        ItemReference.extendIris(nine, 0, count, ids, item, true, fourteen, 0);
        System.arraycopy(fourteen, 0, out, at, fourteen.length);
    }

    private static List<BakedQuad> quads(Random random, int count) {
        List<BakedQuad> quads = new ArrayList<>();
        for (int q = 0; q < count; q++) {
            Direction direction = Direction.values()[random.nextInt(6)];
            int[] data = new int[32];
            for (int v = 0; v < 4; v++) {
                int d = v * 8;
                for (int k = 0; k < 3; k++) data[d + k] = Float.floatToRawIntBits(random.nextFloat());
                data[d + 3] = -1;
                data[d + 4] = Float.floatToRawIntBits(random.nextFloat());
                data[d + 5] = Float.floatToRawIntBits(random.nextFloat());
                data[d + 7] = (direction.getStepX() * 127 & 255) | (direction.getStepY() * 127 & 255) << 8
                        | (direction.getStepZ() * 127 & 255) << 16;
            }
            quads.add(new BakedQuad(data, random.nextBoolean() ? 0 : -1, direction, null, true));
        }
        return quads;
    }
}
