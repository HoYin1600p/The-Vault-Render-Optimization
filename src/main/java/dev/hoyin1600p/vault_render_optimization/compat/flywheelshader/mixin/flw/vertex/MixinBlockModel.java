package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.mixin.flw.vertex;

import com.jozufozu.flywheel.core.model.BlockModel;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.jozufozu.flywheel.core.vertex.BlockVertex;
import com.jozufozu.flywheel.core.vertex.BlockVertexListUnsafe;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.vertex.BlockModelReaders;

@Mixin(value = BlockModel.class)
public class MixinBlockModel {
    @Unique private VertexFormat vro$inputFormat;
    @Redirect(method = "<init>*", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferBuilder$DrawState;format()Lcom/mojang/blaze3d/vertex/VertexFormat;"))
    private VertexFormat irisFlw$ReturnIrisFormat(BufferBuilder.DrawState drawState) {
        vro$inputFormat = drawState.format();
        return BlockModelReaders.acceptedFormat(vro$inputFormat);
    }

    @Redirect(method = "<init>*", at = @At(value = "INVOKE",
            target = "Lcom/jozufozu/flywheel/core/vertex/BlockVertex;createReader(Ljava/nio/ByteBuffer;I)Lcom/jozufozu/flywheel/core/vertex/BlockVertexListUnsafe;",
            remap = false), require = 1, allow = 1)
    private BlockVertexListUnsafe vro$readActualFormat(BlockVertex type, ByteBuffer data, int count) {
        return BlockModelReaders.read(vro$inputFormat, data, count);
    }

    @Redirect(method = "<init>*", at = @At(value = "INVOKE",
            target = "Lcom/jozufozu/flywheel/core/vertex/BlockVertex;createReader(Ljava/nio/ByteBuffer;II)Lcom/jozufozu/flywheel/core/vertex/BlockVertexListUnsafe$Shaded;",
            remap = false), require = 1, allow = 1)
    private BlockVertexListUnsafe.Shaded vro$readActualShadedFormat(BlockVertex type, ByteBuffer data, int count, int unshaded) {
        return BlockModelReaders.read(vro$inputFormat, data, count, unshaded);
    }
}
