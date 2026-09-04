package dev.hoyin1600p.vault_render_optimization.client.chunk.budget;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import dev.hoyin1600p.vault_render_optimization.client.chunk.sorting.IndexOnlySortResult;
import java.util.ArrayDeque;
import java.util.Map;
import me.jellysquid.mods.sodium.client.gl.buffer.IndexedVertexData;
import me.jellysquid.mods.sodium.client.gl.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderBuildTask;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;
import org.junit.jupiter.api.Test;

class BudgetAdaptersTest {
    @Test void uploadHookLeavesNativeCallbackAndInitialQueueUntouched() throws Exception {
        var adapter = new dev.hoyin1600p.vault_render_optimization.mixin.chunk.EmbeddiumAdaptiveBudgetMixin() {};
        var guard = new TerrainLoadingGuard(64 * AdaptiveChunkBudget.MIB);
        guard.begin(0, new int[5], 0, 0, 0, 0, 0, false);
        guard.begin(500_000_000L, new int[5], 0, 0, 0, 0, 0, false);
        assertTrue(guard.pacing());
        var queue = new ArrayDeque<ChunkBuildResult>();
        var unbuilt = mock(RenderSection.class);
        queue.add(new ChunkBuildResult(unbuilt, null, Map.of(), 2));
        var builder = mock(me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder.class,
                withSettings().extraInterfaces(BudgetBuilderAccess.class));
        when(((BudgetBuilderAccess) builder).vro$pendingResults()).thenReturn(queue);
        var type = adapter.getClass().getSuperclass();
        for (var entry : Map.<String, Object>of("builder", builder, "vro$budgetActive", true,
                "vro$loading", guard).entrySet()) {
            var field = type.getDeclaredField(entry.getKey());
            field.setAccessible(true);
            field.set(adapter, entry.getValue());
        }
        var limit = type.getDeclaredMethod("vro$limitAdmissions", int.class,
                me.jellysquid.mods.sodium.client.render.chunk.ChunkUpdateType.class);
        limit.setAccessible(true);
        assertEquals(64, limit.invoke(adapter, 64,
                me.jellysquid.mods.sodium.client.render.chunk.ChunkUpdateType.INITIAL_BUILD));
        var callback = new org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean>("upload", true);
        var upload = type.getDeclaredMethod("vro$spreadUploads", callback.getClass());
        upload.setAccessible(true);
        upload.invoke(adapter, callback);
        assertFalse(callback.isCancelled()); // Native uploader must run, with its entire original queue.
        assertEquals(1, queue.size());
        assertFalse(guard.pacing());
    }

    @Test void cachedInitialResultBehindSortsRequestsNativeDrainWithoutTakingOwnership() {
        var built = mock(RenderSection.class);
        when(built.isBuilt()).thenReturn(true);
        var unbuilt = mock(RenderSection.class);
        when(unbuilt.isBuilt()).thenReturn(false);
        var queue = new ArrayDeque<ChunkBuildResult>();
        var sort = new IndexOnlySortResult(built, 1, Map.of());
        queue.add(sort);
        assertFalse(BudgetResults.needsNativeDrain(queue));
        var initial = new ChunkBuildResult(unbuilt, null, Map.of(), 2);
        queue.add(initial);
        assertTrue(BudgetResults.needsNativeDrain(queue));
        assertTrue(new BudgetResults().inspect(queue, 1).unbuiltTerrain());
        assertSame(sort, queue.remove());
        assertSame(initial, queue.remove()); // neither reordered, consumed nor deleted by inspection
    }

    @Test void ordinaryRebuildCountsBothNativeBuffersAcrossPasses() {
        var solid = new IndexedVertexData(null, new NativeBuffer(1024), new NativeBuffer(96));
        var translucent = new IndexedVertexData(null, new NativeBuffer(512), new NativeBuffer(48));
        var result = new ChunkBuildResult(null, null, Map.of(
                BlockRenderPass.SOLID, new ChunkMeshData(solid, Map.of()),
                BlockRenderPass.TRANSLUCENT, new ChunkMeshData(translucent, Map.of())), 0);
        try { assertEquals(1680, BudgetResults.bytes(result)); }
        finally { result.delete(); }
    }

    @Test void indexOnlyResultsCountOnlyIndicesAndKeepNativeOwnership() {
        NativeBuffer data = new NativeBuffer(48);
        var result = new IndexOnlySortResult(null, 0,
                Map.of(BlockRenderPass.TRANSLUCENT, new IndexOnlySortResult.Indices(null, data)));
        try {
            assertEquals(48, BudgetResults.bytes(result));
            var queue = new ArrayDeque<ChunkBuildResult>();
            queue.add(result);
            var ledger = new BudgetResults();
            assertEquals(48, ledger.inspect(queue, 100).bytes());
            assertEquals(100, ledger.inspect(queue, 200).oldestWait());
            var drain = new BudgetedDrain<>(queue, BudgetResults::bytes, ledger::forget, 10, 1);
            assertSame(result, drain.next());
            assertEquals(0, ledger.inspect(queue, 300).count());
            ledger.clear();
            // Still live; VRO's observer/drain must never free results. Native upload/destruction owns them.
            data.getDirectBuffer().put(0, (byte) 7);
            assertEquals(7, data.getDirectBuffer().get(0));
        } finally { result.delete(); }
    }

    @Test void nativeQueueRemovalAndOverloadDoNotRetainUnboundedMetadata() {
        var queue = new ArrayDeque<ChunkBuildResult>();
        for (int i = 0; i < 4100; i++) queue.add(new ChunkBuildResult(null, null, Map.of(), i));
        var ledger = new BudgetResults();
        assertEquals(4097, ledger.inspect(queue, 100).count());
        queue.clear();
        assertEquals(0, ledger.inspect(queue, 200).count());
        assertEquals(0, ledger.inspect(queue, 300).oldestWait());
    }

    @Test void timedTaskPassesExactInputsResultAndCleanupToNativeTask() {
        var original = mock(ChunkRenderBuildTask.class);
        var context = mock(ChunkBuildContext.class);
        var cancel = mock(CancellationSource.class);
        var result = new ChunkBuildResult(null, null, Map.of(), 17);
        when(original.performBuild(context, cancel)).thenReturn(result);
        var timed = new TimedChunkTask(original, new AdaptiveChunkBudget(1L << 30), false);
        assertSame(result, timed.performBuild(context, cancel));
        timed.releaseResources();
        verify(original).performBuild(same(context), same(cancel));
        verify(original).releaseResources();
        verifyNoMoreInteractions(original);
    }

    @Test void cancellationAndFailuresDoNotChangeTheNativeLifecycle() {
        var original = mock(ChunkRenderBuildTask.class);
        var timed = new TimedChunkTask(original, new AdaptiveChunkBudget(1L << 30), true);
        assertNull(timed.performBuild(null, null));
        var failure = new IllegalStateException("simulated worker failure");
        when(original.performBuild(null, null)).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> timed.performBuild(null, null)));
        verify(original, never()).releaseResources(); // The native executor calls this in its finally block.
        timed.releaseResources();
        verify(original).releaseResources();
    }
}
