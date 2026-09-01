package dev.hoyin1600p.vault_render_optimization.renderertransfer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NullBufferVertexSinkGuardTest {
    @Test
    void fallsBackOnlyWhenTheBackingBufferIsAbsent() {
        assertTrue(NullBufferVertexSinkGuard.requiresFallback(null));
        assertFalse(NullBufferVertexSinkGuard.requiresFallback(new Object()));
    }
}
