package dev.hoyin1600p.vault_render_optimization.client.particle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ParticleOptimizationStateTest {
    @Test
    void autoAndVroSelectVroGeometry() {
        assertTrue(resolve(ParticleBillboardOwner.AUTO, true, false));
        assertTrue(resolve(ParticleBillboardOwner.VRO, true, false));
        assertTrue(resolve(ParticleBillboardOwner.AUTO, false, false));
    }

    @Test
    void rendererSelectionYieldsOnlyWhenRendererExists() {
        assertFalse(resolve(ParticleBillboardOwner.RENDERER, true, false));
        assertTrue(resolve(ParticleBillboardOwner.RENDERER, false, false));
    }

    @Test
    void externalOwnerAlwaysWins() {
        assertFalse(resolve(ParticleBillboardOwner.AUTO, true, true));
        assertFalse(resolve(ParticleBillboardOwner.VRO, false, true));
    }

    @Test
    void compareModeAndDisabledOptionYield() {
        assertFalse(ParticleOptimizationState.shouldUseVroBillboardGeometry(
                false, true, ParticleBillboardOwner.VRO, true, false
        ));
        assertFalse(ParticleOptimizationState.shouldUseVroBillboardGeometry(
                true, false, ParticleBillboardOwner.VRO, true, false
        ));
    }

    private static boolean resolve(
            ParticleBillboardOwner owner,
            boolean renderer,
            boolean externalOwner
    ) {
        return ParticleOptimizationState.shouldUseVroBillboardGeometry(
                true, true, owner, renderer, externalOwner
        );
    }
}
