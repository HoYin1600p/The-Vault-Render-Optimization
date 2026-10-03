package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Vec3i;

/**
 * Reads baked quads exactly as the item quad writers consume them. The normal input floats of every vertex
 * depend only on the quad data and the writer, so they are resolved here:
 * <ul>
 * <li>{@link ItemWriter#FORGE}: starts as the face direction normal; a vertex whose baked normal bytes are not
 * all zero uses {@code byte / 127F}, otherwise it keeps the previous vertex's normal.</li>
 * <li>{@link ItemWriter#EMBEDDIUM}: the baked normal, or the packed face direction ({@code +-127}) when its low
 * 24 bits are zero, unpacked with {@code (byte) * 0.007874016F}.</li>
 * </ul>
 */
public final class ItemMeshCapture {
    private static final int INTS_PER_QUAD = 32;
    private static final int INTS_PER_VERTEX = 8;
    private static final float NORM3B = 0.007874016F;

    private ItemMeshCapture() {
    }

    /** @return the mesh, or null when a quad does not have exactly 32 ints (it then stays on the CPU path) */
    public static ItemMesh capture(List<BakedQuad> quads, ItemWriter writer) {
        if (quads == null || writer == null) return null;
        for (BakedQuad quad : quads) {
            if (quad == null) return null;
            int[] data = quad.getVertices();
            if (data == null || data.length != INTS_PER_QUAD || quad.getDirection() == null) return null;
        }
        if (quads.isEmpty()) return ItemMesh.EMPTY;
        int[] words = new int[quads.size() * 4 * ItemMesh.WORDS_PER_VERTEX];
        int[] tint = new int[quads.size()];
        int o = 0;
        int q = 0;
        for (BakedQuad quad : quads) {
            int[] data = quad.getVertices();
            Vec3i face = quad.getDirection().getNormal();
            float px = face.getX(), py = face.getY(), pz = face.getZ();
            int facePacked = ((face.getX() * 127) & 255) | (((face.getY() * 127) & 255) << 8)
                    | (((face.getZ() * 127) & 255) << 16);
            for (int v = 0; v < 4; v++) {
                int d = v * INTS_PER_VERTEX;
                float nx, ny, nz;
                int baked = data[d + 7];
                if (writer == ItemWriter.FORGE) {
                    byte bx = (byte) baked, by = (byte) (baked >> 8), bz = (byte) (baked >> 16);
                    if (bx != 0 || by != 0 || bz != 0) {
                        px = bx / 127.0F;
                        py = by / 127.0F;
                        pz = bz / 127.0F;
                    }
                    nx = px;
                    ny = py;
                    nz = pz;
                } else {
                    int packed = (baked & 0xFFFFFF) == 0 ? facePacked : baked;
                    nx = (byte) packed * NORM3B;
                    ny = (byte) (packed >> 8) * NORM3B;
                    nz = (byte) (packed >> 16) * NORM3B;
                }
                words[o++] = data[d];
                words[o++] = data[d + 1];
                words[o++] = data[d + 2];
                words[o++] = data[d + 4];
                words[o++] = data[d + 5];
                words[o++] = Float.floatToRawIntBits(nx);
                words[o++] = Float.floatToRawIntBits(ny);
                words[o++] = Float.floatToRawIntBits(nz);
                words[o++] = data[d + 3];
                words[o++] = data[d + 6];
            }
            tint[q++] = quad.isTinted() ? quad.getTintIndex() : -1;
        }
        return new ItemMesh(words, quads.size() * 4, tint);
    }
}
