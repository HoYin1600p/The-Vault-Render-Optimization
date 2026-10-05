package dev.hoyin1600p.vault_render_optimization.client.benchmark;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuEntityModels;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

class GpuBenchmarkTogglesTest {
    @Test void uninitializedAndActiveAreAvailableButFailuresAndCompareModeAreNot() {
        try (var gpu = mockStatic(GpuEntityModels.class); var config = mockStatic(ClientOptimizationConfig.class)) {
            config.when(ClientOptimizationConfig::optimizationsEnabled).thenReturn(true);
            for (GpuEntityModels.State state : GpuEntityModels.State.values()) {
                gpu.when(GpuEntityModels::state).thenReturn(state);
                for (BenchmarkToggle toggle : GpuBenchmarkToggles.create(true)) {
                    boolean expected = state == GpuEntityModels.State.ACTIVE || state == GpuEntityModels.State.UNINITIALIZED;
                    assertEquals(expected, toggle.available(), state + " " + toggle.id());
                    assertEquals(expected, toggle.unavailableReason().isEmpty());
                }
            }
            gpu.when(GpuEntityModels::state).thenReturn(GpuEntityModels.State.ACTIVE);
            config.when(ClientOptimizationConfig::optimizationsEnabled).thenReturn(false);
            assertTrue(GpuBenchmarkToggles.create(false).stream().noneMatch(BenchmarkToggle::available));
        }
    }

    @Test void dependenciesStayEnabledAcrossAllRoundsAndRestoreFromDefaultOff() {
        boolean base = ClientOptimizationConfig.gpuEntityModels;
        boolean items = ClientOptimizationConfig.gpuItems;
        boolean particles = ClientOptimizationConfig.gpuParticles;
        boolean shaderEntities = ClientOptimizationConfig.gpuEntityModelsWithShaders;
        boolean shaderParticles = ClientOptimizationConfig.gpuParticlesWithShaders;
        try {
            for (boolean shaders : new boolean[] {false, true}) {
                ClientOptimizationConfig.gpuEntityModels = false;
                ClientOptimizationConfig.gpuItems = false;
                ClientOptimizationConfig.gpuParticles = false;
                ClientOptimizationConfig.gpuEntityModelsWithShaders = false;
                ClientOptimizationConfig.gpuParticlesWithShaders = false;
                List<BenchmarkToggle> toggles = GpuBenchmarkToggles.create(shaders);
                for (BenchmarkToggle toggle : toggles) {
                    if (toggle.id().equals("gpu_entity_models")) continue;
                    toggle.beginMeasure();
                    for (int round = 0; round < 4; round++) {
                        toggle.applySession(BenchmarkPlan.enabled(round));
                        assertTrue(ClientOptimizationConfig.gpuEntityModels);
                        if (shaders && toggle.id().equals("gpu_items")) {
                            assertTrue(ClientOptimizationConfig.gpuEntityModelsWithShaders);
                        }
                        if (shaders && toggle.id().equals("gpu_particles_with_shaders")) {
                            assertTrue(ClientOptimizationConfig.gpuParticles);
                            assertFalse(ClientOptimizationConfig.gpuEntityModelsWithShaders);
                        }
                    }
                    toggle.applySession(false);
                    toggle.endMeasure();
                    assertFalse(ClientOptimizationConfig.gpuEntityModels);
                    assertFalse(ClientOptimizationConfig.gpuItems);
                    assertFalse(ClientOptimizationConfig.gpuParticles);
                    assertFalse(ClientOptimizationConfig.gpuEntityModelsWithShaders);
                    assertFalse(ClientOptimizationConfig.gpuParticlesWithShaders);
                }
            }
        } finally {
            ClientOptimizationConfig.gpuEntityModels = base;
            ClientOptimizationConfig.gpuItems = items;
            ClientOptimizationConfig.gpuParticles = particles;
            ClientOptimizationConfig.gpuEntityModelsWithShaders = shaderEntities;
            ClientOptimizationConfig.gpuParticlesWithShaders = shaderParticles;
        }
    }
}
