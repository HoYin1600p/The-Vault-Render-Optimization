package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.system.MemoryUtil;

/**
 * Runs the compute program on real model cubes and poses (mirrored, baby, non-uniform and random
 * matrices, normals past the clamp) and requires every output word to equal {@link ReferenceExpander}
 * and every word between the holes to stay untouched. Any difference blocks the feature.
 */
public final class GpuEntitySelfTest {
    static final int SENTINEL = 0xDEADBEEF;
    static final int GAP_WORDS = 5;

    private GpuEntitySelfTest() {
    }

    /** Test inputs, built on the CPU (unit tested without GL). */
    record Case(List<ModelMesh> meshes, int[] records, int count, int outputWords, int[] expected, boolean[] written) {
    }

    static Case build(long seed, int instances) {
        return build(seed, instances, false);
    }

    /** @param iris expected output in Oculus' extended entity format, with random captured IDs */
    static Case build(long seed, int instances, boolean iris) {
        Random random = new Random(seed);
        List<ModelMesh> meshes = new ArrayList<>();
        int meshVertices = 0;
        int[] records = new int[instances * InstanceRecord.WORDS];
        List<int[]> outputs = new ArrayList<>();
        int out = GAP_WORDS;
        int[] outOffsets = new int[instances];
        for (int i = 0; i < instances; i++) {
            ModelMesh mesh = randomMesh(random);
            float[] pose;
            float[] normal;
            if (random.nextInt(4) == 0) {
                pose = randomMatrix(random, 16);
                normal = randomMatrix(random, 9);
            } else {
                PoseStack.Pose last = randomPose(random).last();
                FloatBuffer p = FloatBuffer.allocate(16);
                last.pose().store(p);
                FloatBuffer n = FloatBuffer.allocate(9);
                last.normal().store(n);
                pose = p.array();
                normal = n.array();
            }
            int color = EntityVertexPacking.color(random.nextFloat(), random.nextFloat(), random.nextFloat(),
                    random.nextFloat());
            int overlay = random.nextInt(0x000F000F);
            int light = random.nextInt(0x00F000F1);
            float[] sprite = random.nextInt(3) == 0 ? randomSprite(random) : null;
            int flips = random.nextInt(2) == 0 ? (random.nextInt(8) << 1) : 0;
            InstanceRecord.write(records, i * InstanceRecord.WORDS, pose, normal, meshVertices, mesh.vertexCount(),
                    out, color, overlay, light, sprite, flips);
            int[] expected = new int[mesh.vertexCount() * EntityVertexPacking.WORDS_PER_VERTEX];
            ReferenceExpander.expand(mesh, pose, normal, color, overlay, light, sprite, flips, expected, 0);
            if (iris) {
                int ids = IrisEntityExtension.idsWord(random.nextInt(), random.nextInt());
                int item = IrisEntityExtension.itemWord(random.nextInt());
                records[i * InstanceRecord.WORDS + InstanceRecord.IRIS_IDS] = ids;
                records[i * InstanceRecord.WORDS + InstanceRecord.IRIS_ITEM] = item;
                int[] extended = new int[mesh.vertexCount() * IrisEntityExtension.WORDS_PER_VERTEX];
                IrisEntityExtension.extend(expected, 0, mesh.vertexCount(), ids, item, extended, 0);
                expected = extended;
            }
            outputs.add(expected);
            outOffsets[i] = out;
            out += expected.length + GAP_WORDS;
            meshes.add(mesh);
            meshVertices += mesh.vertexCount();
        }
        int[] expected = new int[out];
        boolean[] written = new boolean[out];
        java.util.Arrays.fill(expected, SENTINEL);
        for (int i = 0; i < instances; i++) {
            int[] words = outputs.get(i);
            System.arraycopy(words, 0, expected, outOffsets[i], words.length);
            java.util.Arrays.fill(written, outOffsets[i], outOffsets[i] + words.length, true);
        }
        return new Case(meshes, records, instances, out, expected, written);
    }

    private static ModelMesh randomMesh(Random random) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        int count = 1 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            cubes.add(new ModelPart.Cube(random.nextInt(64), random.nextInt(64),
                    random.nextFloat() * 16 - 8, random.nextFloat() * 16 - 8, random.nextFloat() * 16 - 8,
                    1 + random.nextInt(12), 1 + random.nextInt(12), random.nextInt(4) == 0 ? 0 : 1 + random.nextInt(12),
                    random.nextInt(3) == 0 ? random.nextFloat() : 0, random.nextInt(3) == 0 ? random.nextFloat() : 0,
                    random.nextInt(3) == 0 ? random.nextFloat() : 0,
                    random.nextBoolean(), 64, 32 + 32 * random.nextInt(2)));
        }
        return ModelMeshCapture.capture(new ModelPart(cubes, Map.of()));
    }

    /** An atlas sprite's {u0, u1 - u0, v0, v1 - v0} as a 1.18.2 atlas would lay it out. */
    static float[] randomSprite(Random random) {
        int atlas = 1 << (8 + random.nextInt(6));
        int size = 16 << random.nextInt(3);
        int x = random.nextInt(atlas / size) * size, y = random.nextInt(atlas / size) * size;
        float u0 = (float) x / atlas, u1 = (float) (x + size) / atlas;
        float v0 = (float) y / atlas, v1 = (float) (y + size) / atlas;
        return new float[]{u0, u1 - u0, v0, v1 - v0};
    }

    private static PoseStack randomPose(Random random) {
        PoseStack stack = new PoseStack();
        stack.translate(random.nextDouble() * 64 - 32, random.nextDouble() * 64 - 32, random.nextDouble() * 64 - 32);
        stack.mulPose(Vector3f.YP.rotationDegrees(random.nextFloat() * 360));
        stack.mulPose(Vector3f.XP.rotationDegrees(random.nextFloat() * 360));
        switch (random.nextInt(5)) {
            case 0 -> stack.scale(-1.0F, -1.0F, 1.0F);
            case 1 -> stack.scale(0.5F, 0.5F, 0.5F);
            case 2 -> stack.scale(random.nextFloat() + 0.2F, random.nextFloat() + 0.2F, random.nextFloat() + 0.2F);
            case 3 -> stack.scale(-0.9375F, -0.9375F, 0.9375F);
            default -> { }
        }
        stack.translate(random.nextFloat() - 0.5F, random.nextFloat() * 1.5F, random.nextFloat() - 0.5F);
        stack.mulPose(Vector3f.ZP.rotation(random.nextFloat() * 6.3F - 3.15F));
        return stack;
    }

    /** Arbitrary finite matrices; entries past +-1 drive the normal clamp, zeros give signed zeros. */
    private static float[] randomMatrix(Random random, int size) {
        float[] matrix = new float[size];
        for (int i = 0; i < size; i++) {
            matrix[i] = switch (random.nextInt(6)) {
                case 0 -> 0.0F;
                case 1 -> -0.0F;
                case 2 -> (random.nextFloat() - 0.5F) * 4096.0F;
                default -> (random.nextFloat() - 0.5F) * 3.0F;
            };
        }
        return matrix;
    }

    /** @return null when the GPU output is bit-identical, otherwise the first difference */
    static String run(GpuEntityBackend backend, long seed, int instances) {
        return run(backend, seed, instances, false);
    }

    static String run(GpuEntityBackend backend, long seed, int instances, boolean iris) {
        Case test = build(seed, instances, iris);
        int meshBuffer = GL15C.glGenBuffers();
        int outputBuffer = GL15C.glGenBuffers();
        ByteBuffer meshData = null;
        IntBuffer sentinel = null;
        IntBuffer readback = null;
        try {
            int meshFloats = 0;
            for (ModelMesh mesh : test.meshes()) meshFloats += mesh.vertexCount() * ModelMesh.FLOATS_PER_VERTEX;
            meshData = MemoryUtil.memAlloc(Math.max(4, meshFloats * 4));
            FloatBuffer floats = meshData.asFloatBuffer();
            for (ModelMesh mesh : test.meshes()) floats.put(mesh.data(), 0, mesh.vertexCount() * ModelMesh.FLOATS_PER_VERTEX);
            GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, meshBuffer);
            GL15C.glBufferData(GL43C.GL_SHADER_STORAGE_BUFFER, meshData, GL15C.GL_STATIC_DRAW);

            sentinel = MemoryUtil.memAllocInt(test.outputWords());
            for (int i = 0; i < test.outputWords(); i++) sentinel.put(i, SENTINEL);
            GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, outputBuffer);
            GL15C.glBufferData(GL43C.GL_SHADER_STORAGE_BUFFER, sentinel, GL15C.GL_STATIC_DRAW);
            GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, 0);

            // A small group limit exercises the multi-dispatch path.
            backend.run(meshBuffer, backend.stage(test.records(), test.count()), test.count(), outputBuffer, 7, true, iris);
            GL20C.glUseProgram(0);

            readback = MemoryUtil.memAllocInt(test.outputWords());
            GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, outputBuffer);
            GL15C.glGetBufferSubData(GL43C.GL_SHADER_STORAGE_BUFFER, 0L, readback);
            GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, 0);
            int error = GL11C.glGetError();
            if (error != GL11C.GL_NO_ERROR) return "GL error 0x" + Integer.toHexString(error) + " during the self-test";
            return compare(test, readback);
        } finally {
            GL15C.glDeleteBuffers(meshBuffer);
            GL15C.glDeleteBuffers(outputBuffer);
            if (meshData != null) MemoryUtil.memFree(meshData);
            if (sentinel != null) MemoryUtil.memFree(sentinel);
            if (readback != null) MemoryUtil.memFree(readback);
        }
    }

    /** Item-mode test inputs: several records per instance mesh (one per run of equal tint index). */
    record ItemCase(List<ItemMesh> meshes, int[] records, int count, int outputWords, int[] expected,
                    boolean[] written) {
    }

    /**
     * Random item quads through {@link ItemMeshCapture} with both writers, varied baked colors, baked light,
     * baked normals (zero, axis, arbitrary) and tint runs, expanded by {@link ItemReference}.
     */
    static ItemCase buildItems(long seed, int instances, boolean iris) {
        Random random = new Random(seed);
        List<ItemMesh> meshes = new ArrayList<>();
        List<int[]> recordWords = new ArrayList<>();
        List<int[]> outputs = new ArrayList<>();
        List<Integer> outOffsets = new ArrayList<>();
        int meshVertices = 0;
        int out = GAP_WORDS;
        for (int i = 0; i < instances; i++) {
            ItemWriter writer = random.nextBoolean() ? ItemWriter.EMBEDDIUM : ItemWriter.FORGE;
            boolean embeddium = writer == ItemWriter.EMBEDDIUM;
            int quadCount = 1 + random.nextInt(6);
            int[] tintIndex = new int[quadCount];
            List<BakedQuad> quads = new ArrayList<>();
            for (int q = 0; q < quadCount; q++) {
                tintIndex[q] = q > 0 && random.nextInt(3) != 0 ? tintIndex[q - 1] : random.nextInt(4) - 1;
                quads.add(randomQuad(random, tintIndex[q]));
            }
            ItemMesh mesh = ItemMeshCapture.capture(quads, writer);
            float[] pose;
            float[] normal;
            if (random.nextInt(4) == 0) {
                pose = randomMatrix(random, 16);
                normal = randomMatrix(random, 9);
            } else {
                PoseStack.Pose last = randomPose(random).last();
                FloatBuffer p = FloatBuffer.allocate(16);
                last.pose().store(p);
                FloatBuffer n = FloatBuffer.allocate(9);
                last.normal().store(n);
                pose = p.array();
                normal = n.array();
            }
            int overlay = random.nextInt(0x000F000F);
            int light = random.nextInt(4) == 0 ? random.nextInt() : random.nextInt(0x00F000F1);
            int ids = IrisEntityExtension.idsWord(random.nextInt(), random.nextInt());
            int itemId = IrisEntityExtension.itemWord(random.nextInt());
            int q = 0;
            while (q < quadCount) {
                int end = q + 1;
                while (end < quadCount && tintIndex[end] == tintIndex[q]) end++;
                int tint = tintIndex[q] < 0 ? -1 : (random.nextInt() | 0xFF000000);
                int first = q * 4;
                int vertexCount = (end - q) * 4;
                int[] record = new int[InstanceRecord.WORDS];
                InstanceRecord.write(record, 0, pose, normal, meshVertices + first, vertexCount, out, tint, overlay,
                        light, null, 0);
                record[InstanceRecord.SPRITE] = embeddium ? InstanceRecord.FLAG_ITEM_EMBEDDIUM : 0;
                int[] expected = new int[vertexCount * EntityVertexPacking.WORDS_PER_VERTEX];
                ItemReference.expand(mesh, first, vertexCount, pose, normal, tint, overlay, light, embeddium,
                        expected, 0);
                if (iris) {
                    record[InstanceRecord.IRIS_IDS] = ids;
                    record[InstanceRecord.IRIS_ITEM] = itemId;
                    int[] extended = new int[vertexCount * IrisEntityExtension.WORDS_PER_VERTEX];
                    ItemReference.extendIris(expected, 0, vertexCount, ids, itemId, embeddium, extended, 0);
                    expected = extended;
                }
                recordWords.add(record);
                outputs.add(expected);
                outOffsets.add(out);
                out += expected.length + GAP_WORDS;
                q = end;
            }
            meshes.add(mesh);
            meshVertices += mesh.vertexCount();
        }
        int[] records = new int[recordWords.size() * InstanceRecord.WORDS];
        for (int i = 0; i < recordWords.size(); i++) {
            System.arraycopy(recordWords.get(i), 0, records, i * InstanceRecord.WORDS, InstanceRecord.WORDS);
        }
        int[] expected = new int[out];
        boolean[] written = new boolean[out];
        java.util.Arrays.fill(expected, SENTINEL);
        for (int i = 0; i < outputs.size(); i++) {
            int[] words = outputs.get(i);
            System.arraycopy(words, 0, expected, outOffsets.get(i), words.length);
            java.util.Arrays.fill(written, outOffsets.get(i), outOffsets.get(i) + words.length, true);
        }
        return new ItemCase(meshes, records, recordWords.size(), out, expected, written);
    }

    /** A random baked quad: 32 ints, DefaultVertexFormat.BLOCK layout. */
    static BakedQuad randomQuad(Random random, int tintIndex) {
        int[] data = new int[32];
        Direction direction = Direction.values()[random.nextInt(6)];
        boolean degenerate = random.nextInt(24) == 0;
        float bx = random.nextFloat() * 2 - 0.5F, by = random.nextFloat() * 2 - 0.5F, bz = random.nextFloat() * 2 - 0.5F;
        for (int v = 0; v < 4; v++) {
            int d = v * 8;
            float x = degenerate ? bx : random.nextInt(3) == 0 ? random.nextInt(2) : random.nextFloat() * 2 - 0.5F;
            float y = degenerate ? by : random.nextInt(3) == 0 ? random.nextInt(2) : random.nextFloat() * 2 - 0.5F;
            float z = degenerate ? bz : random.nextInt(3) == 0 ? random.nextInt(2) : random.nextFloat() * 2 - 0.5F;
            data[d] = Float.floatToRawIntBits(x);
            data[d + 1] = Float.floatToRawIntBits(y);
            data[d + 2] = Float.floatToRawIntBits(z);
            data[d + 3] = switch (random.nextInt(5)) {
                case 0 -> -1;
                case 1 -> 0;
                case 2 -> random.nextInt() | 0xFF000000;
                default -> random.nextInt();
            };
            data[d + 4] = Float.floatToRawIntBits(random.nextInt(4) == 0 ? random.nextInt(2) : random.nextFloat());
            data[d + 5] = Float.floatToRawIntBits(random.nextInt(4) == 0 ? random.nextInt(2) : random.nextFloat());
            data[d + 6] = switch (random.nextInt(4)) {
                case 0, 1 -> 0;
                case 2 -> (random.nextInt(16) << 20) | (random.nextInt(16) << 4);
                default -> random.nextInt();
            };
            data[d + 7] = switch (random.nextInt(5)) {
                case 0 -> 0;
                case 1 -> ((direction.getStepX() * 127) & 255) | (((direction.getStepY() * 127) & 255) << 8)
                        | (((direction.getStepZ() * 127) & 255) << 16);
                case 2 -> random.nextInt();
                default -> ((random.nextInt(255) - 127) & 255) | (((random.nextInt(255) - 127) & 255) << 8)
                        | (((random.nextInt(255) - 127) & 255) << 16);
            };
        }
        return new BakedQuad(data, tintIndex, direction, null, random.nextBoolean());
    }

    /**
     * Item quads (and, with {@code iris}, Oculus extended item vertices): the item program on random baked
     * quads against {@link ItemReference}, word for word, sentinel gaps untouched.
     *
     * @param items false runs the model mode of {@link #run(GpuEntityBackend, long, int, boolean)}
     */
    static String run(GpuEntityBackend backend, long seed, int instances, boolean items, boolean iris) {
        if (!items) return run(backend, seed, instances, iris);
        if (iris ? !backend.itemsIrisAvailable() : !backend.itemsAvailable()) {
            return "the item program is unavailable: " + (iris ? backend.itemIrisFailure() : backend.itemFailure());
        }
        ItemCase test = buildItems(seed, instances, iris);
        int meshBuffer = GL15C.glGenBuffers();
        int outputBuffer = GL15C.glGenBuffers();
        ByteBuffer meshData = null;
        IntBuffer sentinel = null;
        IntBuffer readback = null;
        try {
            int meshWords = 0;
            for (ItemMesh mesh : test.meshes()) meshWords += mesh.vertexCount() * ItemMesh.WORDS_PER_VERTEX;
            meshData = MemoryUtil.memAlloc(Math.max(4, meshWords * 4));
            IntBuffer ints = meshData.asIntBuffer();
            for (ItemMesh mesh : test.meshes()) {
                ints.put(mesh.words(), 0, mesh.vertexCount() * ItemMesh.WORDS_PER_VERTEX);
            }
            GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, meshBuffer);
            GL15C.glBufferData(GL43C.GL_SHADER_STORAGE_BUFFER, meshData, GL15C.GL_STATIC_DRAW);

            sentinel = MemoryUtil.memAllocInt(test.outputWords());
            for (int i = 0; i < test.outputWords(); i++) sentinel.put(i, SENTINEL);
            GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, outputBuffer);
            GL15C.glBufferData(GL43C.GL_SHADER_STORAGE_BUFFER, sentinel, GL15C.GL_STATIC_DRAW);
            GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, 0);

            backend.run(meshBuffer, backend.stage(test.records(), test.count()), test.count(), outputBuffer, 7, true,
                    iris, true);
            GL20C.glUseProgram(0);

            readback = MemoryUtil.memAllocInt(test.outputWords());
            GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, outputBuffer);
            GL15C.glGetBufferSubData(GL43C.GL_SHADER_STORAGE_BUFFER, 0L, readback);
            GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, 0);
            int error = GL11C.glGetError();
            if (error != GL11C.GL_NO_ERROR) {
                return "GL error 0x" + Integer.toHexString(error) + " during the item self-test";
            }
            return compare(test.expected(), test.written(), readback);
        } finally {
            GL15C.glDeleteBuffers(meshBuffer);
            GL15C.glDeleteBuffers(outputBuffer);
            if (meshData != null) MemoryUtil.memFree(meshData);
            if (sentinel != null) MemoryUtil.memFree(sentinel);
            if (readback != null) MemoryUtil.memFree(readback);
        }
    }

    static String compare(ItemCase test, IntBuffer actual) {
        return compare(test.expected(), test.written(), actual);
    }

    static String compare(Case test, IntBuffer actual) {
        return compare(test.expected(), test.written(), actual);
    }

    static String compare(int[] expected, boolean[] written, IntBuffer actual) {
        for (int i = 0; i < expected.length; i++) {
            int got = actual.get(i);
            if (got != expected[i]) {
                return (written[i] ? "vertex word " : "gap word ") + i + " expected 0x" + Integer.toHexString(expected[i])
                        + " but the GPU wrote 0x" + Integer.toHexString(got);
            }
        }
        return null;
    }
}
