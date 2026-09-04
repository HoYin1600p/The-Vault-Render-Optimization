package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.mixin.flw.vertex;

import com.jozufozu.flywheel.core.model.ShadeSeparatingVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.coderbot.iris.vertices.BlockSensitiveBufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.FlywheelShaderCompatState;

@Mixin(ShadeSeparatingVertexConsumer.class)
public class MixinShadeSeparatingVertexConsumer implements BlockSensitiveBufferBuilder {

    // Mixin this class to implement BlockSensitiveBufferBuilder interface
    // which allows us to pass the block id and render type to the buffer builder.

    @Shadow(remap = false)
    protected VertexConsumer shadedConsumer;

    @Shadow(remap = false)
    protected VertexConsumer unshadedConsumer;

    @Override
    public void beginBlock(short blockId, short renderType, int blockX, int blockY, int blockZ) {
        if (!FlywheelShaderCompatState.isRenderPathActive()) {
            return;
        }
        if (shadedConsumer instanceof BlockSensitiveBufferBuilder sensitiveBufferBuilder) {
            sensitiveBufferBuilder.beginBlock(blockId, renderType, blockX, blockY, blockZ);
        }
        if (unshadedConsumer instanceof BlockSensitiveBufferBuilder sensitiveBufferBuilder) {
            sensitiveBufferBuilder.beginBlock(blockId, renderType, blockX, blockY, blockZ);
        }
    }

    @Override
    public void endBlock() {
        if (!FlywheelShaderCompatState.isRenderPathActive()) {
            return;
        }
        if (shadedConsumer instanceof BlockSensitiveBufferBuilder sensitiveBufferBuilder) {
            sensitiveBufferBuilder.endBlock();
        }
        if (unshadedConsumer instanceof BlockSensitiveBufferBuilder sensitiveBufferBuilder) {
            sensitiveBufferBuilder.endBlock();
        }
    }
}
