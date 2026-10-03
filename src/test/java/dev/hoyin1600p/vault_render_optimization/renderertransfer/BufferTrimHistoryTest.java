package dev.hoyin1600p.vault_render_optimization.renderertransfer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BufferTrimHistoryTest {
    private static final int MIB = 1024 * 1024;

    @Test void keepsRecentPeakThenAgesOutWithoutPressure() {
        var history = new BufferTrimHistory();
        assertEquals(8 * MIB, history.target(8 * MIB, MIB, 6 * MIB, 0, false));
        assertEquals(8 * MIB, history.target(8 * MIB, MIB, MIB, 1, false));
        assertEquals(8 * MIB, history.target(8 * MIB, MIB, MIB, BufferTrimHistory.WINDOW_NANOS - 1, false));
        assertEquals(MIB, history.target(8 * MIB, MIB, MIB, BufferTrimHistory.WINDOW_NANOS, false));
    }

    @Test void pressureRequiresThreeSmallBuildsAndDoesNotChurnAlternatingDemand() {
        var history = new BufferTrimHistory();
        for (int i = 0; i < 10; i++) {
            assertEquals(8 * MIB, history.target(8 * MIB, MIB, 6 * MIB, i * 2, true));
            assertEquals(8 * MIB, history.target(8 * MIB, MIB, MIB, i * 2 + 1, true));
        }
        assertEquals(8 * MIB, history.target(8 * MIB, MIB, MIB, 20, true));
        assertEquals(MIB, history.target(8 * MIB, MIB, MIB, 21, true));
    }

    @Test void idleReturnTrimsButNeverBelowBaselineOrGrowsDuringTrim() {
        var history = new BufferTrimHistory();
        history.target(8 * MIB, MIB, 6 * MIB, 0, false);
        assertEquals(MIB, history.target(8 * MIB, MIB, 6 * MIB, BufferTrimHistory.WINDOW_NANOS, false));
        assertEquals(MIB, history.target(MIB, MIB, 8 * MIB, BufferTrimHistory.WINDOW_NANOS + 1, true));
    }

    @Test void aggregateTracksOnlyExcessAndExplicitDestroyRemovesIt() {
        var a = new BufferTrimHistory();
        var b = new BufferTrimHistory();
        try {
            assertFalse(RetainedBufferPressure.observe(a, 8 * MIB, MIB, 10 * MIB, 0));
            assertTrue(RetainedBufferPressure.observe(b, 8 * MIB, MIB, 10 * MIB, 1));
            RetainedBufferPressure.remove(a);
            assertFalse(RetainedBufferPressure.observe(b, 8 * MIB, MIB, 10 * MIB, 2));
        } finally {
            RetainedBufferPressure.remove(a); RetainedBufferPressure.remove(b);
        }
    }
}
