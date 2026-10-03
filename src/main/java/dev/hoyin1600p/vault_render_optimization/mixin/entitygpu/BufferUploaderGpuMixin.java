package dev.hoyin1600p.vault_render_optimization.mixin.entitygpu;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuEntityModels;
import java.nio.ByteBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The one place reserved entity vertices are written on the GPU: {@code _end}'s vertex upload is
 * replaced by one that copies only the CPU-written ranges, then the GPU fills the reserved ranges,
 * before {@code _end} binds the shader and draws. Any upload without reservations is vanilla's call.
 * Hooks use {@code require = 0}; the mixin audit verifies they are present before the path can turn on.
 */
@Mixin(BufferUploader.class)
public abstract class BufferUploaderGpuMixin {
    @Inject(method = "end", at = @At("HEAD"), require = 0)
    private static void vro$armUpload(BufferBuilder builder, CallbackInfo ci) {
        GpuEntityModels.armUpload();
    }

    @Inject(method = "end", at = @At("RETURN"), require = 0)
    private static void vro$disarmUpload(BufferBuilder builder, CallbackInfo ci) {
        GpuEntityModels.disarmUpload();
    }

    @Redirect(method = "_end", require = 0, at = @At(value = "INVOKE", ordinal = 0,
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glBufferData(ILjava/nio/ByteBuffer;I)V"))
    private static void vro$uploadVertices(int target, ByteBuffer data, int usage, ByteBuffer buffer,
                                           VertexFormat.Mode mode, VertexFormat format, int vertexCount,
                                           VertexFormat.IndexType indexType, int indexCount, boolean sequentialIndex) {
        GpuEntityModels.uploadVertices(target, data, usage, format);
    }
}
