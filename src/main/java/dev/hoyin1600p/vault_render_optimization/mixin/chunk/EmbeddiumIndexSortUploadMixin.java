package dev.hoyin1600p.vault_render_optimization.mixin.chunk;

import dev.hoyin1600p.vault_render_optimization.client.chunk.sorting.IndexOnlySortResult;
import dev.hoyin1600p.vault_render_optimization.client.chunk.sorting.IndexOnlyUploads;
import java.util.List;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(value = RenderRegionManager.class, remap = false)
public abstract class EmbeddiumIndexSortUploadMixin {
    @Invoker("upload")
    public abstract void vro$uploadNativeBatch(CommandList commands, RenderRegion region, List<ChunkBuildResult> results);

    @Redirect(method = "upload(Lme/jellysquid/mods/sodium/client/gl/device/CommandList;Ljava/util/Iterator;)V",
            at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/region/RenderRegionManager;upload(Lme/jellysquid/mods/sodium/client/gl/device/CommandList;Lme/jellysquid/mods/sodium/client/render/chunk/region/RenderRegion;Ljava/util/List;)V"),
            require = 1, allow = 1)
    private void vro$uploadSortIndices(RenderRegionManager manager, CommandList commands,
            RenderRegion region, List<ChunkBuildResult> results) {
        // Always consume jobs already in flight, even if the runtime toggle is now off.
        IndexOnlyUploads.upload(commands, region, results, batch -> vro$uploadNativeBatch(commands, region, batch));
    }

    @Redirect(method = "upload(Lme/jellysquid/mods/sodium/client/gl/device/CommandList;Ljava/util/Iterator;)V",
            at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/RenderSection;onBuildFinished(Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildResult;)V"),
            require = 1, allow = 1)
    private void vro$finishOnlyAcceptedSort(RenderSection section, ChunkBuildResult result) {
        if (!(result instanceof IndexOnlySortResult indices) || indices.applied) section.onBuildFinished(result);
    }
}
