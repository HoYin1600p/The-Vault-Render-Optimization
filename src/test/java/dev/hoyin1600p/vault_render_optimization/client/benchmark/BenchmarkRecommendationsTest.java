package dev.hoyin1600p.vault_render_optimization.client.benchmark;

import static org.junit.jupiter.api.Assertions.*;
import static dev.hoyin1600p.vault_render_optimization.client.benchmark.BenchmarkVerdict.Recommendation.*;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BenchmarkRecommendationsTest {
    @Test void noDifferencePreservesOriginalValuesAndDoesNotMutateInputs() {
        Map<String, Boolean> original = Map.of("gpu_entity_models", true, "gpu_items", false, "gpu_particles", false);
        var result = BenchmarkRecommendations.resolve(original, Map.of("gpu_entity_models", NO_DIFFERENCE,
                "gpu_items", NO_DIFFERENCE, "gpu_particles", NO_DIFFERENCE));
        assertEquals(original, result.states());
        assertTrue(result.required().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> result.states().put("gpu_items", true));
    }

    @Test void itemsOnOverridesBaseOffAndNotesRequirement() {
        var result = BenchmarkRecommendations.resolve(
                Map.of("gpu_entity_models", false, "gpu_items", false, "gpu_particles", false),
                Map.of("gpu_entity_models", OFF, "gpu_items", ON, "gpu_particles", OFF));
        assertEquals(Map.of("gpu_entity_models", true, "gpu_items", true, "gpu_particles", false), result.states());
        assertEquals(Set.of("gpu_entity_models"), result.required());
    }

    @Test void originalDependentOnAlsoRequiresBaseWhenVerdictIsNoDifference() {
        var result = BenchmarkRecommendations.resolve(Map.of("gpu_entity_models", false, "gpu_particles", true),
                Map.of("gpu_entity_models", OFF, "gpu_particles", NO_DIFFERENCE));
        assertTrue(result.states().get("gpu_entity_models"));
        assertTrue(result.states().get("gpu_particles"));
    }

    @Test void shaderDependenciesAreResolvedForItemsParticlesAndEntities() {
        Map<String, Boolean> original = Map.of("gpu_entity_models", false, "gpu_items", false,
                "gpu_particles", false, "gpu_entity_models_with_shaders", false, "gpu_particles_with_shaders", false);
        var items = BenchmarkRecommendations.resolve(original, Map.of("gpu_items", ON));
        assertTrue(items.states().get("gpu_entity_models"));
        assertTrue(items.states().get("gpu_entity_models_with_shaders"));
        var particles = BenchmarkRecommendations.resolve(original, Map.of("gpu_particles_with_shaders", ON));
        assertTrue(particles.states().get("gpu_entity_models"));
        assertTrue(particles.states().get("gpu_particles"));
        assertFalse(particles.states().get("gpu_entity_models_with_shaders"));
        var entities = BenchmarkRecommendations.resolve(original, Map.of("gpu_entity_models_with_shaders", ON));
        assertTrue(entities.states().get("gpu_entity_models"));
        var plainParticles = BenchmarkRecommendations.resolve(original, Map.of("gpu_particles", ON));
        assertTrue(plainParticles.states().get("gpu_particles_with_shaders"));
    }
}
