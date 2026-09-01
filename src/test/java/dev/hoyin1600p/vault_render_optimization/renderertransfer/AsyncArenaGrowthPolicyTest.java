package dev.hoyin1600p.vault_render_optimization.renderertransfer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AsyncArenaGrowthPolicyTest {
    @Test
    void reservesOneSixthHeadroomForOrdinaryGrowth() {
        int increment = AsyncArenaGrowthPolicy.fixedGrowthIncrement(600, 6, 64);
        assertEquals(100, increment);
        assertEquals(700, AsyncArenaGrowthPolicy.nextCapacity(600, 500, 50, increment));
    }

    @Test
    void repeatedGrowthUsesTheFixedInitialIncrementInsteadOfCompounding() {
        int increment = AsyncArenaGrowthPolicy.fixedGrowthIncrement(600, 6, 64);
        assertEquals(800, AsyncArenaGrowthPolicy.nextCapacity(700, 650, 100, increment));
    }

    @Test
    void largeUploadAlwaysFitsEvenWhenItExceedsHeadroom() {
        assertEquals(1500, AsyncArenaGrowthPolicy.nextCapacity(600, 500, 1000, 100));
    }

    @Test
    void configuredCeilingBoundsSpeculativeVramOnly() {
        int mib = 1024 * 1024;
        int increment = AsyncArenaGrowthPolicy.fixedGrowthIncrement(600 * mib, 2, 1);
        assertEquals(mib, increment);
        assertEquals(601 * mib, AsyncArenaGrowthPolicy.nextCapacity(
                600 * mib, 600 * mib, 1, increment
        ));
    }
}
