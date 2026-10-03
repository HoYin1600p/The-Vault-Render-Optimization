package dev.hoyin1600p.vault_render_optimization.client.particle;

import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParticleLifecycleTest {
    @Test void dormantWorkerCacheCannotStronglyRetainAWorld() throws Exception {
        Class<?> state = Class.forName(ParticleSharedLightCache.class.getName() + "$State");
        assertEquals(WeakReference.class, state.getDeclaredField("level").getType());
    }

    @Test void teardownIsInAlwaysRegisteredParticleMixinNotOptionalLightingMixin() throws Exception {
        String source = Files.readString(Path.of("src/main/java/dev/hoyin1600p/vault_render_optimization/mixin/ParticleEngineDiagnosticsMixin.java"));
        assertTrue(source.contains("method = \"setLevel\""));
        assertTrue(source.contains("ParticleDiagnostics.reset();"));
        assertTrue(Files.readString(Path.of("src/main/resources/mixins.vault_render_optimization.json"))
                .contains("\"ParticleEngineDiagnosticsMixin\""));
        assertFalse(Files.readString(Path.of("src/main/java/dev/hoyin1600p/vault_render_optimization/mixin/VaultRenderOptimizationMixinPlugin.java"))
                .contains("\"ParticleEngineDiagnosticsMixin\""));
    }
}
