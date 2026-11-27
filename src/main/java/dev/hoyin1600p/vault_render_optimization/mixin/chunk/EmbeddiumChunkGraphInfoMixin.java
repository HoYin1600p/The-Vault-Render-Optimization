package dev.hoyin1600p.vault_render_optimization.mixin.chunk;

import dev.hoyin1600p.vault_render_optimization.client.render.AngleSectionOcclusion;
import me.jellysquid.mods.sodium.client.render.chunk.graph.ChunkGraphInfo;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ChunkGraphInfo.class, remap = false)
public abstract class EmbeddiumChunkGraphInfoMixin {
    @Shadow
    private long visibilityData;

    @Shadow
    public abstract int getOriginX();

    @Shadow
    public abstract int getOriginY();

    @Shadow
    public abstract int getOriginZ();

    @Redirect(
            method = "updateCullingState",
            at = @At(
                    value = "FIELD",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/graph/ChunkGraphInfo;visibilityData:J",
                    opcode = Opcodes.GETFIELD
            ),
            remap = false
    )
    private long vro$applyAngleVisibilityMask(ChunkGraphInfo instance) {
        return AngleSectionOcclusion.maskVisibility(
                this.visibilityData,
                this.getOriginX(),
                this.getOriginY(),
                this.getOriginZ()
        );
    }
}
