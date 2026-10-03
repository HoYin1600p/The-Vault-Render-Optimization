package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

/**
 * One reserved particle quad: 14 words, laid out as {@code particle_expand.comp} reads them. The inputs are
 * exactly those {@code ParticleBillboardGeometry.computeRolled} takes (camera-relative position, size, the roll's
 * {@code Mth.sin}/{@code Mth.cos}) plus the four UV bounds and the packed colour and light words the vertex
 * writers store. The camera's left and up vectors are per batch. While a record waits on the CPU,
 * {@link #OUTPUT} holds the absolute byte offset of its hole in the {@code BufferBuilder}.
 */
public final class ParticleRecord {
    public static final int WORDS = 14;
    public static final int X = 0;
    public static final int Y = 1;
    public static final int Z = 2;
    public static final int SIZE = 3;
    public static final int SIN = 4;
    public static final int COS = 5;
    /** 1 when the particle is rolled (the rotated basis is used). */
    public static final int FLAGS = 6;
    public static final int MIN_U = 7;
    public static final int MAX_U = 8;
    public static final int MIN_V = 9;
    public static final int MAX_V = 10;
    public static final int COLOR = 11;
    public static final int LIGHT = 12;
    public static final int OUTPUT = 13;
    /** {@code DefaultVertexFormat.PARTICLE}: position (3 floats), uv (2 floats), colour (4 bytes), light (2 shorts). */
    public static final int VERTEX_WORDS = 7;
    public static final int VERTICES = 4;

    private ParticleRecord() {
    }

    public static void write(int[] words, int offset, float x, float y, float z, float size, boolean rolled, float sin,
                             float cos, float minU, float maxU, float minV, float maxV, int color, int light, int output) {
        words[offset + X] = Float.floatToRawIntBits(x);
        words[offset + Y] = Float.floatToRawIntBits(y);
        words[offset + Z] = Float.floatToRawIntBits(z);
        words[offset + SIZE] = Float.floatToRawIntBits(size);
        words[offset + SIN] = Float.floatToRawIntBits(sin);
        words[offset + COS] = Float.floatToRawIntBits(cos);
        words[offset + FLAGS] = rolled ? 1 : 0;
        words[offset + MIN_U] = Float.floatToRawIntBits(minU);
        words[offset + MAX_U] = Float.floatToRawIntBits(maxU);
        words[offset + MIN_V] = Float.floatToRawIntBits(minV);
        words[offset + MAX_V] = Float.floatToRawIntBits(maxV);
        words[offset + COLOR] = color;
        words[offset + LIGHT] = light;
        words[offset + OUTPUT] = output;
    }
}
