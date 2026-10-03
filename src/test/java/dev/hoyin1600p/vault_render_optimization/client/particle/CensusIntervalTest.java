package dev.hoyin1600p.vault_render_optimization.client.particle;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CensusIntervalTest {
    @Test void firstFrameAndResetSampleImmediately() {
        var gate = new CensusInterval();
        assertTrue(gate.due(0));
        assertFalse(gate.due(0));
        gate.reset();
        assertTrue(gate.due(1));
    }

    @Test void samplesAtMostFourTimesPerSecond() {
        var gate = new CensusInterval();
        assertTrue(gate.due(100));
        for (int i = 1; i < 250; i++) assertFalse(gate.due(100 + i * 1_000_000L));
        assertTrue(gate.due(250_000_100L));
    }

    @Test void handlesNanoTimeOverflow() {
        var gate = new CensusInterval();
        long start = Long.MAX_VALUE - 10;
        assertTrue(gate.due(start));
        assertFalse(gate.due(start + 20));
        assertTrue(gate.due(start + CensusInterval.INTERVAL_NANOS));
    }
}
