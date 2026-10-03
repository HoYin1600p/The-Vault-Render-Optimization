package dev.hoyin1600p.vault_render_optimization.client.particle;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ParticleProviderCacheTest {
    @Test
    void lookupHappensOncePerTypeUntilProvidersChange() {
        Map<String, String> providers = new HashMap<>(Map.of("nova", "novaProvider", "cloud", "cloudProvider"));
        ParticleProviderCache<String, String> cache = new ParticleProviderCache<>();
        AtomicInteger lookups = new AtomicInteger();
        for (int i = 0; i < 1_000; i++) {
            assertEquals("novaProvider", cache.get("nova", providers.size(), key -> { lookups.incrementAndGet(); return providers.get(key); }));
        }
        assertEquals(1, lookups.get());

        cache.invalidate(); // register() replaced a provider
        providers.put("nova", "replacement");
        assertEquals("replacement", cache.get("nova", providers.size(), providers::get));

        providers.put("frost", "frostProvider"); // a mod wrote to the map directly: size changed
        assertEquals("frostProvider", cache.get("frost", providers.size(), providers::get));
    }

    @Test
    void missingProvidersAreNotCached() {
        Map<String, String> providers = new HashMap<>();
        ParticleProviderCache<String, String> cache = new ParticleProviderCache<>();
        assertNull(cache.get("late", providers.size(), providers::get));
        assertEquals(0, cache.size());
        providers.put("late", "lateProvider");
        cache.invalidate();
        assertEquals("lateProvider", cache.get("late", providers.size(), providers::get));
    }
}
