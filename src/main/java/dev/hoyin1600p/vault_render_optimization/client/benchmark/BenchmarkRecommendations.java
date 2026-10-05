package dev.hoyin1600p.vault_render_optimization.client.benchmark;

import dev.hoyin1600p.vault_render_optimization.util.ConfigKeys;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Pure resolution of measured verdicts and the switches required to realize them. */
public final class BenchmarkRecommendations {
    public record Resolved(Map<String, Boolean> states, Set<String> required) {
        public Resolved {
            states = Map.copyOf(states);
            required = Set.copyOf(required);
        }
    }

    private BenchmarkRecommendations() { }

    public static Resolved resolve(Map<String, Boolean> original,
                                    Map<String, BenchmarkVerdict.Recommendation> verdicts) {
        Map<String, Boolean> states = new LinkedHashMap<>(original);
        verdicts.forEach((id, verdict) -> {
            if (verdict != BenchmarkVerdict.Recommendation.NO_DIFFERENCE) {
                states.put(id, verdict == BenchmarkVerdict.Recommendation.ON);
            }
        });
        Set<String> required = new LinkedHashSet<>();
        boolean shaders = original.containsKey(ConfigKeys.GPU_ENTITY_MODELS_WITH_SHADERS);
        if (on(states, ConfigKeys.GPU_ITEMS) && shaders) {
            require(states, required, ConfigKeys.GPU_ENTITY_MODELS_WITH_SHADERS);
        }
        if (on(states, ConfigKeys.GPU_PARTICLES_WITH_SHADERS)) require(states, required, ConfigKeys.GPU_PARTICLES);
        if (on(states, ConfigKeys.GPU_PARTICLES) && shaders && !on(states, ConfigKeys.GPU_ENTITY_MODELS_WITH_SHADERS)) {
            require(states, required, ConfigKeys.GPU_PARTICLES_WITH_SHADERS);
        }
        if (on(states, ConfigKeys.GPU_ITEMS) || on(states, ConfigKeys.GPU_PARTICLES)
                || on(states, ConfigKeys.GPU_ENTITY_MODELS_WITH_SHADERS)
                || on(states, ConfigKeys.GPU_PARTICLES_WITH_SHADERS)) {
            require(states, required, ConfigKeys.GPU_ENTITY_MODELS);
        }
        return new Resolved(states, required);
    }

    private static boolean on(Map<String, Boolean> states, String id) {
        return Boolean.TRUE.equals(states.get(id));
    }

    private static void require(Map<String, Boolean> states, Set<String> required, String id) {
        if (!on(states, id)) required.add(id);
        states.put(id, true);
    }
}
