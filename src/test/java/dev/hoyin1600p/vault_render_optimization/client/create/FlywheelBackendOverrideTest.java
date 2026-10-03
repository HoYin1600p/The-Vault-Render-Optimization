package dev.hoyin1600p.vault_render_optimization.client.create;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jozufozu.flywheel.config.BackendType;
import org.junit.jupiter.api.Test;

class FlywheelBackendOverrideTest {
    @Test
    void storedOffWithOptionAndOptimizationsOnReadsAsInstancing() {
        assertEquals(BackendType.INSTANCING, effective(BackendType.OFF, true, true));
    }

    @Test
    void everyOtherCombinationKeepsTheStoredValue() {
        for (BackendType stored : BackendType.values()) {
            for (int flags = 0; flags < 4; flags++) {
                boolean optimizations = (flags & 1) != 0;
                boolean option = (flags & 2) != 0;
                if (stored == BackendType.OFF && optimizations && option) {
                    continue;
                }
                assertEquals(stored, effective(stored, optimizations, option),
                        stored + " optimizations=" + optimizations + " option=" + option);
            }
        }
    }

    private static BackendType effective(BackendType stored, boolean optimizations, boolean option) {
        return FlywheelBackendOverride.effective(stored, BackendType.OFF, BackendType.INSTANCING, optimizations, option);
    }
}
