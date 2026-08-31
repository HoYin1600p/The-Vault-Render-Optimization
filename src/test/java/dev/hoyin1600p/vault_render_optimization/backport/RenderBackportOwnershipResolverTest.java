package dev.hoyin1600p.vault_render_optimization.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RenderBackportOwnershipResolverTest {
    private static final RenderBackportFeature FEATURE =
            RenderBackportFeature.MODEL_VARIANT_TRAVERSAL;

    @Test
    void vroOwnsAnEnabledCompatibleFeatureWhenNoOtherImplementationIsActive() {
        assertOwner(RenderBackportOwner.VRO, true, false, true, false, ModernFixOwnership.ABSENT, null);
    }

    @Test
    void currentVhAcceleratorMarkerWinsDuringTheTemporaryOverlap() {
        assertOwner(RenderBackportOwner.VH_ACCELERATOR, true, false, true, true, ModernFixOwnership.ACTIVE, null);
    }

    @Test
    void activeModernFixWinsWhenVhAcceleratorNoLongerContainsTheFeature() {
        assertOwner(RenderBackportOwner.MODERNFIX, true, false, true, false, ModernFixOwnership.ACTIVE, null);
    }

    @Test
    void unknownModernFixStateFailsClosed() {
        assertOwner(RenderBackportOwner.UNAVAILABLE, true, false, true, false, ModernFixOwnership.UNKNOWN, null);
    }

    @Test
    void compareModeAndConfigEachDisableVroOwnership() {
        assertOwner(RenderBackportOwner.DISABLED, true, true, true, false, ModernFixOwnership.ABSENT, null);
        assertOwner(RenderBackportOwner.DISABLED, true, false, false, false, ModernFixOwnership.ABSENT, null);
    }

    @Test
    void compatibilityAndPhysicalSideFailClosedBeforeOwnership() {
        assertOwner(RenderBackportOwner.UNAVAILABLE, true, false, true, true, ModernFixOwnership.ACTIVE, "blocked");
        assertOwner(RenderBackportOwner.UNAVAILABLE, false, false, true, true, ModernFixOwnership.ACTIVE, null);
    }

    private static void assertOwner(
            RenderBackportOwner expected,
            boolean physicalClient,
            boolean compareMode,
            boolean configured,
            boolean vhAccelerator,
            ModernFixOwnership modernFix,
            String compatibilityBlocker
    ) {
        assertEquals(
                expected,
                RenderBackportOwnershipResolver.resolve(
                        FEATURE,
                        physicalClient,
                        compareMode,
                        configured,
                        vhAccelerator,
                        modernFix,
                        compatibilityBlocker
                ).owner()
        );
    }
}
