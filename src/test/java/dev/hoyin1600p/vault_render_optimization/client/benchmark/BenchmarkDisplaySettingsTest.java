package dev.hoyin1600p.vault_render_optimization.client.benchmark;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class BenchmarkDisplaySettingsTest {
    @Test
    void failedStartupRollsBackPartialDisplayChanges() {
        class Options implements BenchmarkDisplaySettings.Options {
            boolean vsync = true;
            int limit = 144;
            public boolean vsync() { return vsync; }
            public int optionLimit() { return limit; }
            public int windowLimit() { return limit; }
            public void apply(boolean value, int option, int window) {
                vsync = value;
                limit = option;
                if (!value) throw new IllegalStateException("test display failure");
            }
        }
        Options options = new Options();
        assertThrows(IllegalStateException.class, () -> new BenchmarkDisplaySettings(options));
        assertTrue(options.vsync);
        assertEquals(144, options.limit);
    }

    @Test
    void restoresExactPreviousOptionsAndWindowValuesForBothVsyncStates() {
        for (boolean initialVsync : new boolean[] {false, true}) {
            class Options implements BenchmarkDisplaySettings.Options {
                boolean vsync = initialVsync;
                int optionLimit = 144;
                int windowLimit = 60;
                int writes;
                public boolean vsync() { return vsync; }
                public int optionLimit() { return optionLimit; }
                public int windowLimit() { return windowLimit; }
                public void apply(boolean value, int option, int window) {
                    writes++;
                    vsync = value;
                    optionLimit = option;
                    windowLimit = window;
                }
            }
            Options options = new Options();
            BenchmarkDisplaySettings session = new BenchmarkDisplaySettings(options);
            assertFalse(options.vsync);
            assertEquals(260, options.optionLimit);
            assertEquals(260, options.windowLimit);
            session.close();
            assertEquals(initialVsync, options.vsync);
            assertEquals(144, options.optionLimit);
            assertEquals(60, options.windowLimit);
            session.close();
            assertEquals(2, options.writes);
        }
    }
}
