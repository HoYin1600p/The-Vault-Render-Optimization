package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.system.MemoryUtil;
import static dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuEntityModels.*;
import static dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuVerifier.*;
import static dev.hoyin1600p.vault_render_optimization.client.entitygpu.ParkedSegments.*;

/** The upload hand-off: arm, pop, park, upload and dispatch of reserved batches (render thread only). */
final class GpuUploadPath {
    private GpuUploadPath() {
    }

    // Upload hand-off between BufferUploader.end, BufferBuilder.popNextBuffer and BufferUploader._end.
    static boolean uploadArmed;
    static HoleBatch handoff;
    static ByteBuffer handoffBuffer;

    /** {@code BufferUploader.end} HEAD: the next pop on this thread feeds the immediate upload. */
    public static void armUpload() {
        uploadArmed = RenderSystem.isOnRenderThread();
    }

    /**
     * {@code BufferUploader.end} RETURN. The arm, hand-off and parked batches belong to the render thread:
     * an upload on any other thread (a mod's own worker) must not consume or fill them.
     */
    public static void disarmUpload() {
        if (!RenderSystem.isOnRenderThread()) return;
        uploadArmed = false;
        if (handoff != null) {
            // Only reachable if _end skipped its hook; fill so the buffer is at least never left with holes.
            LATE_FILLS.incrementAndGet();
            handoff.fillOnCpu(handoffBuffer);
            release(handoff);
            handoff = null;
            handoffBuffer = null;
        }
    }

    /** {@code BufferBuilder.popNextBuffer} RETURN for a draw state that has holes. */
    public static void popped(HoleBatch batch, BufferBuilder.DrawState drawState, ByteBuffer slice, Object owner) {
        if (!RenderSystem.isOnRenderThread()) {
            // Never touch the render thread's arm or hand-off from another thread.
            UNARMED_FILLS.incrementAndGet();
            fillOnCpu(batch, slice);
            return;
        }
        boolean armed = uploadArmed;
        uploadArmed = false;
        boolean small = batch.vertices() < MIN_GPU_VERTICES;
        if (owner instanceof GpuHoleBuilder builder) builder.vro$noteBatch(small);
        if (small && !verify) {
            SMALL_FILLS.incrementAndGet();
            fillOnCpu(batch, slice);
            return;
        }
        long bytes = (long) drawState.vertexCount() * drawState.format().getVertexSize();
        // The instance records are bound as one storage block too, so they share the output's size limit.
        long recordBytes = (long) batch.count() * InstanceRecord.WORDS * 4;
        boolean gpuReady = state == State.ACTIVE && bytes <= maxDrawBytes && recordBytes <= maxDrawBytes
                && (batch.particles()
                ? drawState.format() == DefaultVertexFormat.PARTICLE && particlesActive
                : batch.items() && !(batch.iris() ? itemsIrisActive : itemsActive) ? false
                : batch.iris() ? irisFormat != null && drawState.format() == irisFormat
                : drawState.format() == DefaultVertexFormat.NEW_ENTITY);
        if (armed && gpuReady && handoff == null) {
            handoff = batch;
            handoffBuffer = slice;
            return;
        }
        if (!armed && gpuReady && segmentPopDepth > 0) {
            long address = MemoryUtil.memAddress(slice);
            Parked previous = PARKED.put(address,
                    new Parked(batch, owner, drawState.vertexCount(), drawState.format(), slice.duplicate()));
            if (previous != null) {
                PARKED_DROPPED.incrementAndGet();
                release(previous.batch());
            }
            PARKED_BATCHES.incrementAndGet();
            return;
        }
        if (!armed) UNARMED_FILLS.incrementAndGet();
        fillOnCpu(batch, slice);
    }

    /**
     * A pop without holes of its own. If it is the immediate upload of memory a parked batch belongs
     * to (Oculus' stand-in builder), hand that batch to the GPU; either way the arm is consumed.
     */
    public static void poppedWithoutHoles(BufferBuilder.DrawState drawState, ByteBuffer slice) {
        if (!RenderSystem.isOnRenderThread()) return;
        boolean armed = uploadArmed;
        uploadArmed = false;
        if (!armed || PARKED.isEmpty() || handoff != null) return;
        Parked parked = PARKED.remove(MemoryUtil.memAddress(slice));
        if (parked == null) return;
        if (state != State.ACTIVE || drawState.vertexCount() != parked.vertexCount()
                || drawState.format() != parked.format()) {
            // Not the draw state it was parked with: never upload holes. Fill the memory the batch was popped into,
            // at its own size: this upload's slice can be shorter, and the batch's offsets are relative to its own pop.
            LATE_FILLS.incrementAndGet();
            noteLateFill(ParkedSegments.lateFillReason(state, parked.vertexCount(), drawState.vertexCount(),
                    parked.format(), drawState.format(), parked.batch().kind(), parked.owner(),
                    parked.slice().limit(), slice.limit()));
            fillOnCpu(parked.batch(), parked.slice());
            return;
        }
        handoff = parked.batch();
        handoffBuffer = slice;
    }

    /**
     * Replaces {@code BufferUploader._end}'s vertex {@code glBufferData}. When this upload carries a
     * reserved batch, only the bytes the CPU wrote are copied: the buffer is allocated at full size
     * and each range between reserved vertices is uploaded, so reserved vertices (overwritten by the
     * GPU right after) are never copied by the driver. Any other upload is vanilla's call unchanged.
     */
    public static void uploadVertices(int target, ByteBuffer buffer, int usage, VertexFormat format) {
        HoleBatch batch = handoff;
        if (batch == null || handoffBuffer != buffer) {
            GlStateManager._glBufferData(target, buffer, usage);
            if (batch != null) uploaded(buffer, format);
            return;
        }
        boolean gaps;
        try {
            gaps = uploadGaps(target, buffer, usage, batch);
        } catch (RuntimeException failure) {
            gaps = false; // upload everything; uploaded() still writes the reserved vertices
        }
        if (!gaps) GlStateManager._glBufferData(target, buffer, usage);
        uploaded(buffer, format);
    }

    /** @return false when the holes are not in ascending order (never expected): upload everything */
    static boolean uploadGaps(int target, ByteBuffer buffer, int usage, HoleBatch batch) {
        int size = buffer.remaining();
        int cursor = 0;
        for (int i = 0; i < batch.holeCount(); i++) {
            int start = batch.holeStart(i);
            if (start < cursor) return false;
            cursor = batch.holeEnd(i);
        }
        if (cursor > size) return false;
        GL15C.glBufferData(target, size, usage);
        long address = MemoryUtil.memAddress(buffer);
        cursor = 0;
        long skipped = 0;
        for (int i = 0; i < batch.holeCount(); i++) {
            int start = batch.holeStart(i);
            int end = batch.holeEnd(i);
            if (start > cursor) GL15C.nglBufferSubData(target, cursor, start - cursor, address + cursor);
            skipped += end - start;
            cursor = end;
        }
        if (cursor < size) GL15C.nglBufferSubData(target, cursor, size - cursor, address + cursor);
        BYTES_NOT_UPLOADED.addAndGet(skipped);
        return true;
    }

    /** {@code BufferUploader._end}, right after the vertex upload and before the draw. */
    public static void uploaded(ByteBuffer buffer, VertexFormat format) {
        HoleBatch batch = handoff;
        if (batch == null) return;
        ByteBuffer expected = handoffBuffer;
        handoff = null;
        handoffBuffer = null;
        if (expected != buffer) {
            // Not the buffer that was popped for this upload; never draw holes.
            LATE_FILLS.incrementAndGet();
            fillOnCpu(batch, expected);
            return;
        }
        try {
            if (batch.particles()) {
                backend.dispatchParticles(batch, format.getOrCreateVertexBufferObject(), verify);
                PARTICLES_GPU.addAndGet(batch.particleCount());
            } else {
                backend.dispatch(batch, format.getOrCreateVertexBufferObject(), verify);
                if (batch.iris()) IRIS_DISPATCHES.incrementAndGet();
            }
            forgetProgram();
            if (verify) verify(batch, buffer, format);
            DISPATCHES.incrementAndGet();
            VERTICES_GPU.addAndGet(batch.vertices());
            release(batch);
        } catch (Throwable failure) {
            try {
                restoreProgram();
                batch.fillOnCpu(buffer);
                // The vertex buffer is still bound by BufferUploader; replace the uploaded bytes.
                int limit = buffer.limit();
                buffer.position(0);
                GL15C.glBufferSubData(GL15C.GL_ARRAY_BUFFER, 0L, buffer);
                buffer.position(0).limit(limit);
                release(batch);
            } catch (Throwable secondary) {
                failure.addSuppressed(secondary);
            }
            fail("dispatch failed", failure);
        }
    }
}
