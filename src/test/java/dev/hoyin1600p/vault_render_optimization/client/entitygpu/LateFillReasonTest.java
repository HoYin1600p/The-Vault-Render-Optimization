package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import org.junit.jupiter.api.Test;

/** The late-fill reason names exactly the conditions that failed, with both sides of each. */
class LateFillReasonTest {
    @Test
    void vertexCountMismatchIsNamedWithBothCounts() {
        String reason = GpuEntityModels.lateFillReason(GpuEntityModels.State.ACTIVE, 400, 120,
                DefaultVertexFormat.NEW_ENTITY, DefaultVertexFormat.NEW_ENTITY, HoleBatch.Kind.ITEMS, new Object(),
                14400, 4320);
        assertTrue(reason.contains("vertex count 400 parked vs 120 uploaded"), reason);
        assertTrue(reason.contains("ITEMS batch"), reason);
        assertTrue(reason.contains("14400 bytes parked vs 4320 uploaded"), reason);
        assertFalse(reason.contains("GPU path"), reason);
        assertFalse(reason.contains("format "), reason);
    }

    @Test
    void stateAndFormatMismatchesAreNamed() {
        String reason = GpuEntityModels.lateFillReason(GpuEntityModels.State.FAILED, 8, 8,
                DefaultVertexFormat.NEW_ENTITY, DefaultVertexFormat.PARTICLE, HoleBatch.Kind.MODELS, null, 288, 288);
        assertTrue(reason.contains("GPU path FAILED"), reason);
        assertTrue(reason.contains("format NEW_ENTITY parked vs PARTICLE uploaded"), reason);
        assertTrue(reason.contains("builder unknown"), reason);
        assertFalse(reason.contains("vertex count"), reason);
    }
}
