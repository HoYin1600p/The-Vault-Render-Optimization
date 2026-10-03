package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * The reserved (not yet written) model vertices of one {@code BufferBuilder} draw state. Each entry
 * is an {@link InstanceRecord} plus the CPU mesh, so the holes can always be filled on the CPU with
 * the exact vanilla bytes when the GPU path cannot take them.
 */
public final class HoleBatch {
    int[] words = new int[InstanceRecord.WORDS * 16];
    ModelMesh[] meshes = new ModelMesh[16];
    /**
     * Item batches only: the CPU-fill segments. Consecutive item runs that continue both the arena range and the
     * output of the previous record with the same instance words share one GPU record, so a record can span
     * several meshes; each run keeps its own segment (mesh, first vertex, count, output, record).
     */
    ItemMesh[] segMesh = new ItemMesh[16];
    int[] segInts = new int[16 * 4];
    int segments;
    int count;
    int vertices;
    /** Index of the builder draw state these holes belong to (set when the builder ends). */
    int stateIndex = -1;
    /** Absolute byte offset of the draw state inside the builder, set when it is popped. */
    int base;
    /** Oculus extended entity vertices ({@link IrisEntityExtension}) instead of {@code NEW_ENTITY}. */
    boolean iris;
    private int[] extended = new int[IrisEntityExtension.WORDS_PER_VERTEX * 64];

    private final float[] pose = new float[16];
    private final float[] normal = new float[9];
    private final float[] sprite = new float[4];
    private int[] scratch = new int[EntityVertexPacking.WORDS_PER_VERTEX * 64];

    // Particle holes (a batch is either model parts or particles: one draw state has one format).
    int[] particleWords = new int[ParticleRecord.WORDS * 64];
    int particleCount;
    /** The camera's {left, up} vectors every particle record of this batch was built with. */
    final float[] camera = new float[6];
    private final int[] particleScratch = new int[ParticleRecord.VERTICES * ParticleRecord.VERTEX_WORDS];

    /** What a batch holds; an empty batch takes any kind, a non-empty one only its own. */
    public enum Kind { MODELS, ITEMS, PARTICLES }

    public Kind kind() {
        return particleCount > 0 ? Kind.PARTICLES : items() ? Kind.ITEMS : Kind.MODELS;
    }

    /** True when the records are item quads ({@link ItemMesh}), expanded by the item programs. */
    public boolean items() {
        return items && count > 0;
    }

    private boolean items;

    /** @return whether a model part can join this batch (it holds no items and no particles) */
    public boolean canAddModel() {
        return particleCount == 0 && !items();
    }

    public boolean particles() {
        return particleCount > 0;
    }

    public int particleCount() {
        return particleCount;
    }

    public int[] particleWords() {
        return particleWords;
    }

    public float[] camera() {
        return camera;
    }

    /**
     * @return false when this batch cannot take the particle (it holds model parts, or its particles were
     *         built with another camera basis); the caller then writes the particle on the CPU
     */
    public boolean addParticle(float x, float y, float z, float size, boolean rolled, float sin, float cos,
                               float minU, float maxU, float minV, float maxV, int color, int light, int byteOffset,
                               float leftX, float leftY, float leftZ, float upX, float upY, float upZ) {
        if (count > 0) return false;
        if (particleCount == 0) {
            camera[0] = leftX; camera[1] = leftY; camera[2] = leftZ;
            camera[3] = upX; camera[4] = upY; camera[5] = upZ;
        } else if (Float.floatToRawIntBits(camera[0]) != Float.floatToRawIntBits(leftX)
                || Float.floatToRawIntBits(camera[1]) != Float.floatToRawIntBits(leftY)
                || Float.floatToRawIntBits(camera[2]) != Float.floatToRawIntBits(leftZ)
                || Float.floatToRawIntBits(camera[3]) != Float.floatToRawIntBits(upX)
                || Float.floatToRawIntBits(camera[4]) != Float.floatToRawIntBits(upY)
                || Float.floatToRawIntBits(camera[5]) != Float.floatToRawIntBits(upZ)) {
            return false;
        }
        if ((particleCount + 1) * ParticleRecord.WORDS > particleWords.length) {
            particleWords = Arrays.copyOf(particleWords, particleWords.length * 2);
        }
        ParticleRecord.write(particleWords, particleCount * ParticleRecord.WORDS, x, y, z, size, rolled, sin, cos,
                minU, maxU, minV, maxV, color, light, byteOffset);
        particleCount++;
        vertices += ParticleRecord.VERTICES;
        return true;
    }

    /** Reserved ranges in order, for the gap upload: model-part records or particle records. */
    int holeCount() {
        return particles() ? particleCount : count;
    }

    /** Byte offset of hole {@code i} inside the popped buffer. */
    int holeStart(int i) {
        return particles() ? particleWords[i * ParticleRecord.WORDS + ParticleRecord.OUTPUT] - base : outputWord(i) * 4;
    }

    int holeEnd(int i) {
        return particles()
                ? holeStart(i) + ParticleRecord.VERTICES * ParticleRecord.VERTEX_WORDS * 4
                : holeStart(i) + words[i * InstanceRecord.WORDS + InstanceRecord.VERTEX_COUNT]
                        * vertexWords() * 4;
    }

    public boolean iris() {
        return iris;
    }

    /** Words per reserved model vertex: 9 for {@code NEW_ENTITY}, 14 for Oculus' extended format. */
    int vertexWords() {
        return iris ? IrisEntityExtension.WORDS_PER_VERTEX : EntityVertexPacking.WORDS_PER_VERTEX;
    }

    public void add(ModelMesh mesh, int meshFirst, float[] pose, float[] normal, int byteOffset, int color, int overlay,
             int light) {
        add(mesh, meshFirst, pose, normal, byteOffset, color, overlay, light, null);
    }

    public void add(ModelMesh mesh, int meshFirst, float[] pose, float[] normal, int byteOffset, int color, int overlay,
                    int light, float[] sprite) {
        add(mesh, meshFirst, pose, normal, byteOffset, color, overlay, light, sprite, 0);
    }

    public void add(ModelMesh mesh, int meshFirst, float[] pose, float[] normal, int byteOffset, int color, int overlay,
                    int light, float[] sprite, int flips) {
        add(mesh, meshFirst, pose, normal, byteOffset, color, overlay, light, sprite, flips, false, 0, 0);
    }

    /**
     * @param iris     the builder writes Oculus' extended entity format; every record of a batch agrees
     * @param idsWord  {@link IrisEntityExtension#idsWord} of Oculus' captured entity and block entity IDs
     * @param itemWord {@link IrisEntityExtension#itemWord} of Oculus' captured item ID
     */
    public void add(ModelMesh mesh, int meshFirst, float[] pose, float[] normal, int byteOffset, int color, int overlay,
                    int light, float[] sprite, int flips, boolean iris, int idsWord, int itemWord) {
        this.iris = iris;
        this.items = false;
        grow();
        InstanceRecord.write(words, count * InstanceRecord.WORDS, pose, normal, meshFirst, mesh.vertexCount(),
                byteOffset, color, overlay, light, sprite, flips);
        words[count * InstanceRecord.WORDS + InstanceRecord.IRIS_IDS] = idsWord;
        words[count * InstanceRecord.WORDS + InstanceRecord.IRIS_ITEM] = itemWord;
        meshes[count++] = mesh;
        vertices += mesh.vertexCount();
    }

    private void grow() {
        if (count == meshes.length) {
            meshes = Arrays.copyOf(meshes, count * 2);
            words = Arrays.copyOf(words, count * 2 * InstanceRecord.WORDS);
        }
    }

    /**
     * Adds a run of item quads.
     *
     * @param meshFirst   arena vertex of {@code mesh}'s first vertex ({@link GpuEntityBackend#uploadItem})
     * @param firstVertex first vertex of the run within {@code mesh} (a multiple of four)
     * @param tint        ABGR tint word, -1 for untinted
     * @param embeddium   the Embeddium writer's rules ({@link InstanceRecord#FLAG_ITEM_EMBEDDIUM}), else Forge's
     * @return false when this batch holds model parts or particles (the caller then writes on the CPU)
     */
    public boolean addItem(ItemMesh mesh, int meshFirst, int firstVertex, int vertexCount, float[] pose,
                           float[] normal, int byteOffset, int tint, int overlay, int light, boolean embeddium,
                           boolean iris, int idsWord, int itemWord) {
        if (particleCount > 0 || (count > 0 && !items)) return false;
        if (count > 0 && this.iris != iris) return false;
        this.items = true;
        this.iris = iris;
        if (segments == segMesh.length) {
            segMesh = Arrays.copyOf(segMesh, segments * 2);
            segInts = Arrays.copyOf(segInts, segments * 2 * 4);
        }
        int flags = embeddium ? InstanceRecord.FLAG_ITEM_EMBEDDIUM : 0;
        if (count > 0 && extendsLast(meshFirst + firstVertex, byteOffset, pose, normal, tint, overlay, light, flags,
                idsWord, itemWord)) {
            words[(count - 1) * InstanceRecord.WORDS + InstanceRecord.VERTEX_COUNT] += vertexCount;
            addSegment(mesh, firstVertex, vertexCount, byteOffset, count - 1);
            vertices += vertexCount;
            return true;
        }
        grow();
        int r = count * InstanceRecord.WORDS;
        InstanceRecord.write(words, r, pose, normal, meshFirst + firstVertex, vertexCount, byteOffset, tint, overlay,
                light, null, 0);
        words[r + InstanceRecord.SPRITE] = embeddium ? InstanceRecord.FLAG_ITEM_EMBEDDIUM : 0;
        words[r + InstanceRecord.IRIS_IDS] = idsWord;
        words[r + InstanceRecord.IRIS_ITEM] = itemWord;
        words[r + InstanceRecord.SPRITE] = flags;
        addSegment(mesh, firstVertex, vertexCount, byteOffset, count);
        meshes[count++] = null;
        vertices += vertexCount;
        return true;
    }

    private void addSegment(ItemMesh mesh, int firstVertex, int vertexCount, int byteOffset, int record) {
        segMesh[segments] = mesh;
        int s = segments * 4;
        segInts[s] = firstVertex;
        segInts[s + 1] = vertexCount;
        segInts[s + 2] = byteOffset;
        segInts[s + 3] = record;
        segments++;
    }

    /** True when a run continues the last record: same instance words, next arena vertex and next output byte. */
    private boolean extendsLast(int arenaFirst, int byteOffset, float[] pose, float[] normal, int tint, int overlay,
                                int light, int flags, int idsWord, int itemWord) {
        int r = (count - 1) * InstanceRecord.WORDS;
        int last = words[r + InstanceRecord.VERTEX_COUNT];
        if (words[r + InstanceRecord.MESH_FIRST] + last != arenaFirst
                || words[r + InstanceRecord.OUTPUT] + last * vertexWords() * 4 != byteOffset
                || words[r + InstanceRecord.COLOR] != tint
                || words[r + InstanceRecord.OVERLAY] != EntityVertexPacking.shorts(overlay)
                || words[r + InstanceRecord.LIGHT] != EntityVertexPacking.shorts(light)
                || words[r + InstanceRecord.SPRITE] != flags || words[r + InstanceRecord.IRIS_IDS] != idsWord
                || words[r + InstanceRecord.IRIS_ITEM] != itemWord) {
            return false;
        }
        for (int k = 0; k < 16; k++) {
            if (words[r + InstanceRecord.POSE + k] != Float.floatToRawIntBits(pose[k])) return false;
        }
        for (int k = 0; k < 9; k++) {
            if (words[r + InstanceRecord.NORMAL + k] != Float.floatToRawIntBits(normal[k])) return false;
        }
        return true;
    }

    public int stateIndex() {
        return stateIndex;
    }

    public void setStateIndex(int stateIndex) {
        this.stateIndex = stateIndex;
    }

    /** Absolute byte offset where the popped slice starts (0 for the builder's own buffer). */
    public void setBase(int base) {
        this.base = base;
    }

    public int count() {
        return count;
    }

    public int vertices() {
        return vertices;
    }

    public int[] words() {
        return words;
    }

    /** Word offset of record {@code i}'s output inside the popped buffer. */
    int outputWord(int i) {
        return (words[i * InstanceRecord.WORDS + InstanceRecord.OUTPUT] - base) >> 2;
    }

    /** Writes every hole with {@link ReferenceExpander}: the same bytes the vanilla path writes. */
    public void fillOnCpu(ByteBuffer popped) {
        boolean swap = popped.order() != ByteOrder.LITTLE_ENDIAN;
        for (int i = 0; i < particleCount; i++) {
            int r = i * ParticleRecord.WORDS;
            ParticleReference.expand(particleWords, r, camera, particleScratch, 0);
            int at = particleWords[r + ParticleRecord.OUTPUT] - base;
            for (int w = 0; w < particleScratch.length; w++) {
                popped.putInt(at + w * 4, swap ? Integer.reverseBytes(particleScratch[w]) : particleScratch[w]);
            }
        }
        if (items) {
            for (int s = 0; s < segments; s++) fillItem(popped, swap, s);
            return;
        }
        for (int i = 0; i < count; i++) {
            int r = i * InstanceRecord.WORDS;
            for (int k = 0; k < 16; k++) pose[k] = Float.intBitsToFloat(words[r + InstanceRecord.POSE + k]);
            for (int k = 0; k < 9; k++) normal[k] = Float.intBitsToFloat(words[r + InstanceRecord.NORMAL + k]);
            ModelMesh mesh = meshes[i];
            int needed = mesh.vertexCount() * EntityVertexPacking.WORDS_PER_VERTEX;
            if (scratch.length < needed) scratch = new int[needed];
            // The stored overlay/light words are the packed ints themselves (shorts() is the identity).
            int flags = words[r + InstanceRecord.SPRITE];
            boolean remap = (flags & InstanceRecord.FLAG_SPRITE) != 0;
            for (int k = 0; k < 4; k++) sprite[k] = Float.intBitsToFloat(words[r + InstanceRecord.SPRITE_U0 + k]);
            ReferenceExpander.expand(mesh, pose, normal, words[r + InstanceRecord.COLOR],
                    words[r + InstanceRecord.OVERLAY], words[r + InstanceRecord.LIGHT], remap ? sprite : null,
                    flags & InstanceRecord.FLIPS, scratch, 0);
            int[] result = scratch;
            int resultWords = needed;
            if (iris) {
                resultWords = mesh.vertexCount() * IrisEntityExtension.WORDS_PER_VERTEX;
                if (extended.length < resultWords) extended = new int[resultWords];
                IrisEntityExtension.extend(scratch, 0, mesh.vertexCount(), words[r + InstanceRecord.IRIS_IDS],
                        words[r + InstanceRecord.IRIS_ITEM], extended, 0);
                result = extended;
            }
            int at = words[r + InstanceRecord.OUTPUT] - base;
            for (int w = 0; w < resultWords; w++) {
                popped.putInt(at + w * 4, swap ? Integer.reverseBytes(result[w]) : result[w]);
            }
        }
    }

    private void fillItem(ByteBuffer popped, boolean swap, int segment) {
        int s = segment * 4;
        int r = segInts[s + 3] * InstanceRecord.WORDS;
        for (int k = 0; k < 16; k++) pose[k] = Float.intBitsToFloat(words[r + InstanceRecord.POSE + k]);
        for (int k = 0; k < 9; k++) normal[k] = Float.intBitsToFloat(words[r + InstanceRecord.NORMAL + k]);
        int vertexCount = segInts[s + 1];
        int needed = vertexCount * EntityVertexPacking.WORDS_PER_VERTEX;
        if (scratch.length < needed) scratch = new int[needed];
        boolean embeddium = (words[r + InstanceRecord.SPRITE] & InstanceRecord.FLAG_ITEM_EMBEDDIUM) != 0;
        ItemReference.expand(segMesh[segment], segInts[s], vertexCount, pose, normal, words[r + InstanceRecord.COLOR],
                words[r + InstanceRecord.OVERLAY], words[r + InstanceRecord.LIGHT], embeddium, scratch, 0);
        int[] result = scratch;
        int resultWords = needed;
        if (iris) {
            resultWords = vertexCount * IrisEntityExtension.WORDS_PER_VERTEX;
            if (extended.length < resultWords) extended = new int[resultWords];
            ItemReference.extendIris(scratch, 0, vertexCount, words[r + InstanceRecord.IRIS_IDS],
                    words[r + InstanceRecord.IRIS_ITEM], embeddium, extended, 0);
            result = extended;
        }
        int at = segInts[s + 2] - base;
        for (int w = 0; w < resultWords; w++) {
            popped.putInt(at + w * 4, swap ? Integer.reverseBytes(result[w]) : result[w]);
        }
    }

    /**
     * Sorted render types (flat and translucent items): vanilla computes each quad's sort point from the
     * positions of its vertices 0 and 2 as written in the builder. Writes exactly those position floats for
     * every reserved quad (same arithmetic as {@link ItemReference}); the rest of the hole stays for the GPU,
     * and these bytes are never uploaded (the gap upload skips holes, the shader rewrites the whole vertex).
     */
    public void writeSortPositions(ByteBuffer builder) {
        boolean swap = builder.order() != ByteOrder.LITTLE_ENDIAN;
        int stride = vertexWords() * 4;
        for (int s = 0; s < segments; s++) {
            int si = s * 4;
            int r = segInts[si + 3] * InstanceRecord.WORDS;
            float m00 = f(r, 0), m10 = f(r, 1), m20 = f(r, 2), m01 = f(r, 4), m11 = f(r, 5), m21 = f(r, 6);
            float m02 = f(r, 8), m12 = f(r, 9), m22 = f(r, 10), m03 = f(r, 12), m13 = f(r, 13), m23 = f(r, 14);
            int[] mesh = segMesh[s].words();
            int first = segInts[si], vertexCount = segInts[si + 1], out = segInts[si + 2] - base;
            for (int v = 0; v < vertexCount; v++) {
                if ((v & 3) != 0 && (v & 3) != 2) continue;
                int d = (first + v) * ItemMesh.WORDS_PER_VERTEX;
                float x = Float.intBitsToFloat(mesh[d]), y = Float.intBitsToFloat(mesh[d + 1]), z = Float.intBitsToFloat(mesh[d + 2]);
                float px = m00 * x + m01 * y + m02 * z + m03 * 1.0F;
                float py = m10 * x + m11 * y + m12 * z + m13 * 1.0F;
                float pz = m20 * x + m21 * y + m22 * z + m23 * 1.0F;
                int at = out + v * stride;
                put(builder, at, Float.floatToRawIntBits(px), swap);
                put(builder, at + 4, Float.floatToRawIntBits(py), swap);
                put(builder, at + 8, Float.floatToRawIntBits(pz), swap);
            }
        }
    }

    /** Verify mode: the sort positions written above equal the full reference fill's. */
    public String checkSortPositions(ByteBuffer builder) {
        boolean swap = builder.order() != ByteOrder.LITTLE_ENDIAN;
        int stride = vertexWords() * 4;
        for (int s = 0; s < segments; s++) {
            int si = s * 4;
            int r = segInts[si + 3] * InstanceRecord.WORDS;
            for (int k = 0; k < 16; k++) pose[k] = Float.intBitsToFloat(words[r + InstanceRecord.POSE + k]);
            for (int k = 0; k < 9; k++) normal[k] = Float.intBitsToFloat(words[r + InstanceRecord.NORMAL + k]);
            int vertexCount = segInts[si + 1];
            int needed = vertexCount * EntityVertexPacking.WORDS_PER_VERTEX;
            if (scratch.length < needed) scratch = new int[needed];
            ItemReference.expand(segMesh[s], segInts[si], vertexCount, pose, normal, words[r + InstanceRecord.COLOR],
                    words[r + InstanceRecord.OVERLAY], words[r + InstanceRecord.LIGHT],
                    (words[r + InstanceRecord.SPRITE] & InstanceRecord.FLAG_ITEM_EMBEDDIUM) != 0, scratch, 0);
            int out = segInts[si + 2] - base;
            for (int v = 0; v < vertexCount; v++) {
                if ((v & 3) != 0 && (v & 3) != 2) continue;
                for (int k = 0; k < 3; k++) {
                    int got = builder.getInt(out + v * stride + k * 4);
                    if (swap) got = Integer.reverseBytes(got);
                    int want = scratch[v * EntityVertexPacking.WORDS_PER_VERTEX + k];
                    if (got != want) {
                        return "sort position segment " + s + " vertex " + v + " axis " + k + ": 0x"
                                + Integer.toHexString(got) + " reference 0x" + Integer.toHexString(want);
                    }
                }
            }
        }
        return null;
    }

    private float f(int record, int k) {
        return Float.intBitsToFloat(words[record + InstanceRecord.POSE + k]);
    }

    private static void put(ByteBuffer buffer, int at, int value, boolean swap) {
        buffer.putInt(at, swap ? Integer.reverseBytes(value) : value);
    }

    void reset() {
        // Pooled batches keep their arrays (item and model batches refill thousands of records every frame);
        // only an extreme one is dropped.
        if (count > 1 << 16) {
            words = new int[InstanceRecord.WORDS * 16];
            meshes = new ModelMesh[16];
        } else {
            Arrays.fill(meshes, 0, count, null);
        }
        if (segments > 1 << 16) {
            segMesh = new ItemMesh[16];
            segInts = new int[16 * 4];
        } else {
            Arrays.fill(segMesh, 0, segments, null);
        }
        segments = 0;
        items = false;
        count = 0;
        vertices = 0;
        iris = false;
        // Pooled batches keep their particle array: heavy particle scenes refill it every frame.
        particleCount = 0;
        stateIndex = -1;
        base = 0;
    }
}
