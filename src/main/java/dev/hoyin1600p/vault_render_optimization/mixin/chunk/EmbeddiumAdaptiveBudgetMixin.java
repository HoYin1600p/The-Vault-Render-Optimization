/* SPDX-License-Identifier: LGPL-3.0-only
 * Fresh VRO scheduling adapter; native lifecycle retained. See THIRD_PARTY_NOTICES.md.
 */
package dev.hoyin1600p.vault_render_optimization.mixin.chunk;

import dev.hoyin1600p.vault_render_optimization.client.chunk.ChunkUpdateState;
import dev.hoyin1600p.vault_render_optimization.client.chunk.budget.*;
import it.unimi.dsi.fastutil.PriorityQueue;
import java.util.Map;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkUpdateType;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderBuildTask;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class EmbeddiumAdaptiveBudgetMixin {
    @Shadow @Final private ChunkBuilder builder;
    @Shadow @Final private RenderRegionManager regions;
    @Shadow @Final private Map<ChunkUpdateType, PriorityQueue<RenderSection>> rebuildQueues;
    @Shadow private boolean alwaysDeferChunkUpdates;
    @Unique private AdaptiveChunkBudget vro$budget;
    @Unique private TerrainLoadingGuard vro$loading;
    @Unique private final BudgetResults vro$results = new BudgetResults();
    @Unique private boolean vro$budgetActive;
    @Unique private long vro$lastBudgetReport;
    @Unique private int vro$pendingAtStart;

    @Inject(method = "updateChunks", at = @At("HEAD"), require = 1)
    private void vro$beginBudget(CallbackInfo ci) {
        boolean enabled = AdaptiveBudgetState.enabled() && ChunkUpdateState.defer(alwaysDeferChunkUpdates);
        if (!enabled) {
            if (vro$budget != null) vro$budget.close();
            vro$budget = null;
            vro$loading = null;
            vro$results.clear();
            vro$budgetActive = false;
            AdaptiveBudgetState.observe("YIELDED: disabled/Compare Mode or native synchronous scheduling selected");
            return;
        }
        if (vro$budget == null) {
            vro$budget = new AdaptiveChunkBudget(Runtime.getRuntime().maxMemory());
            vro$loading = new TerrainLoadingGuard(vro$budget.memoryWatermark());
            vro$lastBudgetReport = System.nanoTime() - 500_000_000L;
        }
        vro$budgetActive = true;
        var access = (BudgetBuilderAccess) builder;
        long now = System.nanoTime();
        var pending = vro$results.inspect(access.vro$pendingResults(), now);
        vro$pendingAtStart = pending.count();
        boolean[] waiting = new boolean[5];
        int[] requestCounts = new int[5];
        for (ChunkUpdateType type : ChunkUpdateType.values()) {
            var queue = rebuildQueues.get(type);
            waiting[type.ordinal()] = queue != null && !queue.isEmpty();
            requestCounts[type.ordinal()] = queue == null ? 0 : queue.size();
        }
        int workers = access.vro$workerCount();
        int active = Math.max(0, workers - builder.getNumAvailableBuilders());
        vro$loading.begin(now, requestCounts, access.vro$queuedTaskCount(), active,
                pending.count(), pending.bytes(), pending.oldestWait(), pending.unbuiltTerrain());
        vro$budget.beginFrame(now, workers, active,
                builder.getSchedulingBudget(), pending.bytes(), pending.count(), pending.oldestWait(), waiting);
        // Text only, at most twice per second; no per-frame logging or retained renderer globals.
        if (now - vro$lastBudgetReport >= 500_000_000L) {
            AdaptiveBudgetState.observe(vro$loading.status() + "; " + vro$budget.snapshot());
            vro$lastBudgetReport = now;
        }
    }

    // Both stores are intentional: initial budget and vanilla's SORT minimum of one.
    // Limiting is idempotent and happens before any native queue dequeue.
    @ModifyVariable(method = "submitRebuildTasks", at = @At("STORE"), ordinal = 0, require = 2, allow = 2)
    private int vro$limitAdmissions(int budget, ChunkUpdateType type) {
        return vro$budgetActive && vro$loading.mayLimit(type.ordinal())
                ? vro$budget.limit(budget, type.ordinal(), ChunkUpdateType.isSort(type)) : budget;
    }

    @Redirect(method = "submitRebuildTasks", at = @At(value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuilder;scheduleDeferred(Lme/jellysquid/mods/sodium/client/render/chunk/tasks/ChunkRenderBuildTask;)Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuilder$WrappedTask;"), require = 1, allow = 1)
    private ChunkBuilder.WrappedTask vro$measureAdmittedTask(ChunkBuilder owner, ChunkRenderBuildTask task, ChunkUpdateType type) {
        if (!vro$budgetActive || type == ChunkUpdateType.INITIAL_BUILD) return owner.scheduleDeferred(task);
        boolean sort = ChunkUpdateType.isSort(type);
        var scheduled = owner.scheduleDeferred(new TimedChunkTask(task, vro$budget, sort));
        if (vro$loading.pacing()) vro$budget.admitted(type.ordinal(), sort);
        return scheduled;
    }

    @Inject(method = "performPendingUploads", at = @At("HEAD"), cancellable = true, require = 1)
    private void vro$spreadUploads(CallbackInfoReturnable<Boolean> ci) {
        if (!vro$budgetActive) return;
        var queue = ((BudgetBuilderAccess) builder).vro$pendingResults();
        if (BudgetResults.needsNativeDrain(queue)) vro$loading.protectReadyTerrain();
        if (!vro$loading.pacing()) {
            // Preserve native batch/order/ownership and flush ALL ready work, including zero-byte initial builds.
            // No capped iterator can strand initial geometry behind a cosmetic sort.
            vro$results.clear();
            return;
        }
        // A bounded count also prevents continuously completing producers from extending this frame forever.
        var drain = new BudgetedDrain<ChunkBuildResult>(queue, BudgetResults::bytes, vro$results::forget,
                vro$budget.uploadByteAllowance(), Math.max(1, vro$pendingAtStart));
        if (!drain.hasNext()) { ci.setReturnValue(false); return; }
        long start = System.nanoTime();
        try {
            regions.upload(RenderDevice.INSTANCE.createCommandList(), drain);
        } finally {
            vro$budget.observeUpload(System.nanoTime() - start, drain.bytes(), drain.count());
        }
        ci.setReturnValue(true);
    }

    @Inject(method = "destroy", at = @At("HEAD"), require = 1)
    private void vro$releaseBudget(CallbackInfo ci) {
        if (vro$budget != null) vro$budget.close();
        vro$budget = null;
        vro$loading = null;
        vro$budgetActive = false;
        vro$results.clear();
        AdaptiveBudgetState.observe("renderer closed; no active budget");
        // Native destroy still cancels workers and frees every unconsumed result.
    }
}
