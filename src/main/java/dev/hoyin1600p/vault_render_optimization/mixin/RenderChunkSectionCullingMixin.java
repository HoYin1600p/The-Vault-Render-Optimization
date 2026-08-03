package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.render.SectionDistanceCulling;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkRenderDispatcher.RenderChunk.class)
public abstract class RenderChunkSectionCullingMixin {
    @Shadow
    public abstract BlockPos getOrigin();

    @Inject(method = "getCompiledChunk", at = @At("HEAD"), cancellable = true)
    private void vault_render_optimization$hideDistantSection(
            CallbackInfoReturnable<ChunkRenderDispatcher.CompiledChunk> callbackInfo) {
        BlockPos origin = this.getOrigin();
        if (SectionDistanceCulling.shouldCullActiveContext(
                origin.getX(), origin.getY(), origin.getZ())) {
            callbackInfo.setReturnValue(ChunkRenderDispatcher.CompiledChunk.UNCOMPILED);
        }
    }
}
