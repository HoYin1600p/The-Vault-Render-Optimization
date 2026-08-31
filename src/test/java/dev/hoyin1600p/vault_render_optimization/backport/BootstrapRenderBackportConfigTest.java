package dev.hoyin1600p.vault_render_optimization.backport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BootstrapRenderBackportConfigTest {
    @Test
    void missingValuesUseEnabledBackportsAndDisabledCompareMode() {
        BootstrapRenderBackportConfig.Snapshot snapshot =
                BootstrapRenderBackportConfig.resolveValues(path -> null);
        for (RenderBackportFeature feature : RenderBackportFeature.values()) {
            assertTrue(snapshot.options().get(feature));
        }
        assertFalse(snapshot.compareMode());
    }

    @Test
    void eachBackportAndCompareModeResolveIndependently() {
        RenderBackportFeature disabled = RenderBackportFeature.FASTER_TEXTURE_STITCHING;
        BootstrapRenderBackportConfig.Snapshot snapshot =
                BootstrapRenderBackportConfig.resolveValues(path -> configuredValue(path, disabled));
        assertFalse(snapshot.options().get(disabled));
        assertTrue(snapshot.options().get(RenderBackportFeature.CHUNK_MESHING));
        assertTrue(snapshot.compareMode());
    }

    private static Object configuredValue(List<String> path, RenderBackportFeature disabled) {
        if (path.equals(List.of("modernfix_backports", disabled.configKey()))) {
            return false;
        }
        if (path.equals(List.of("benchmark", "compare_mode"))) {
            return true;
        }
        return null;
    }
}
