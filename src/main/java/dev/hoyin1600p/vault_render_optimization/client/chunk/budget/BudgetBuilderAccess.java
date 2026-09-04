package dev.hoyin1600p.vault_render_optimization.client.chunk.budget;

import java.util.Queue;
import java.util.Deque;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;

public interface BudgetBuilderAccess {
    Queue<ChunkBuildResult> vro$pendingResults();
    int vro$workerCount();
    Deque<ChunkBuilder.WrappedTask> vro$queuedTasks();
    default int vro$queuedTaskCount() { return vro$queuedTasks().size(); }
}
