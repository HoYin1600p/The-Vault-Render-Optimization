package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleBillboardGeometry;

/**
 * Java reference for {@code particle_expand.comp} and the oracle of its self-test: the four vertices VRO's
 * billboard writers produce for one particle record, in their order ((maxU, maxV), (maxU, minV), (minU, minV),
 * (minU, maxV)), as {@code DefaultVertexFormat.PARTICLE} words.
 */
public final class ParticleReference {
    private ParticleReference() {
    }

    /** @param camera the batch's {@code {leftX, leftY, leftZ, upX, upY, upZ}} */
    public static void expand(int[] words, int record, float[] camera, int[] out, int outOffset) {
        ParticleBillboardGeometry g = ParticleBillboardGeometry.computeRolled(
                camera[0], camera[1], camera[2], camera[3], camera[4], camera[5],
                words[record + ParticleRecord.FLAGS] != 0, f(words, record + ParticleRecord.SIN),
                f(words, record + ParticleRecord.COS), f(words, record + ParticleRecord.SIZE),
                f(words, record + ParticleRecord.X), f(words, record + ParticleRecord.Y), f(words, record + ParticleRecord.Z));
        int minU = words[record + ParticleRecord.MIN_U], maxU = words[record + ParticleRecord.MAX_U];
        int minV = words[record + ParticleRecord.MIN_V], maxV = words[record + ParticleRecord.MAX_V];
        int color = words[record + ParticleRecord.COLOR], light = words[record + ParticleRecord.LIGHT];
        int o = outOffset;
        o = vertex(out, o, g.x0(), g.y0(), g.z0(), maxU, maxV, color, light);
        o = vertex(out, o, g.x1(), g.y1(), g.z1(), maxU, minV, color, light);
        o = vertex(out, o, g.x2(), g.y2(), g.z2(), minU, minV, color, light);
        vertex(out, o, g.x3(), g.y3(), g.z3(), minU, maxV, color, light);
    }

    private static float f(int[] words, int index) {
        return Float.intBitsToFloat(words[index]);
    }

    private static int vertex(int[] out, int o, float x, float y, float z, int u, int v, int color, int light) {
        out[o] = Float.floatToRawIntBits(x);
        out[o + 1] = Float.floatToRawIntBits(y);
        out[o + 2] = Float.floatToRawIntBits(z);
        out[o + 3] = u;
        out[o + 4] = v;
        out[o + 5] = color;
        out[o + 6] = light;
        return o + ParticleRecord.VERTEX_WORDS;
    }
}
