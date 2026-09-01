package dev.hoyin1600p.vault_render_optimization.renderertransfer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BootstrapRendererTransferConfigTest {
    @Test
    void defaultsEveryTransferOnAndReadsExactNestedKeys() {
        var defaults = BootstrapRendererTransferConfig.resolveValues(path -> null);
        for (RendererTransferFeature feature : RendererTransferFeature.values()) {
            assertTrue(defaults.options().get(feature));
        }

        var configured = BootstrapRendererTransferConfig.resolveValues(path -> path.equals(List.of(
                "embeddium_transfers", RendererTransferFeature.ADJACENT_BLOCK_OCCLUSION.configKey()
        )) ? false : null);
        assertFalse(configured.options().get(RendererTransferFeature.ADJACENT_BLOCK_OCCLUSION));
    }
}
