package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Vector3f;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import sun.misc.Unsafe;

/** Block entities: parts drawn through vanilla's SpriteCoordinateExpander. */
class GpuEntitySpriteTest {
    private static TextureAtlasSprite sprite(float u0, float u1, float v0, float v1) throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        TextureAtlasSprite sprite = (TextureAtlasSprite) unsafe.allocateInstance(TextureAtlasSprite.class);
        for (Object[] field : new Object[][]{{"u0", u0}, {"u1", u1}, {"v0", v0}, {"v1", v1}}) {
            Field f = TextureAtlasSprite.class.getDeclaredField((String) field[0]);
            unsafe.putFloat(sprite, unsafe.objectFieldOffset(f), (Float) field[1]);
        }
        return sprite;
    }

    @Test
    void cpuFillWithSpriteRemapReproducesVanillaBytes() throws Exception {
        Random random = new Random(2024);
        BufferBuilder builder = new BufferBuilder(1 << 16);
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
        HoleBatch batch = new HoleBatch();
        int offset = 0;
        for (int i = 0; i < 80; i++) {
            float[] s = GpuEntitySelfTest.randomSprite(random);
            TextureAtlasSprite atlasSprite = sprite(s[0], s[0] + s[1], s[2], s[2] + s[3]);
            List<ModelPart.Cube> cubes = new ArrayList<>();
            cubes.add(new ModelPart.Cube(random.nextInt(64), random.nextInt(64), -7, -3, -7, 14, 10, 14,
                    random.nextInt(3) == 0 ? random.nextFloat() : 0, 0, 0, random.nextBoolean(), 64, 64));
            ModelPart part = new ModelPart(cubes, Map.of());
            part.xRot = random.nextFloat() * 3;
            part.y = random.nextFloat() * 9;
            PoseStack stack = new PoseStack();
            stack.translate(random.nextFloat() * 10, 2, -3);
            stack.mulPose(Vector3f.YP.rotationDegrees(random.nextFloat() * 360));
            int light = random.nextInt(0x00F000F1), overlay = random.nextInt(0x000F000F);
            // Vanilla: the chest/bed/sign path wraps the entity buffer in a SpriteCoordinateExpander.
            part.render(stack, new SpriteCoordinateExpander(builder, atlasSprite), light, overlay, 1, 1, 1, 1);

            stack.pushPose();
            part.translateAndRotate(stack);
            FloatBuffer pose = FloatBuffer.allocate(16);
            FloatBuffer normal = FloatBuffer.allocate(9);
            stack.last().pose().store(pose);
            stack.last().normal().store(normal);
            ModelMesh mesh = ModelMeshCapture.capture(part);
            // The same floats GpuEntityModels.reserve takes from the sprite.
            float[] remap = {atlasSprite.getU0(), atlasSprite.getU1() - atlasSprite.getU0(),
                    atlasSprite.getV0(), atlasSprite.getV1() - atlasSprite.getV0()};
            batch.add(mesh, 0, pose.array(), normal.array(), offset, EntityVertexPacking.color(1, 1, 1, 1), overlay,
                    light, remap);
            offset += mesh.vertexCount() * 36;
        }
        builder.end();
        Pair<BufferBuilder.DrawState, ByteBuffer> popped = builder.popNextBuffer();
        ByteBuffer vanilla = popped.getSecond();
        ByteBuffer filled = ByteBuffer.allocateDirect(vanilla.remaining()).order(ByteOrder.nativeOrder());
        batch.setBase(0);
        batch.fillOnCpu(filled);
        for (int i = 0; i < filled.capacity(); i++) {
            if (i % 36 == 35) continue; // padding byte
            assertEquals(vanilla.get(i), filled.get(i), "byte " + i);
        }
    }

    /** Embeddium writes u0 + (u1 - u0) * u; vanilla u0 + ((u1 - u0) * (u * 16)) / 16. Same float. */
    @Test
    void embeddiumAndVanillaSpriteFormulasAgree() {
        Random random = new Random(7);
        for (int i = 0; i < 2_000_000; i++) {
            float[] s = GpuEntitySelfTest.randomSprite(random);
            float u = random.nextFloat();
            float vanilla = s[0] + s[1] * (float) (double) (u * 16.0F) / 16.0F;
            float embeddium = s[1] * u + s[0];
            assertEquals(Float.floatToRawIntBits(vanilla), Float.floatToRawIntBits(embeddium), "u=" + u);
        }
    }

    @Test
    void spriteAuditTargetsResolveAgainstVanilla() throws IOException {
        ClassNode node = new ClassNode();
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream(GpuEntityAudit.SPRITE_EXPANDER + ".class")) {
            assertNotNull(in);
            new ClassReader(in.readAllBytes()).accept(node, 0);
        }
        assertNull(GpuEntityAudit.skippedBodiesUntouched(node,
                List.of(GpuEntityAudit.SPRITE_VERTEX, GpuEntityAudit.SPRITE_UV)));
    }
}
