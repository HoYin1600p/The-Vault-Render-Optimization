package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleBillboardGeometry;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import net.minecraft.util.Mth;
import org.junit.jupiter.api.Test;

/**
 * VRO's portable billboard writer (SingleQuadParticleMixin: compute, then vertex/uv/color/uv2/endVertex) is the
 * oracle: a particle record expanded by {@link ParticleReference} must give its bytes exactly, for random
 * cameras, positions, sizes, rolls, UVs, colours and lights.
 */
class ParticleReferenceTest {
    @Test
    void referenceMatchesTheBillboardWriterBitForBit() {
        Random random = new Random(1600);
        BufferBuilder builder = new BufferBuilder(1 << 14);
        int[] words = new int[ParticleRecord.WORDS];
        int[] expected = new int[ParticleRecord.VERTICES * ParticleRecord.VERTEX_WORDS];
        int rolledSeen = 0;
        for (int trial = 0; trial < 20_000; trial++) {
            Quaternion rotation = Vector3f.YP.rotationDegrees(-random.nextFloat() * 360.0F);
            rotation.mul(Vector3f.XP.rotationDegrees(random.nextFloat() * 180.0F - 90.0F));
            Vector3f left = new Vector3f(1.0F, 0.0F, 0.0F);
            left.transform(rotation);
            Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
            up.transform(rotation);
            float x = random.nextFloat() * 128 - 64, y = random.nextFloat() * 128 - 64, z = random.nextFloat() * 128 - 64;
            float size = random.nextInt(8) == 0 ? random.nextFloat() * 6 : random.nextFloat() * 0.4F;
            float angle = random.nextInt(3) == 0 ? random.nextFloat() * 12 - 6 : 0.0F;
            float minU = random.nextFloat(), maxU = random.nextFloat(), minV = random.nextFloat(), maxV = random.nextFloat();
            float r = random.nextFloat(), g = random.nextFloat(), b = random.nextFloat(), a = random.nextFloat();
            int light = random.nextInt(0x00F000F1);

            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            ParticleBillboardGeometry geometry = ParticleBillboardGeometry.compute(left.x(), left.y(), left.z(),
                    up.x(), up.y(), up.z(), angle, size, x, y, z);
            float[][] corners = {
                    {geometry.x0(), geometry.y0(), geometry.z0(), maxU, maxV},
                    {geometry.x1(), geometry.y1(), geometry.z1(), maxU, minV},
                    {geometry.x2(), geometry.y2(), geometry.z2(), minU, minV},
                    {geometry.x3(), geometry.y3(), geometry.z3(), minU, maxV}};
            for (float[] c : corners) {
                builder.vertex(c[0], c[1], c[2]).uv(c[3], c[4]).color(r, g, b, a).uv2(light).endVertex();
            }
            builder.end();
            ByteBuffer bytes = builder.popNextBuffer().getSecond().order(ByteOrder.LITTLE_ENDIAN);

            boolean rolled = angle != 0.0F;
            if (rolled) rolledSeen++;
            ParticleRecord.write(words, 0, x, y, z, size, rolled, rolled ? Mth.sin(angle) : 0.0F, rolled ? Mth.cos(angle) : 1.0F,
                    minU, maxU, minV, maxV, EntityVertexPacking.color(r, g, b, a), light, 0);
            ParticleReference.expand(words, 0, new float[]{left.x(), left.y(), left.z(), up.x(), up.y(), up.z()}, expected, 0);

            assertEquals(expected.length * 4, bytes.remaining(), "trial " + trial);
            for (int w = 0; w < expected.length; w++) {
                assertEquals(expected[w], bytes.getInt(w * 4), "trial " + trial + " word " + w);
            }
        }
        assertTrue(rolledSeen > 5_000, "rolled " + rolledSeen);
    }
}
