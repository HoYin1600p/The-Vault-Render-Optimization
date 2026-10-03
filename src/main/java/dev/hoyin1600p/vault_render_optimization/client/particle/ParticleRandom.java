package dev.hoyin1600p.vault_render_optimization.client.particle;

import java.util.Random;

/**
 * {@link java.util.Random}'s exact 48-bit LCG without the atomic seed. Vanilla gives every particle
 * its own {@code new Random()}: a seed-uniquifier CAS, a {@code nanoTime} call and an
 * {@code AtomicLong} per particle, then a CAS on every draw. Particles are created and ticked on the
 * client thread, so one generator per creating thread gives the same uniform, independent draws
 * without that work. The same seed produces the same sequence as {@code java.util.Random}.
 * Not thread-safe by design; each thread gets its own instance.
 */
public final class ParticleRandom extends Random {
    private static final long MULTIPLIER = 0x5DEECE66DL;
    private static final long ADDEND = 0xBL;
    private static final long MASK = (1L << 48) - 1;
    private static final ThreadLocal<ParticleRandom> PER_THREAD = ThreadLocal.withInitial(
            () -> new ParticleRandom(System.nanoTime() ^ Thread.currentThread().getId() * 0x9E3779B97F4A7C15L));

    private long state;
    private boolean ready;
    private double nextNextGaussian;
    private boolean haveNextNextGaussian;

    public ParticleRandom(long seed) {
        super(seed); // Random's constructor calls setSeed(seed) for subclasses.
        ready = true;
    }

    public static ParticleRandom current() {
        return PER_THREAD.get();
    }

    @Override
    public synchronized void setSeed(long seed) {
        state = (seed ^ MULTIPLIER) & MASK;
        haveNextNextGaussian = false;
        if (ready) super.setSeed(seed);
    }

    /** {@link Random#nextGaussian()}'s documented polar method, without its monitor. */
    @Override
    public double nextGaussian() {
        if (haveNextNextGaussian) {
            haveNextNextGaussian = false;
            return nextNextGaussian;
        }
        double v1, v2, s;
        do {
            v1 = 2 * nextDouble() - 1;
            v2 = 2 * nextDouble() - 1;
            s = v1 * v1 + v2 * v2;
        } while (s >= 1 || s == 0);
        double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
        nextNextGaussian = v2 * multiplier;
        haveNextNextGaussian = true;
        return v1 * multiplier;
    }

    @Override
    protected int next(int bits) {
        long next = (state * MULTIPLIER + ADDEND) & MASK;
        state = next;
        return (int) (next >>> (48 - bits));
    }
}
