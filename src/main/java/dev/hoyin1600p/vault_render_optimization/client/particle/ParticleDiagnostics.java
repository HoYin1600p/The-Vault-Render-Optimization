package dev.hoyin1600p.vault_render_optimization.client.particle;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

public final class ParticleDiagnostics {
    private static final LongAdder VRO_RENDERER_WRITES = new LongAdder();
    private static final LongAdder VRO_PORTABLE_WRITES = new LongAdder();
    private static final LongAdder RENDERER_PASSTHROUGHS = new LongAdder();
    private static final LongAdder PARTICLE_LIGHT_HITS = new LongAdder();
    private static final LongAdder SHARED_LIGHT_HITS = new LongAdder();
    private static final LongAdder LIGHT_LOOKUPS = new LongAdder();
    private static final LongAdder EMPTY_RENDER_SKIPS = new LongAdder();
    private static final ConcurrentHashMap<String, LongAdder> BILLBOARD_CLASSES =
            new ConcurrentHashMap<>();

    private static volatile long renderCalls;
    private static volatile long tickCalls;
    private static volatile long lastRenderNanos;
    private static volatile long totalRenderNanos;
    private static volatile long lastTickNanos;
    private static volatile long totalTickNanos;
    private static volatile int queuedParticles;
    private static volatile Map<String, Integer> queuedClasses = Map.of();

    private ParticleDiagnostics() {
    }

    public static boolean enabled() {
        return ClientOptimizationConfig.particleDiagnostics;
    }

    public static long beginRender(Map<ParticleRenderType, Queue<Particle>> particles) {
        if (!enabled()) {
            return 0L;
        }
        TreeMap<String, Integer> byClass = new TreeMap<>();
        int total = 0;
        for (Queue<Particle> queue : particles.values()) {
            total += queue.size();
            for (Particle particle : queue) {
                byClass.merge(particle.getClass().getName(), 1, Integer::sum);
            }
        }
        queuedParticles = total;
        queuedClasses = Collections.unmodifiableMap(byClass);
        return System.nanoTime();
    }

    public static void endRender(long started) {
        if (started == 0L) {
            return;
        }
        long elapsed = System.nanoTime() - started;
        lastRenderNanos = elapsed;
        totalRenderNanos += elapsed;
        renderCalls++;
    }

    public static long beginTick() {
        return enabled() ? System.nanoTime() : 0L;
    }

    public static void endTick(long started) {
        if (started == 0L) {
            return;
        }
        long elapsed = System.nanoTime() - started;
        lastTickNanos = elapsed;
        totalTickNanos += elapsed;
        tickCalls++;
    }

    public static void recordVroBillboard(boolean rendererWriter, Class<?> particleClass) {
        if (!enabled()) {
            return;
        }
        (rendererWriter ? VRO_RENDERER_WRITES : VRO_PORTABLE_WRITES).increment();
        BILLBOARD_CLASSES.computeIfAbsent(particleClass.getName(), ignored -> new LongAdder()).increment();
    }

    public static void recordRendererPassthrough() {
        if (enabled()) {
            RENDERER_PASSTHROUGHS.increment();
        }
    }

    public static void recordParticleLightHit() {
        if (enabled()) {
            PARTICLE_LIGHT_HITS.increment();
        }
    }

    public static void recordSharedLightHit() {
        if (enabled()) {
            SHARED_LIGHT_HITS.increment();
        }
    }

    public static void recordLightLookup() {
        if (enabled()) {
            LIGHT_LOOKUPS.increment();
        }
    }

    public static void recordEmptyRenderSkip() {
        if (enabled()) {
            EMPTY_RENDER_SKIPS.increment();
        }
    }

    public static Snapshot snapshot() {
        TreeMap<String, Long> billboardClasses = new TreeMap<>();
        BILLBOARD_CLASSES.forEach((name, count) -> billboardClasses.put(name, count.sum()));
        return new Snapshot(
                enabled(),
                ParticleOptimizationState.resolvedBillboardOwner(),
                ParticleOptimizationState.rendererAvailable(),
                queuedParticles,
                queuedClasses,
                Map.copyOf(billboardClasses),
                VRO_RENDERER_WRITES.sum(),
                VRO_PORTABLE_WRITES.sum(),
                RENDERER_PASSTHROUGHS.sum(),
                PARTICLE_LIGHT_HITS.sum(),
                SHARED_LIGHT_HITS.sum(),
                LIGHT_LOOKUPS.sum(),
                EMPTY_RENDER_SKIPS.sum(),
                renderCalls,
                lastRenderNanos,
                renderCalls == 0L ? 0L : totalRenderNanos / renderCalls,
                tickCalls,
                lastTickNanos,
                tickCalls == 0L ? 0L : totalTickNanos / tickCalls
        );
    }

    public static void reset() {
        VRO_RENDERER_WRITES.reset();
        VRO_PORTABLE_WRITES.reset();
        RENDERER_PASSTHROUGHS.reset();
        PARTICLE_LIGHT_HITS.reset();
        SHARED_LIGHT_HITS.reset();
        LIGHT_LOOKUPS.reset();
        EMPTY_RENDER_SKIPS.reset();
        BILLBOARD_CLASSES.clear();
        renderCalls = 0L;
        tickCalls = 0L;
        lastRenderNanos = 0L;
        totalRenderNanos = 0L;
        lastTickNanos = 0L;
        totalTickNanos = 0L;
        queuedParticles = 0;
        queuedClasses = Map.of();
        ParticleSharedLightCache.clearCurrentThread();
    }

    public record Snapshot(
            boolean enabled,
            String billboardOwner,
            boolean rendererAvailable,
            int queuedParticles,
            Map<String, Integer> queuedClasses,
            Map<String, Long> billboardClasses,
            long vroRendererWrites,
            long vroPortableWrites,
            long rendererPassthroughs,
            long particleLightHits,
            long sharedLightHits,
            long lightLookups,
            long emptyRenderSkips,
            long renderCalls,
            long lastRenderNanos,
            long averageRenderNanos,
            long tickCalls,
            long lastTickNanos,
            long averageTickNanos
    ) {
    }
}
