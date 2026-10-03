package dev.hoyin1600p.vault_render_optimization.client.hud;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/** Captures exact final vertices, including native shadow translation and colors. */
public final class HudGlyphMesh implements MultiBufferSource {
    private final List<Run> runs = new ArrayList<>();
    private int vertices;
    private boolean frozen;

    @Override public VertexConsumer getBuffer(RenderType type) {
        if (frozen) throw new IllegalStateException("Frozen HUD mesh");
        if (runs.isEmpty() || runs.get(runs.size() - 1).type != type) runs.add(new Run(type));
        return runs.get(runs.size() - 1);
    }

    public void freeze() {
        for (Run run : runs) run.data = Arrays.copyOf(run.data, run.size);
        frozen = true;
    }

    public int bytes() { return vertices * 11 * Float.BYTES; }

    public void render(MultiBufferSource buffers) {
        if (!frozen) throw new IllegalStateException("Unfinished HUD mesh");
        for (Run run : runs) {
            VertexConsumer output = buffers.getBuffer(run.type);
            for (int i = 0; i < run.size; i += 11) {
                output.vertex(run.data[i], run.data[i + 1], run.data[i + 2]);
                output.color((int) run.data[i + 3], (int) run.data[i + 4], (int) run.data[i + 5], (int) run.data[i + 6]);
                output.uv(run.data[i + 7], run.data[i + 8]);
                output.uv2((int) run.data[i + 9], (int) run.data[i + 10]);
                output.endVertex();
            }
        }
    }

    private final class Run implements VertexConsumer {
        private final RenderType type;
        private float[] data = new float[88];
        private final float[] pending = new float[11];
        private int size;
        private int attributes;
        Run(RenderType type) { this.type = type; }
        public VertexConsumer vertex(double x, double y, double z) {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) throw unsupported();
            pending[0] = (float) x; pending[1] = (float) y; pending[2] = (float) z; attributes |= 1; return this;
        }
        public VertexConsumer color(int r, int g, int b, int a) {
            pending[3] = r; pending[4] = g; pending[5] = b; pending[6] = a; attributes |= 2; return this;
        }
        public VertexConsumer uv(float u, float v) {
            pending[7] = u; pending[8] = v; attributes |= 4; return this;
        }
        public VertexConsumer uv2(int u, int v) {
            pending[9] = u; pending[10] = v; attributes |= 8; return this;
        }
        public VertexConsumer overlayCoords(int u, int v) { throw unsupported(); }
        public VertexConsumer normal(float x, float y, float z) { throw unsupported(); }
        public void defaultColor(int r, int g, int b, int a) { throw unsupported(); }
        public void unsetDefaultColor() { throw unsupported(); }
        public void endVertex() {
            if (attributes != 15 || ++vertices > 512) throw unsupported();
            if (size + 11 > data.length) data = Arrays.copyOf(data, data.length * 2);
            System.arraycopy(pending, 0, data, size, 11);
            size += 11;
            attributes = 0;
        }
    }

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Nonstandard HUD glyph output; use original renderer");
    }
}
