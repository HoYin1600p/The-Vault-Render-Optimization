package dev.hoyin1600p.vault_render_optimization.client.compat.manastealer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ManaStealerOrbKinematicsTest {
    @Test
    void samplesDeterministicUniformSphereDirectionsWithinSpeedBounds() {
        double sumX = 0.0D;
        double sumY = 0.0D;
        double sumZ = 0.0D;
        int samples = 10_000;

        for (long seed = 0; seed < samples; seed++) {
            ManaStealerOrbKinematics.Sample sample = ManaStealerOrbKinematics.sample(seed, 0.125D, 0.2D);
            double length = Math.sqrt(
                    sample.x() * sample.x()
                            + sample.y() * sample.y()
                            + sample.z() * sample.z()
            );
            assertEquals(1.0D, length, 1.0E-12D);
            assertTrue(sample.speedPerTick() >= 0.125D);
            assertTrue(sample.speedPerTick() <= 0.2D);
            assertEquals(sample, ManaStealerOrbKinematics.sample(seed, 0.125D, 0.2D));
            sumX += sample.x();
            sumY += sample.y();
            sumZ += sample.z();
        }

        assertEquals(0.0D, sumX / samples, 0.02D);
        assertEquals(0.0D, sumY / samples, 0.02D);
        assertEquals(0.0D, sumZ / samples, 0.02D);
    }

    @Test
    void sixBlockTripsMatchRequestedSpeedRange() {
        assertEquals(48, ManaStealerOrbKinematics.lifetimeTicks(6.0D, 0.125D));
        assertEquals(30, ManaStealerOrbKinematics.lifetimeTicks(6.0D, 0.2D));
    }

    @Test
    void scaleDoublesOuterSizeAndIncreasesCenterSizeByTwentyFivePercent() {
        assertEquals(2.5F, ManaStealerOrbKinematics.scale(0.0F), 1.0E-6F);
        assertEquals(1.3125F, ManaStealerOrbKinematics.scale(0.5F), 1.0E-6F);
        assertEquals(0.125F, ManaStealerOrbKinematics.scale(1.0F), 1.0E-6F);
        assertEquals(2.5F, ManaStealerOrbKinematics.scale(-1.0F), 1.0E-6F);
        assertEquals(0.125F, ManaStealerOrbKinematics.scale(2.0F), 1.0E-6F);
    }
}
