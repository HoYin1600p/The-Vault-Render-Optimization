package dev.hoyin1600p.vault_render_optimization.renderertransfer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AsyncArenaGrowthPolicyTest {
    @Test
    void reservesOneSixthHeadroomForOrdinaryGrowth() {
        assertEquals(700, AsyncArenaGrowthPolicy.nextCapacity(600, 500, 50, 6, 64));
    }

    @Test
    void largeUploadAlwaysFitsEvenWhenItExceedsHeadroom() {
        assertEquals(1500, AsyncArenaGrowthPolicy.nextCapacity(600, 500, 1000, 6, 64));
    }

    @Test
    void configuredCeilingBoundsSpeculativeVramOnly() {
        int mib = 1024 * 1024;
        assertEquals(601 * mib,
                AsyncArenaGrowthPolicy.nextCapacity(600 * mib, 600 * mib, 1, 2, 1));
    }
}
