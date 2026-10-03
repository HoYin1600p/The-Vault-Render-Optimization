package dev.hoyin1600p.vault_render_optimization.client.render;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;
import net.minecraft.client.renderer.culling.Frustum;
import org.junit.jupiter.api.Test;

class FrustumPlanesTest {
    private static final Method vanilla;
    private static final Field planes;

    static {
        try {
            vanilla = Frustum.class.getDeclaredMethod("cubeInFrustum",
                    float.class, float.class, float.class, float.class, float.class, float.class);
            vanilla.setAccessible(true);
            planes = Frustum.class.getDeclaredField("frustumData");
            planes.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static boolean vanilla(Frustum frustum, float... box) throws Exception {
        return (Boolean) vanilla.invoke(frustum, box[0], box[1], box[2], box[3], box[4], box[5]);
    }

    private static Frustum frustum(Random random) {
        Matrix4f projection = Matrix4f.perspective(30 + random.nextFloat() * 90,
                0.5F + random.nextFloat() * 2, 0.05F, 16 + random.nextFloat() * 1000);
        Matrix4f view = new Matrix4f();
        view.setIdentity();
        view.multiply(Vector3f.XP.rotationDegrees(random.nextFloat() * 180 - 90));
        view.multiply(Vector3f.YP.rotationDegrees(random.nextFloat() * 360));
        view.multiply(Vector3f.ZP.rotationDegrees(random.nextInt(4) == 0 ? random.nextFloat() * 20 : 0));
        return new Frustum(view, projection);
    }

    @Test
    void matchesVanillaBitForBitOnRandomFrustaAndBoxes() throws Exception {
        Random random = new Random(1600);
        int visible = 0, hidden = 0;
        for (int f = 0; f < 300; f++) {
            Frustum frustum = frustum(random);
            Vector4f[] data = (Vector4f[]) planes.get(frustum) ;
            for (int b = 0; b < 2_000; b++) {
                float scale = random.nextInt(4) == 0 ? 2 : random.nextInt(3) == 0 ? 400 : 60;
                float x = (random.nextFloat() - 0.5F) * scale, y = (random.nextFloat() - 0.5F) * scale,
                        z = (random.nextFloat() - 0.5F) * scale;
                float size = random.nextInt(5) == 0 ? 0 : random.nextFloat() * (random.nextBoolean() ? 0.3F : 16);
                float[] box = {x, y, z, x + size, y + size * random.nextFloat(), z + size};
                boolean expected = vanilla(frustum, box);
                assertEquals(expected, FrustumPlanes.intersects(data, box[0], box[1], box[2], box[3], box[4], box[5]),
                        "frustum " + f + " box " + b);
                if (expected) visible++; else hidden++;
            }
        }
        assertTrue(visible > 10_000 && hidden > 10_000, "both outcomes exercised: " + visible + "/" + hidden);
    }

    @Test
    void boxesTouchingAPlaneExactlyAgreeWithVanilla() throws Exception {
        Random random = new Random(7);
        for (int f = 0; f < 200; f++) {
            Frustum frustum = frustum(random);
            Vector4f[] data = (Vector4f[]) planes.get(frustum);
            for (Vector4f plane : data) {
                // Points on the plane: dot == 0 exactly is the boundary vanilla treats as outside.
                for (int i = 0; i < 50; i++) {
                    float x = (random.nextFloat() - 0.5F) * 50, y = (random.nextFloat() - 0.5F) * 50;
                    float z = plane.z() == 0 ? 0 : -(plane.x() * x + plane.y() * y + plane.w()) / plane.z();
                    float[] box = {x, y, z, x, y, z};
                    assertEquals(vanilla(frustum, box), FrustumPlanes.intersects(data, x, y, z, x, y, z));
                }
            }
        }
    }

    /** Vanilla's double-to-float conversion followed by the plane test equals isVisible for world-space boxes. */
    @Test
    void worldSpaceBoxesMatchVanillaIsVisible() throws Exception {
        Field camX = Frustum.class.getDeclaredField("camX");
        Field camY = Frustum.class.getDeclaredField("camY");
        Field camZ = Frustum.class.getDeclaredField("camZ");
        camX.setAccessible(true);
        camY.setAccessible(true);
        camZ.setAccessible(true);
        Random random = new Random(99);
        int visible = 0;
        for (int f = 0; f < 200; f++) {
            Frustum frustum = frustum(random);
            double cx = random.nextDouble() * 60_000 - 30_000, cy = random.nextDouble() * 400 - 64, cz = random.nextDouble() * 60_000 - 30_000;
            frustum.prepare(cx, cy, cz);
            Vector4f[] data = (Vector4f[]) planes.get(frustum);
            for (int b = 0; b < 1_000; b++) {
                double x = cx + (random.nextDouble() - 0.5) * 120, y = cy + (random.nextDouble() - 0.5) * 120,
                        z = cz + (random.nextDouble() - 0.5) * 120, size = random.nextDouble() * (random.nextBoolean() ? 0.25 : 4);
                net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(x, y, z, x + size, y + size, z + size);
                double px = camX.getDouble(frustum), py = camY.getDouble(frustum), pz = camZ.getDouble(frustum);
                boolean fast = FrustumPlanes.intersects(data, (float) (box.minX - px), (float) (box.minY - py),
                        (float) (box.minZ - pz), (float) (box.maxX - px), (float) (box.maxY - py), (float) (box.maxZ - pz));
                boolean expected = frustum.isVisible(box);
                assertEquals(expected, fast, "frustum " + f + " box " + b);
                if (expected) visible++;
            }
        }
        assertTrue(visible > 5_000, "visible " + visible);
    }

    @Test
    void liveVerifyReferenceIsVanilla() throws Exception {
        Random random = new Random(42);
        for (int f = 0; f < 100; f++) {
            Frustum frustum = frustum(random);
            Vector4f[] data = (Vector4f[]) planes.get(frustum);
            for (int b = 0; b < 1_000; b++) {
                float x = (random.nextFloat() - 0.5F) * 200, y = (random.nextFloat() - 0.5F) * 200,
                        z = (random.nextFloat() - 0.5F) * 200, size = random.nextFloat() * 16;
                float[] box = {x, y, z, x + size, y + size, z + size};
                assertEquals(vanilla(frustum, box), FrustumPlanes.reference(data, box[0], box[1], box[2], box[3], box[4], box[5]));
            }
        }
    }

    @Test
    void nonFiniteInputsAreLeftToVanilla() {
        assertFalse(FrustumPlanes.finite(Float.NaN, 0, 0, 1, 1, 1));
        assertFalse(FrustumPlanes.finite(0, 0, 0, Float.POSITIVE_INFINITY, 1, 1));
        assertTrue(FrustumPlanes.finite(-1, -2, -3, 4, 5, 6));
    }
}
