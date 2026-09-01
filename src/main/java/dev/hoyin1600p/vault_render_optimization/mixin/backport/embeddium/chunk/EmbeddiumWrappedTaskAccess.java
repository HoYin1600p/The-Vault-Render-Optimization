package dev.hoyin1600p.vault_render_optimization.mixin.backport.embeddium.chunk;

import java.util.concurrent.CompletableFuture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder$WrappedTask", remap = false)
public interface EmbeddiumWrappedTaskAccess {
    @Accessor("future")
    CompletableFuture<?> vro$getFuture();

    @Accessor("isCancelled")
    boolean vro$isCancelled();
}
