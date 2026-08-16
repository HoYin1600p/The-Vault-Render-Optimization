package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.forge.mixin;

import com.jozufozu.flywheel.core.model.WorldModelBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.coderbot.iris.block_rendering.BlockRenderingSettings;
import net.coderbot.iris.vertices.BlockSensitiveBufferBuilder;
import net.coderbot.iris.vertices.ExtendedDataHelper;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.IModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.FlywheelShaderCompatState;

import java.util.Random;

@Mixin(value = WorldModelBuilder.class)
public class MixinWorldModelBuilder {

    @Unique
    private final Object2IntMap<BlockState> vroFlywheel$blockStateIds = vroFlywheel$getBlockStateIds();

    @Unique
    private Object2IntMap<BlockState> vroFlywheel$getBlockStateIds() {
        return BlockRenderingSettings.INSTANCE.getBlockStateIds();
    }

    @Unique
    private short vroFlywheel$resolveBlockId(BlockState state) {
        if (vroFlywheel$blockStateIds == null) {
            return -1;
        }

        return (short) vroFlywheel$blockStateIds.getOrDefault(state, -1);
    }
    @Redirect(method = "bufferInto(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/renderer/block/ModelBlockRenderer;Ljava/util/Random;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;" +
                    "tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;" +
                    "Lnet/minecraft/client/resources/model/BakedModel;" +
                    "Lnet/minecraft/world/level/block/state/BlockState;" +
                    "Lnet/minecraft/core/BlockPos;" +
                    "Lcom/mojang/blaze3d/vertex/PoseStack;" +
                    "Lcom/mojang/blaze3d/vertex/VertexConsumer;" +
                    "ZLjava/util/Random;" +
                    "JILnet/minecraftforge/client/model/data/IModelData;)Z"), require = 0, remap = false)
    public boolean vroFlywheel$bufferInto(ModelBlockRenderer instance, BlockAndTintGetter tintGetter, BakedModel model, BlockState state, BlockPos pos, PoseStack poseStack, VertexConsumer consumer, boolean checkSides, Random random, long seed, int packedOverlay, IModelData modelData) {

        // The WorldModelBuilder is used to buffer the Contraption's block models vertex data.

        // When the bufferInto method is called, we need to check if the VertexConsumer is an instance of BlockSensitiveBufferBuilder that added by Iris.
        // If it is, we need to call the beginBlock to pass the block id and render type to the buffer builder.
        // So the shader of flywheel can get the block id and render type.


        if(FlywheelShaderCompatState.isRenderPathActive() && consumer instanceof BlockSensitiveBufferBuilder blockSensitiveBufferBuilder) {
            var blockId = vroFlywheel$resolveBlockId(state);
            blockSensitiveBufferBuilder.beginBlock(blockId, ExtendedDataHelper.BLOCK_RENDER_TYPE, pos.getX(), pos.getY(), pos.getZ());
            var ret = instance.tesselateBlock(tintGetter, model, state, pos, poseStack, consumer, checkSides, random, seed, packedOverlay, modelData);
            blockSensitiveBufferBuilder.endBlock();
            return ret;
        }else{
            return instance.tesselateBlock(tintGetter, model, state, pos, poseStack, consumer, checkSides, random, seed, packedOverlay, modelData);
        }
    }
}
