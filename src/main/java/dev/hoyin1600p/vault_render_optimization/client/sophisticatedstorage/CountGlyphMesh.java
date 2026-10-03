package dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * VRO-owned local-space capture for static, plain white count glyphs only.
 * Retains consecutive atlas runs, never changes draw order or owns a GPU buffer.
 * Color, pose and light are supplied on replay through the original consumer.
 */
public final class CountGlyphMesh implements MultiBufferSource {
    private static final int MAX_VERTICES = 128;
    private final List<Run> runs = new ArrayList<>();
    private int vertices;
    private boolean frozen;

    @Override public VertexConsumer getBuffer(RenderType type) {
        if (frozen) throw new IllegalStateException("Count glyph mesh already frozen");
        if (runs.isEmpty() || runs.get(runs.size() - 1).type != type) runs.add(new Run(type));
        return runs.get(runs.size() - 1);
    }

    public void freeze() {
        for (Run run : runs) run.data = Arrays.copyOf(run.data, run.size);
        frozen = true;
    }

    public void render(Matrix4f pose, MultiBufferSource buffers, int color, int light) {
        if (!frozen) throw new IllegalStateException("Count glyph mesh not frozen");
        // Match Font.adjustColor, including color values without an explicit alpha.
        if ((color & 0xFC000000) == 0) color |= 0xFF000000;
        int r = (color >>> 16) & 255, g = (color >>> 8) & 255, b = color & 255, a = color >>> 24;
        for (Run run : runs) {
            VertexConsumer output = buffers.getBuffer(run.type);
            for (int i = 0; i < run.size; i += 5) {
                // Do not chain return values: sprite/shader wrappers may return their delegate.
                output.vertex(pose, run.data[i], run.data[i + 1], run.data[i + 2]);
                output.color(r, g, b, a);
                output.uv(run.data[i + 3], run.data[i + 4]);
                output.uv2(light);
                output.endVertex();
            }
        }
    }

    private final class Run implements VertexConsumer {
        private final RenderType type;
        private float[] data = new float[40];
        private int size;
        private float x, y, z, u, v;
        Run(RenderType type) { this.type = type; }
        public VertexConsumer vertex(double x, double y, double z) {
            this.x = (float) x; this.y = (float) y; this.z = (float) z; return this;
        }
        public VertexConsumer color(int r, int g, int b, int a) {
            if (r != 255 || g != 255 || b != 255 || a != 255) throw unsupported();
            return this;
        }
        public VertexConsumer uv(float u, float v) { this.u = u; this.v = v; return this; }
        public VertexConsumer uv2(int u, int v) { return this; }
        public VertexConsumer overlayCoords(int u, int v) { throw unsupported(); }
        public VertexConsumer normal(float x, float y, float z) { throw unsupported(); }
        public void defaultColor(int r, int g, int b, int a) { throw unsupported(); }
        public void unsetDefaultColor() { throw unsupported(); }
        public void endVertex() {
            if (++vertices > MAX_VERTICES) throw unsupported();
            if (size + 5 > data.length) data = Arrays.copyOf(data, data.length * 2);
            data[size++] = x; data[size++] = y; data[size++] = z;
            data[size++] = u; data[size++] = v;
        }
    }

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Nonstandard count glyph output; use original font renderer");
    }
}
