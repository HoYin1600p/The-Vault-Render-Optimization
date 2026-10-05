package dev.hoyin1600p.vault_render_optimization.client.benchmark.scene;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class SceneIdsTest {
    @Test void reservedNegativeIdsKeepDecreasingAcrossSceneSizedAllocations() {
        SceneIds ids = new SceneIds();
        var seen = new HashSet<Integer>();
        int previous = SceneIds.FIRST + 1;
        for (int i = 0; i < SceneComposition.TOTAL * 4; i++) {
            int id = ids.next();
            assertEquals(previous - 1, id);
            assertTrue(id < 0);
            assertTrue(seen.add(id));
            previous = id;
        }
    }
}
