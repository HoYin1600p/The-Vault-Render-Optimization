package dev.hoyin1600p.vault_render_optimization.mixin.chunk;

import dev.hoyin1600p.vault_render_optimization.client.chunk.budget.BudgetBuilderAccess;
import java.util.Queue;
import java.util.Deque;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(value = ChunkBuilder.class, remap = false)
public interface EmbeddiumBudgetBuilderMixin extends BudgetBuilderAccess {
    @Override @Accessor("deferredResultQueue") Queue<ChunkBuildResult> vro$pendingResults();
    @Override @Accessor("limitThreads") int vro$workerCount();
    @Override @Accessor("buildQueue") Deque<ChunkBuilder.WrappedTask> vro$queuedTasks();
}
