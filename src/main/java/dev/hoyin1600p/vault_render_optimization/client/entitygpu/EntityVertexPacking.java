package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import net.minecraft.util.Mth;

/**
 * {@code BufferBuilder}'s exact byte rules for one {@code DefaultVertexFormat.NEW_ENTITY} vertex
 * (36 bytes, nine little-endian 32-bit words): position floats, RGBA bytes, UV floats, overlay and
 * light as two shorts each, three normal bytes and a padding byte (written as 0; not an attribute).
 */
public final class EntityVertexPacking {
    public static final int WORDS_PER_VERTEX = 9;

    private EntityVertexPacking() {
    }

    /** {@code (byte)(int)(c * 255F)} per channel, as {@code BufferBuilder.vertex} writes them. */
    public static int color(float red, float green, float blue, float alpha) {
        return (colorByte(red)) | (colorByte(green) << 8) | (colorByte(blue) << 16) | (colorByte(alpha) << 24);
    }

    static int colorByte(float channel) {
        return ((int) (channel * 255.0F)) & 255;
    }

    /** Overlay or light: the low and high shorts in order, which is the packed int itself. */
    public static int shorts(int packed) {
        return (packed & 0xFFFF) | ((packed >> 16 & 0xFFFF) << 16);
    }

    /** {@code BufferVertexConsumer.normalIntValue}: clamp to [-1, 1], scale by 127, truncate. */
    public static int normalByte(float value) {
        return (int) (Mth.clamp(value, -1.0F, 1.0F) * 127.0F) & 255;
    }

    public static int normal(float x, float y, float z) {
        return normalByte(x) | (normalByte(y) << 8) | (normalByte(z) << 16);
    }
}
