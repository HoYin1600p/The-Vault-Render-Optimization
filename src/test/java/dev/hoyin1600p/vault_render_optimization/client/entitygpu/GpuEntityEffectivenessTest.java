package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GpuEntityEffectivenessTest {
    private static void frames(GpuEntityModels.Effectiveness watch, int count, long dispatchesPerFrame, long unarmedPerFrame) {
        for (int i = 0; i < count; i++) {
            GpuEntityModels.DISPATCHES.addAndGet(dispatchesPerFrame);
            GpuEntityModels.UNARMED_FILLS.addAndGet(unarmedPerFrame);
            watch.allowFrame();
        }
    }

    @Test
    void pausesOnlyWhenReservationsNeverReachTheGpu() {
        GpuEntityModels.Effectiveness watch = new GpuEntityModels.Effectiveness();
        // Working path: dispatches happen, occasional unarmed fills (other uploaders) are fine.
        frames(watch, 3 * GpuEntityModels.Effectiveness.PROBE_FRAMES, 5, 1);
        assertFalse(watch.paused());
        // No entities at all: nothing dispatched, nothing filled - not a reason to pause.
        frames(watch, 2 * GpuEntityModels.Effectiveness.PROBE_FRAMES, 0, 0);
        assertFalse(watch.paused());
        // Every reservation filled on the CPU outside the upload (Oculus batched entity rendering).
        frames(watch, GpuEntityModels.Effectiveness.PROBE_FRAMES + 1, 0, 3);
        assertTrue(watch.paused());
        assertFalse(watch.allowFrame(), "paused frames do not reserve");
        assertTrue(watch.reason().contains("BufferUploader.end"), watch.reason());
        // After the pause it probes again.
        frames(watch, GpuEntityModels.Effectiveness.PAUSE_FRAMES, 0, 0);
        assertFalse(watch.paused());
    }
}
