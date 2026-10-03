package dev.hoyin1600p.vault_render_optimization.client.particle;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.EvictingQueue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ParticleListCompactionTest {
    /** Stand-in particle: dies at a fixed age, and may kill another particle while ticking. */
    private static final class P {
        final int id;
        int age;
        final int lifetime;
        boolean alive = true;
        P victim;
        P(int id, int lifetime) { this.id = id; this.lifetime = lifetime; }
        void tick(List<String> log) {
            log.add("tick " + id);
            if (++age >= lifetime) alive = false;
            if (victim != null) victim.alive = false;
        }
    }

    private static EvictingQueue<P> queue(List<P> particles) {
        EvictingQueue<P> queue = EvictingQueue.create(16384);
        queue.addAll(particles);
        return queue;
    }

    /** Vanilla 1.18.2 tickParticleList. */
    private static void vanilla(EvictingQueue<P> queue, List<String> log) {
        Iterator<P> iterator = queue.iterator();
        while (iterator.hasNext()) {
            P particle = iterator.next();
            particle.tick(log);
            if (!particle.alive) {
                log.add("dead " + particle.id);
                iterator.remove();
            }
        }
    }

    private static List<P> world(long seed, List<P> copyTarget) {
        Random random = new Random(seed);
        List<P> list = new ArrayList<>();
        for (int i = 0; i < 2_000; i++) list.add(new P(i, 1 + random.nextInt(6)));
        for (int i = 0; i < 40; i++) list.get(random.nextInt(list.size())).victim = list.get(random.nextInt(list.size()));
        return list;
    }

    private static List<P> copy(List<P> source) {
        List<P> out = new ArrayList<>();
        for (P p : source) out.add(new P(p.id, p.lifetime));
        for (int i = 0; i < source.size(); i++) {
            if (source.get(i).victim != null) out.get(i).victim = out.get(source.get(i).victim.id);
        }
        return out;
    }

    @Test
    void matchesVanillaOrderSurvivorsAndDeathCallbacksOverManyTicks() {
        for (long seed = 0; seed < 20; seed++) {
            List<P> original = world(seed, null);
            EvictingQueue<P> expected = queue(copy(original));
            EvictingQueue<P> actual = queue(copy(original));
            for (int tick = 0; tick < 8; tick++) {
                List<String> vanillaLog = new ArrayList<>();
                List<String> compactLog = new ArrayList<>();
                vanilla(expected, vanillaLog);
                ParticleListCompaction.tick(actual, p -> p.tick(compactLog), p -> p.alive,
                        p -> compactLog.add("dead " + p.id));
                assertEquals(vanillaLog, compactLog, "seed " + seed + " tick " + tick);
                assertEquals(expected.stream().map(p -> p.id).toList(), actual.stream().map(p -> p.id).toList());
            }
        }
    }

    @Test
    void noDeathsLeavesTheQueueUntouched() {
        EvictingQueue<P> queue = queue(List.of(new P(1, 100), new P(2, 100)));
        assertEquals(0, ParticleListCompaction.tick(queue, p -> p.age++, p -> p.alive, p -> fail()));
        assertEquals(2, queue.size());
    }
}
