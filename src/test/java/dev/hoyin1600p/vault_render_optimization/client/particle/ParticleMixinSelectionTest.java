package dev.hoyin1600p.vault_render_optimization.client.particle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ParticleMixinSelectionTest {
    @Test
    void rendererAndPortablePathsAreMutuallyExclusive() {
        assertTrue(ParticleMixinSelection.rendererPath(false, true, false));
        assertFalse(ParticleMixinSelection.portablePath(false, true, false));

        assertFalse(ParticleMixinSelection.rendererPath(false, false, false));
        assertTrue(ParticleMixinSelection.portablePath(false, false, false));
    }

    @Test
    void fleroviumOwnsTheWholeBillboardPath() {
        assertFalse(ParticleMixinSelection.rendererPath(false, true, true));
        assertFalse(ParticleMixinSelection.portablePath(false, false, true));
    }

    @Test
    void failedDiscoveryFailsClosed() {
        assertFalse(ParticleMixinSelection.rendererPath(true, true, false));
        assertFalse(ParticleMixinSelection.portablePath(true, false, false));
    }
}
