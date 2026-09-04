package dev.hoyin1600p.vault_render_optimization.client.chunk.sorting;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import me.jellysquid.mods.sodium.client.gl.arena.*;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.render.chunk.*;
import me.jellysquid.mods.sodium.client.render.chunk.compile.*;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Real VRO upload/result code and renderer graphics states; world, GL and render-layer boundaries are simulated. */
class IndexOnlyUploadsTest {
    private RenderSection section;
    private RenderRegion region;
    private RenderRegion.RenderRegionArenas arenas;
    private GlBufferArena vertexArena, indexArena;
    private CommandList commands;
    private ChunkBufferSorter.SortBuffer generation;
    private GlBufferSegment vertices, oldIndices;
    private AtomicReference<ChunkGraphicsState> current;
    private final List<GlBufferSegment> allocated = new ArrayList<>();

    @BeforeEach
    void setup() throws Exception {
        section = mock(RenderSection.class);
        region = mock(RenderRegion.class);
        arenas = mock(RenderRegion.RenderRegionArenas.class);
        commands = mock(CommandList.class);
        vertexArena = mock(GlBufferArena.class);
        indexArena = mock(GlBufferArena.class);
        var field = RenderRegion.RenderRegionArenas.class.getDeclaredField("indexBuffers");
        field.setAccessible(true);
        field.set(arenas, indexArena);
        field = RenderRegion.RenderRegionArenas.class.getDeclaredField("vertexBuffers");
        field.setAccessible(true);
        field.set(arenas, vertexArena);
        when(section.getRegion()).thenReturn(region);
        when(region.getArenas()).thenReturn(arenas);
        when(section.canAcceptBuildResults(any())).thenReturn(true);
        generation = new ChunkBufferSorter.SortBuffer(ByteBuffer.allocate(120), ByteBuffer.allocate(24), null, Map.of());
        vertices = new GlBufferSegment(vertexArena, 0, 120);
        oldIndices = new GlBufferSegment(indexArena, 0, 24);
        var state = new ChunkGraphicsState(vertices, oldIndices, new ChunkMeshData(null, Map.of()));
        state.setTranslucencyData(generation);
        current = new AtomicReference<>(state);
        when(section.getGraphicsState(BlockRenderPass.TRANSLUCENT)).thenAnswer(call -> current.get());
        when(section.setGraphicsState(eq(BlockRenderPass.TRANSLUCENT), any())).thenAnswer(call -> current.getAndSet(call.getArgument(1)));
        when(indexArena.upload(eq(commands), any())).thenAnswer(call -> {
            try (Stream<PendingUpload> stream = call.getArgument(1)) {
                for (PendingUpload upload : stream.toList()) {
                    var segment = new GlBufferSegment(indexArena, 128 + allocated.size() * 24, 24);
                    allocated.add(segment);
                    var setter = PendingUpload.class.getDeclaredMethod("setResult", GlBufferSegment.class);
                    setter.setAccessible(true);
                    setter.invoke(upload, segment);
                }
            }
            return false;
        });
    }

    private IndexOnlySortResult result(int frame) {
        NativeBuffer data = mock(NativeBuffer.class);
        when(data.getLength()).thenReturn(24);
        return new IndexOnlySortResult(section, frame,
                Map.of(BlockRenderPass.TRANSLUCENT, new IndexOnlySortResult.Indices(generation, data)));
    }

    @Test
    void uploadsOnlyIndicesRetainsVertexOwnershipAndDeletesPayloadExactlyOnce() {
        var result = result(1);
        IndexOnlyUploads.upload(commands, region, List.of(result), batch -> fail("not a full rebuild"));
        assertTrue(result.applied);
        assertSame(vertices, current.get().getVertexSegment());
        assertSame(allocated.get(0), current.get().getIndexSegment());
        assertSame(generation, current.get().getTranslucencyData());
        verifyNoInteractions(vertexArena);
        verify(indexArena).free(oldIndices);
        result.delete();
        verify(result.indices.get(BlockRenderPass.TRANSLUCENT).buffer(), times(1)).free();
    }

    @Test
    void rebuildGenerationChangeOrUnloadRejectsSortWithoutTouchingGpu() {
        var result = result(1);
        current.get().setTranslucencyData(null);
        IndexOnlyUploads.upload(commands, region, List.of(result), batch -> fail());
        assertFalse(result.applied);
        verifyNoInteractions(indexArena, vertexArena);
        var unloaded = result(2);
        current.get().setTranslucencyData(generation);
        when(section.canAcceptBuildResults(any())).thenReturn(false);
        IndexOnlyUploads.upload(commands, region, List.of(unloaded), batch -> fail());
        assertFalse(unloaded.applied);
        verifyNoInteractions(indexArena, vertexArena);
    }

    @Test
    void coalescesCompletedSortsToNewestWithoutCrossingFullRebuilds() {
        var old = result(1);
        var newest = result(3);
        var middle = result(2);
        IndexOnlyUploads.upload(commands, region, List.of(old, newest, middle), batch -> fail());
        assertFalse(old.applied);
        assertTrue(newest.applied);
        assertFalse(middle.applied);
        verify(indexArena, times(1)).upload(eq(commands), any());
        assertEquals(1, allocated.size());
    }

    @Test
    void processesFullRebuildBeforeRejectingItsOlderGeometrySort() {
        ChunkBuildResult full = new ChunkBuildResult(section, null, Map.of(), 2);
        var sort = result(3);
        List<ChunkBuildResult> forwarded = new ArrayList<>();
        IndexOnlyUploads.upload(commands, region, List.of(full, sort), batch -> {
            forwarded.addAll(batch);
            current.get().setTranslucencyData(null);
        });
        assertEquals(List.of(full), forwarded);
        assertFalse(sort.applied);
        verifyNoInteractions(indexArena, vertexArena);
    }

    @Test
    void arenaGrowthInvalidatesTessellationButNotVertexStorage() {
        doAnswer(call -> {
            Stream<PendingUpload> stream = call.getArgument(1);
            var setter = PendingUpload.class.getDeclaredMethod("setResult", GlBufferSegment.class);
            setter.setAccessible(true);
            for (var upload : stream.toList()) setter.invoke(upload, new GlBufferSegment(indexArena, 100, 24));
            return true;
        }).when(indexArena).upload(eq(commands), any());
        IndexOnlyUploads.upload(commands, region, List.of(result(1)), batch -> fail());
        verify(arenas).deleteTessellations(commands);
        verifyNoInteractions(vertexArena);
    }

    @Test
    void failedInstallFreesUnclaimedIndexAllocationExactlyOnce() {
        var sort = result(1);
        when(section.setGraphicsState(eq(BlockRenderPass.TRANSLUCENT), any())).thenThrow(new IllegalStateException("injected"));
        assertThrows(IllegalStateException.class,
                () -> IndexOnlyUploads.upload(commands, region, List.of(sort), batch -> fail()));
        assertFalse(sort.applied);
        assertSame(vertices, current.get().getVertexSegment());
        assertSame(oldIndices, current.get().getIndexSegment());
        verify(indexArena, times(1)).free(allocated.get(0));
        verify(indexArena, never()).free(oldIndices);
        verifyNoInteractions(vertexArena);
        verify(sort.indices.get(BlockRenderPass.TRANSLUCENT).buffer()).free();
    }

    @Test
    void nativeOnlyBatchPassesThroughUnchanged() {
        var batch = List.of(new ChunkBuildResult(section, null, Map.of(), 4));
        IndexOnlyUploads.upload(commands, region, batch, forwarded -> assertSame(batch, forwarded));
        verifyNoInteractions(indexArena, vertexArena);
    }

    @Test
    void olderSortCannotOverrideNewerSortAcrossAnUnrelatedNativeBatch() {
        var newer = result(4);
        var older = result(2);
        var unrelated = new ChunkBuildResult(mock(RenderSection.class), null, Map.of(), 8);
        IndexOnlyUploads.upload(commands, region, List.of(newer, unrelated, older), batch -> assertEquals(List.of(unrelated), batch));
        assertTrue(newer.applied);
        assertFalse(older.applied);
        assertEquals(1, allocated.size());
    }

    @Test
    void taskUsesOnlyIndexNativeAllocationAndTransfersResultCleanup() {
        var format = mock(me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat.class);
        when(format.getStride()).thenReturn(20);
        var indices = ByteBuffer.allocate(24).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < 6; i++) indices.putInt(i * 4, i);
        var snapshot = new ChunkBufferSorter.SortBuffer(ByteBuffer.allocate(120), indices, format, Map.of());
        when(section.getTranslucencyData(BlockRenderPass.TRANSLUCENT)).thenReturn(snapshot);
        try (var nativeBuffers = mockConstruction(NativeBuffer.class, (buffer, context) -> {
            assertEquals(24, context.arguments().get(0));
            when(buffer.getDirectBuffer()).thenReturn(ByteBuffer.allocate(24));
            when(buffer.getLength()).thenReturn(24);
        })) {
            var task = IndexOnlySortTask.capture(section, 1, 0, 0, 0);
            assertNotNull(task);
            var output = (IndexOnlySortResult) task.performBuild(null, () -> false);
            task.releaseResources();
            assertTrue(output.meshes.isEmpty());
            assertSame(snapshot, output.indices.get(BlockRenderPass.TRANSLUCENT).generation());
            assertEquals(1, nativeBuffers.constructed().size());
            verify(nativeBuffers.constructed().get(0), never()).free();
            output.delete();
            output.delete();
            verify(nativeBuffers.constructed().get(0), times(1)).free();
        }
    }

    @Test
    void taskCancellationBeforeBuildAllocatesNothing() {
        when(section.getTranslucencyData(BlockRenderPass.TRANSLUCENT)).thenReturn(generation);
        try (var nativeBuffers = mockConstruction(NativeBuffer.class)) {
            var task = IndexOnlySortTask.capture(section, 1, 0, 0, 0);
            assertNotNull(task);
            assertNull(task.performBuild(null, () -> true));
            task.releaseResources();
            assertTrue(nativeBuffers.constructed().isEmpty());
        }
    }

    @Test
    void partialTaskCancellationFreesEarlierPassOutput() {
        var format = mock(me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat.class);
        when(format.getStride()).thenReturn(20);
        var snapshot = new ChunkBufferSorter.SortBuffer(ByteBuffer.allocate(120), ByteBuffer.allocate(24), format, Map.of());
        when(section.getTranslucencyData(BlockRenderPass.TRANSLUCENT)).thenReturn(snapshot);
        when(section.getTranslucencyData(BlockRenderPass.TRIPWIRE)).thenReturn(snapshot);
        try (var nativeBuffers = mockConstruction(NativeBuffer.class, (buffer, context) ->
                when(buffer.getDirectBuffer()).thenReturn(ByteBuffer.allocate(24)))) {
            var task = IndexOnlySortTask.capture(section, 1, 0, 0, 0);
            var polls = new java.util.concurrent.atomic.AtomicInteger();
            assertNull(task.performBuild(null, () -> polls.incrementAndGet() >= 3));
            task.releaseResources();
            assertEquals(1, nativeBuffers.constructed().size());
            verify(nativeBuffers.constructed().get(0), times(1)).free();
        }
    }

    @Test
    void uploadFailureAfterAllocationRollsBackNewIndexWithoutTouchingOldState() throws Exception {
        var sort = result(1);
        GlBufferSegment unclaimed = new GlBufferSegment(indexArena, 128, 24);
        doAnswer(call -> {
            Stream<PendingUpload> stream = call.getArgument(1);
            var setter = PendingUpload.class.getDeclaredMethod("setResult", GlBufferSegment.class);
            setter.setAccessible(true);
            setter.invoke(stream.findFirst().orElseThrow(), unclaimed);
            throw new IllegalStateException("injected upload failure");
        }).when(indexArena).upload(eq(commands), any());
        assertThrows(IllegalStateException.class,
                () -> IndexOnlyUploads.upload(commands, region, List.of(sort), batch -> fail()));
        verify(indexArena, times(1)).free(unclaimed);
        verify(indexArena, never()).free(oldIndices);
        verifyNoInteractions(vertexArena);
        assertSame(oldIndices, current.get().getIndexSegment());
        verify(sort.indices.get(BlockRenderPass.TRANSLUCENT).buffer()).free();
    }

    @Test
    void repeatedSortsThenUnloadFreeEachIndexAndTheRetainedVertexExactlyOnce() {
        for (int frame = 1; frame <= 100; frame++) {
            var sort = result(frame);
            IndexOnlyUploads.upload(commands, region, List.of(sort), batch -> fail());
            assertTrue(sort.applied);
            assertSame(vertices, current.get().getVertexSegment());
        }
        verifyNoInteractions(vertexArena);
        current.get().delete(); // Native chunk-unload/full-rebuild ownership endpoint.
        verify(vertexArena, times(1)).free(vertices);
        verify(indexArena, times(1)).free(oldIndices);
        for (var segment : allocated) verify(indexArena, times(1)).free(segment);
        assertEquals(100, allocated.size());
    }
}
