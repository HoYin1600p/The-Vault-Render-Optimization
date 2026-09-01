package dev.hoyin1600p.vault_render_optimization.client.compat.manastealer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ManaStealerPopulationPolicyTest {
    @Test
    void preservesFirstTestQualityTargets() {
        assertEquals(80, ManaStealerPopulationPolicy.target(ManaStealerPopulationPolicy.ALL, 80, 52, 20));
        assertEquals(52, ManaStealerPopulationPolicy.target(ManaStealerPopulationPolicy.DECREASED, 80, 52, 20));
        assertEquals(20, ManaStealerPopulationPolicy.target(ManaStealerPopulationPolicy.MINIMAL, 80, 52, 20));
    }

    @Test
    void clampsNegativeTuningValues() {
        assertEquals(0, ManaStealerPopulationPolicy.target(ManaStealerPopulationPolicy.ALL, -1, 52, 20));
        assertEquals(0, ManaStealerPopulationPolicy.target(ManaStealerPopulationPolicy.DECREASED, 80, -1, 20));
        assertEquals(0, ManaStealerPopulationPolicy.target(ManaStealerPopulationPolicy.MINIMAL, 80, 52, -1));
    }
}
