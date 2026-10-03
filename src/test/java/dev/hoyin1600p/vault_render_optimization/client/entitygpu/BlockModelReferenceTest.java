package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Vector3f;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.EmptyModelData;
import org.junit.jupiter.api.Test;

/**
 * The real Forge {@code ModelBlockRenderer.renderModel} into a real {@code BufferBuilder} is the oracle for the FORGE
 * block-model mesh (baked colour forced white, baked light kept) expanded by {@link ItemReference} with the
 * per-quad tint word {@code tinted ? clamp(r, g, b) : white}.
 */
class BlockModelReferenceTest {
    @Test
    void forgeRenderModelMatchesReference() {
        Random random = new Random(77);
        ModelBlockRenderer renderer = new ModelBlockRenderer(new BlockColors());
        for (int trial = 0; trial < 3000; trial++) {
            List<List<BakedQuad>> sides = new ArrayList<>();
            List<BakedQuad> all = new ArrayList<>();
            for (int s = 0; s < 7; s++) {
                List<BakedQuad> list = new ArrayList<>();
                int n = random.nextInt(3);
                for (int q = 0; q < n; q++) list.add(quad(random));
                sides.add(list);
                all.addAll(list);
            }
            BakedModel model = model(sides);
            PoseStack stack = new PoseStack();
            stack.translate(random.nextFloat() * 8 - 4, random.nextFloat() * 8 - 4, random.nextFloat() * 8 - 4);
            stack.mulPose(Vector3f.YP.rotationDegrees(random.nextFloat() * 360));
            stack.mulPose(Vector3f.XP.rotationDegrees(random.nextFloat() * 360));
            if (random.nextBoolean()) stack.scale(random.nextFloat() + 0.25F, random.nextFloat() + 0.25F, random.nextFloat() + 0.25F);
            float r = random.nextFloat() * 1.4F - 0.2F, g = random.nextFloat() * 1.4F - 0.2F, b = random.nextFloat() * 1.4F - 0.2F;
            int light = random.nextInt(4) == 0 ? 0xF000F0 : (random.nextInt(16) << 4) | (random.nextInt(16) << 20);
            int overlay = random.nextInt(0x000F000F);

            BufferBuilder builder = new BufferBuilder(1 << 12);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
            renderer.renderModel(stack.last(), builder, null, model, r, g, b, light, overlay, EmptyModelData.INSTANCE);
            builder.end();
            ByteBuffer bytes = builder.popNextBuffer().getSecond().order(ByteOrder.LITTLE_ENDIAN);

            ItemMesh mesh = ItemMeshCapture.capture(all, ItemWriter.FORGE);
            for (int v = 0; v < mesh.vertexCount(); v++) mesh.words()[v * ItemMesh.WORDS_PER_VERTEX + 8] = -1;
            float[] pose = new float[16];
            float[] normal = new float[9];
            stack.last().pose().store(FloatBuffer.wrap(pose));
            stack.last().normal().store(FloatBuffer.wrap(normal));
            int tinted = (int) (Math.max(0, Math.min(1, r)) * 255.0F) | (int) (Math.max(0, Math.min(1, g)) * 255.0F) << 8
                    | (int) (Math.max(0, Math.min(1, b)) * 255.0F) << 16 | 0xFF000000;
            int[] expected = new int[mesh.vertexCount() * EntityVertexPacking.WORDS_PER_VERTEX];
            for (int q = 0; q < all.size(); q++) {
                ItemReference.expand(mesh, q * 4, 4, pose, normal, all.get(q).isTinted() ? tinted : -1, overlay, light,
                        false, expected, q * 4 * EntityVertexPacking.WORDS_PER_VERTEX);
            }
            assertEquals(expected.length * 4, bytes.remaining(), "trial " + trial);
            for (int w = 0; w < expected.length; w++) {
                int mask = w % EntityVertexPacking.WORDS_PER_VERTEX == 8 ? 0x00FFFFFF : -1;   // byte 35: padding
                assertEquals(expected[w] & mask, bytes.getInt(w * 4) & mask, "trial " + trial + " word " + w);
            }
        }
    }

    private static BakedQuad quad(Random random) {
        Direction direction = Direction.values()[random.nextInt(6)];
        int[] data = new int[32];
        boolean bakedLight = random.nextInt(3) == 0;
        int normalStyle = random.nextInt(3);
        for (int v = 0; v < 4; v++) {
            int d = v * 8;
            for (int k = 0; k < 3; k++) data[d + k] = Float.floatToRawIntBits(random.nextFloat() * 1.2F - 0.1F);
            data[d + 3] = random.nextInt();                       // baked colour: ignored by renderModel
            data[d + 4] = Float.floatToRawIntBits(random.nextFloat());
            data[d + 5] = Float.floatToRawIntBits(random.nextFloat());
            data[d + 6] = bakedLight ? (random.nextInt(16) << 4) | (random.nextInt(16) << 20) : 0;
            data[d + 7] = switch (normalStyle) {
                case 0 -> 0;
                case 1 -> (direction.getStepX() * 127 & 255) | (direction.getStepY() * 127 & 255) << 8
                        | (direction.getStepZ() * 127 & 255) << 16;
                default -> v == 0 || random.nextBoolean() ? random.nextInt() & 0xFFFFFF : 0;
            };
        }
        return new BakedQuad(data, random.nextBoolean() ? 0 : -1, direction, null, true);
    }

    private static BakedModel model(List<List<BakedQuad>> sides) {
        return new BakedModel() {
            @Override
            public List<BakedQuad> getQuads(BlockState state, Direction side, Random random) {
                return sides.get(side == null ? 6 : side.ordinal());
            }

            @Override public boolean useAmbientOcclusion() { return false; }
            @Override public boolean isGui3d() { return false; }
            @Override public boolean usesBlockLight() { return false; }
            @Override public boolean isCustomRenderer() { return false; }
            @Override public TextureAtlasSprite getParticleIcon() { return null; }
            @Override public ItemTransforms getTransforms() { return ItemTransforms.NO_TRANSFORMS; }
            @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
        };
    }
}
