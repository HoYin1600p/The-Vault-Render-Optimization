package dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SophisticatedStorageCompatibilityTest {
    @Test
    void acceptsTheValidatedRemasteredPair() {
        assertTrue(SophisticatedStorageCompatibility.supports(
                "1.18.2-0.9.8.915",
                "1.18.2-0.6.4.604"
        ));
    }

    @Test
    void rejectsMissingOrDifferentInternalLayouts() {
        assertFalse(SophisticatedStorageCompatibility.supports(null, "1.18.2-0.6.4.604"));
        assertFalse(SophisticatedStorageCompatibility.supports("1.18.2-0.9.8.915", null));
        assertFalse(SophisticatedStorageCompatibility.supports(
                "1.18.2-0.9.8.916",
                "1.18.2-0.6.4.604"
        ));
        assertFalse(SophisticatedStorageCompatibility.supports(
                "1.18.2-0.9.8.915",
                "1.18.2-0.6.4.605"
        ));
    }
}
