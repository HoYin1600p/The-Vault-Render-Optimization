package dev.hoyin1600p.vault_render_optimization.mixin.chunk;

import dev.hoyin1600p.vault_render_optimization.client.chunk.sorting.IndexOnlySortTask;
import dev.hoyin1600p.vault_render_optimization.client.chunk.sorting.IndexSortState;
import dev.hoyin1600p.vault_render_optimization.client.chunk.sorting.SortGeometryCache;
import dev.hoyin1600p.vault_render_optimization.client.chunk.sorting.TranslucentSortFootprint;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import java.util.function.Supplier;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBufferSorter;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderBuildTask;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class EmbeddiumIndexSortTaskMixin {
    @Shadow private float cameraX;
    @Shadow private float cameraY;
    @Shadow private float cameraZ;
    @Shadow private int currentFrame;
    @Shadow @Final private Long2ReferenceMap<RenderSection> sections;
    @Unique private final Supplier<TranslucentSortFootprint.Totals> vro$footprint = this::vro$measureFootprint;

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void vro$registerFootprint(CallbackInfo ci) {
        TranslucentSortFootprint.register(vro$footprint);
    }

    @Inject(method = "destroy", at = @At("RETURN"), require = 1)
    private void vro$releaseDecodedGeometry(CallbackInfo ci) {
        SortGeometryCache.clear();
        TranslucentSortFootprint.unregister(vro$footprint);
    }

    @Inject(method = "createSortTask", at = @At("HEAD"), cancellable = true, require = 1)
    private void vro$createIndexOnlySort(RenderSection section, CallbackInfoReturnable<ChunkRenderBuildTask> callback) {
        if (!IndexSortState.enabled()) return;
        ChunkRenderBuildTask task = IndexOnlySortTask.capture(section, currentFrame, cameraX, cameraY, cameraZ);
        if (task != null) callback.setReturnValue(task);
    }

    /** Client thread only (called from a client command); reads the section map the renderer owns. */
    @Unique
    private TranslucentSortFootprint.Totals vro$measureFootprint() {
        int sectionCount = 0, passes = 0;
        long vertexBytes = 0, indexBytes = 0;
        for (RenderSection section : sections.values()) {
            boolean any = false;
            for (BlockRenderPass pass : BlockRenderPass.VALUES) {
                if (!pass.isTranslucent()) continue;
                ChunkBufferSorter.SortBuffer data = section.getTranslucencyData(pass);
                if (data == null) continue;
                any = true;
                passes++;
                vertexBytes += data.vertexBuffer().capacity();
                indexBytes += data.indexBuffer().capacity();
            }
            if (any) sectionCount++;
        }
        return new TranslucentSortFootprint.Totals(sectionCount, passes, vertexBytes, indexBytes);
    }
}
