package dev.hoyin1600p.vault_render_optimization.client.particle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Identity cache in front of Forge's {@code ParticleEngine.makeParticle} lookup, which resolves every
 * spawn through {@code Registry.PARTICLE_TYPE.getKey(type)} and a {@code ResourceLocation} hash lookup.
 * Entries are dropped whenever a provider is registered, and the whole cache is discarded if the
 * provider map's size changes (a guard against mods writing to it directly). Only non-null
 * providers are cached, so a type without a provider still resolves exactly as vanilla does.
 */
public final class ParticleProviderCache<K, V> {
    private final Map<K, V> cache = new ConcurrentHashMap<>();
    private volatile int knownSize = -1;

    public V get(K type, int providerCount, Function<K, V> vanillaLookup) {
        if (providerCount != knownSize) {
            cache.clear();
            knownSize = providerCount;
        }
        V provider = cache.get(type);
        if (provider == null) {
            provider = vanillaLookup.apply(type);
            if (provider != null) cache.put(type, provider);
        }
        return provider;
    }

    public void invalidate() {
        cache.clear();
        knownSize = -1;
    }

    int size() {
        return cache.size();
    }
}
