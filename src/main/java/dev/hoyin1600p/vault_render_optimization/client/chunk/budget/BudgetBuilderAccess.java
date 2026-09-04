package dev.hoyin1600p.vault_render_optimization.client.chunk.budget;

import java.util.Queue;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;

public interface BudgetBuilderAccess {
    Queue<ChunkBuildResult> vro$pendingResults();
    int vro$workerCount();
}
