package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

/**
 * One reserved model part: 38 words, laid out as {@code model_instance_expand.comp} reads them. The
 * pose and normal matrices are stored as float bits in {@code Matrix4f.store}/{@code Matrix3f.store}
 * order. While a record waits on the CPU, {@link #OUTPUT} holds the absolute byte offset of its hole
 * in the {@code BufferBuilder}; it becomes a word offset into the uploaded buffer when sent to the GPU.
 */
public final class InstanceRecord {
    public static final int WORDS = 38;
    public static final int POSE = 0;
    public static final int NORMAL = 16;
    public static final int MESH_FIRST = 25;
    public static final int VERTEX_COUNT = 26;
    public static final int OUTPUT = 27;
    public static final int COLOR = 28;
    public static final int OVERLAY = 29;
    public static final int LIGHT = 30;
    /**
     * Flags: {@link #FLAG_SPRITE} when the part is drawn through a {@code SpriteCoordinateExpander}
     * (block entities), and the GeckoLib flat-cube normal flips {@link #FLIP_X}, {@link #FLIP_Y},
     * {@link #FLIP_Z}: after the normal matrix, a negative component is negated.
     */
    public static final int SPRITE = 31;
    public static final int FLAG_SPRITE = 1;
    public static final int FLIP_X = 2;
    public static final int FLIP_Y = 4;
    public static final int FLIP_Z = 8;
    public static final int FLIPS = FLIP_X | FLIP_Y | FLIP_Z;
    /** Item records only: the vertices follow Embeddium's renderQuadList writer (else Forge's putBulkData). */
    public static final int FLAG_ITEM_EMBEDDIUM = 16;
    public static final int SPRITE_U0 = 32;
    // Words 33 to 35 are unused.
    /** Oculus extended vertices only: {@link IrisEntityExtension#idsWord} and {@link IrisEntityExtension#itemWord}. */
    public static final int IRIS_IDS = 36;
    public static final int IRIS_ITEM = 37;

    private InstanceRecord() {
    }

    public static void write(int[] words, int offset, float[] pose, float[] normal, int meshFirst, int vertexCount,
                             int output, int color, int overlay, int light) {
        write(words, offset, pose, normal, meshFirst, vertexCount, output, color, overlay, light, null);
    }

    /**
     * @param sprite null, or {@code {u0, u1 - u0, v0, v1 - v0}} of the atlas sprite a
     *               {@code SpriteCoordinateExpander} remaps UVs into
     */
    public static void write(int[] words, int offset, float[] pose, float[] normal, int meshFirst, int vertexCount,
                             int output, int color, int overlay, int light, float[] sprite) {
        write(words, offset, pose, normal, meshFirst, vertexCount, output, color, overlay, light, sprite, 0);
    }

    /** @param flips {@link #FLIPS} bits (GeckoLib cubes), 0 for vanilla parts */
    public static void write(int[] words, int offset, float[] pose, float[] normal, int meshFirst, int vertexCount,
                             int output, int color, int overlay, int light, float[] sprite, int flips) {
        for (int i = 0; i < 16; i++) words[offset + POSE + i] = Float.floatToRawIntBits(pose[i]);
        for (int i = 0; i < 9; i++) words[offset + NORMAL + i] = Float.floatToRawIntBits(normal[i]);
        words[offset + MESH_FIRST] = meshFirst;
        words[offset + VERTEX_COUNT] = vertexCount;
        words[offset + OUTPUT] = output;
        words[offset + COLOR] = color;
        words[offset + OVERLAY] = EntityVertexPacking.shorts(overlay);
        words[offset + LIGHT] = EntityVertexPacking.shorts(light);
        words[offset + SPRITE] = (sprite == null ? 0 : FLAG_SPRITE) | (flips & FLIPS);
        for (int i = 0; i < 4; i++) {
            words[offset + SPRITE_U0 + i] = sprite == null ? 0 : Float.floatToRawIntBits(sprite[i]);
        }
        words[offset + IRIS_IDS] = 0;
        words[offset + IRIS_ITEM] = 0;
    }
}
