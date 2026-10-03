package dev.hoyin1600p.vault_render_optimization.client.particle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * One-pass replacement for {@code ParticleEngine.tickParticleList}'s per-particle
 * {@code Iterator.remove()}. The particle queues are Guava {@code EvictingQueue}s over an
 * {@code ArrayDeque}, where each interior removal shifts the array, so a tick in which thousands of
 * particles die costs O(n * deaths). Here every particle ticks in the same order and is checked right
 * after its own tick, exactly as vanilla does; the death callback (particle-group counts) runs at the
 * same moment, so spawns later in the same tick see the same group limits. Only the physical removal
 * is deferred and done once, in order. Nothing is skipped: every particle still ticks.
 */
public final class ParticleListCompaction {
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    private static final class Scratch {
        boolean[] dead = new boolean[256];
        final ArrayList<Object> survivors = new ArrayList<>();
    }

    private ParticleListCompaction() {
    }

    /** @return the number of elements removed */
    @SuppressWarnings("unchecked")
    public static <T> int tick(Collection<T> particles, Consumer<T> tick, Predicate<T> alive, Consumer<T> onDead) {
        int size = particles.size();
        if (size == 0) return 0;
        Scratch scratch = SCRATCH.get();
        if (scratch.dead.length < size) scratch.dead = new boolean[Math.max(size, scratch.dead.length * 2)];
        boolean[] dead = scratch.dead;
        int index = 0, deaths = 0;
        for (T particle : particles) {
            tick.accept(particle);
            boolean died = !alive.test(particle);
            dead[index++] = died;
            if (died) {
                deaths++;
                onDead.accept(particle);
            }
        }
        if (deaths == 0) return 0;
        ArrayList<Object> survivors = scratch.survivors;
        try {
            index = 0;
            for (T particle : particles) {
                if (!dead[index++]) survivors.add(particle);
            }
            particles.clear();
            particles.addAll((Collection<? extends T>) survivors);
        } finally {
            survivors.clear();
            if (size > 65_536) survivors.trimToSize();
        }
        return deaths;
    }
}
