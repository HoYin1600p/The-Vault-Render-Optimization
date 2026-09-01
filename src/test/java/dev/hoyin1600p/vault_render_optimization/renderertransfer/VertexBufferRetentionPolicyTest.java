package dev.hoyin1600p.vault_render_optimization.renderertransfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class VertexBufferRetentionPolicyTest {
    @Test
    void growthUsesWriterEndPlusRequestAndUsuallyDoubles() {
        assertEquals(2048, VertexBufferRetentionPolicy.nextCapacity(1024, 900, 200));
        assertEquals(4100, VertexBufferRetentionPolicy.nextCapacity(1024, 4000, 100));
    }

    @Test
    void retainedLimitNeverDropsBelowInitialCapacity() {
        assertEquals(16 * 1024 * 1024,
                VertexBufferRetentionPolicy.retainedCapacityLimitBytes(16, 1024));
        assertEquals(32 * 1024 * 1024,
                VertexBufferRetentionPolicy.retainedCapacityLimitBytes(16, 32 * 1024 * 1024));
    }

    @Test
    void impossibleRequestsFailBeforeIntegerOverflow() {
        assertThrows(IllegalArgumentException.class,
                () -> VertexBufferRetentionPolicy.nextCapacity(1024, Integer.MAX_VALUE, 1));
    }
}
