package dev.hoyin1600p.vault_render_optimization.client.particle;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

class ParticleRandomTest {
    @Test
    void sameSeedGivesExactlyJavaUtilRandomsSequence() {
        for (long seed : new long[]{0, 1, -1, 42, 1600, Long.MAX_VALUE, System.nanoTime()}) {
            Random expected = new Random(seed);
            Random actual = new ParticleRandom(seed);
            for (int i = 0; i < 2_000; i++) {
                switch (i % 8) {
                    case 0 -> assertEquals(expected.nextInt(), actual.nextInt());
                    case 1 -> assertEquals(expected.nextInt(97), actual.nextInt(97));
                    case 2 -> assertEquals(expected.nextFloat(), actual.nextFloat());
                    case 3 -> assertEquals(expected.nextDouble(), actual.nextDouble());
                    case 4 -> assertEquals(expected.nextGaussian(), actual.nextGaussian());
                    case 5 -> assertEquals(expected.nextLong(), actual.nextLong());
                    case 6 -> assertEquals(expected.nextBoolean(), actual.nextBoolean());
                    default -> assertEquals(expected.nextInt(1 << 20), actual.nextInt(1 << 20));
                }
            }
        }
    }

    @Test
    void reseedingResetsLikeJavaUtilRandom() {
        Random expected = new Random(7);
        ParticleRandom actual = new ParticleRandom(7);
        expected.nextGaussian();
        actual.nextGaussian(); // leaves a cached second gaussian
        expected.setSeed(99);
        actual.setSeed(99);
        for (int i = 0; i < 100; i++) assertEquals(expected.nextGaussian(), actual.nextGaussian());
    }

    @Test
    void eachThreadHasItsOwnGenerator() throws Exception {
        ParticleRandom main = ParticleRandom.current();
        assertSame(main, ParticleRandom.current());
        ParticleRandom[] other = new ParticleRandom[1];
        Thread thread = new Thread(() -> other[0] = ParticleRandom.current());
        thread.start();
        thread.join();
        assertNotNull(other[0]);
        assertNotSame(main, other[0]);
    }
}
