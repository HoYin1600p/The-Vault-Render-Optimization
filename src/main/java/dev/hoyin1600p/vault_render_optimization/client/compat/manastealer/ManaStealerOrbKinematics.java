package dev.hoyin1600p.vault_render_optimization.client.compat.manastealer;

public final class ManaStealerOrbKinematics {
    private static final double UNIT_DOUBLE = 0x1.0p-53;

    private ManaStealerOrbKinematics() {
    }

    public static Sample sample(long seed, double minimumSpeed, double maximumSpeed) {
        long first = mix64(seed);
        long second = mix64(first);
        long third = mix64(second);

        double vertical = Math.fma(unit(first), 2.0D, -1.0D);
        double angle = unit(second) * Math.PI * 2.0D;
        double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - vertical * vertical));
        double speed = Math.fma(unit(third), maximumSpeed - minimumSpeed, minimumSpeed);
        return new Sample(
                horizontal * Math.cos(angle),
                vertical,
                horizontal * Math.sin(angle),
                speed
        );
    }

    public static int lifetimeTicks(double radius, double speedPerTick) {
        if (!(radius > 0.0D) || !(speedPerTick > 0.0D)) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(radius / speedPerTick));
    }

    public static float scale(float progress) {
        float bounded = Math.max(0.0F, Math.min(1.0F, progress));
        return Math.fma(-2.375F, bounded, 2.5F);
    }

    private static double unit(long value) {
        return (value >>> 11) * UNIT_DOUBLE;
    }

    private static long mix64(long value) {
        value += 0x9E3779B97F4A7C15L;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    public record Sample(double x, double y, double z, double speedPerTick) {
    }
}
