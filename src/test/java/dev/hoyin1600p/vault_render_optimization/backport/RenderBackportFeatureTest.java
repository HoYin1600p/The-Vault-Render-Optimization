package dev.hoyin1600p.vault_render_optimization.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class RenderBackportFeatureTest {
    @Test
    void metadataContainsExactlyTheTransferredFeatureSet() {
        assertEquals(11, RenderBackportFeature.values().length);
        HashSet<String> ids = new HashSet<>();
        HashSet<String> configKeys = new HashSet<>();
        for (RenderBackportFeature feature : RenderBackportFeature.values()) {
            ids.add(feature.id());
            configKeys.add(feature.configKey());
            assertNotNull(RenderBackportFeature.forMixin(
                    "dev.hoyin1600p.vault_render_optimization.mixin.backport.modernfix.client."
                            + feature.mixinPackageFragment() + ".ExampleMixin"
            ));
        }
        assertEquals(11, ids.size());
        assertEquals(11, configKeys.size());
    }
}
