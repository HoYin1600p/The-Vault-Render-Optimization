package dev.hoyin1600p.vault_render_optimization.client.compat.manastealer;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;

public final class ManaStealerVisualController {
    private static final Map<Integer, Tracker> TRACKERS = new HashMap<>();

    private ManaStealerVisualController() {
    }

    public static boolean canReplace() {
        return ManaStealerVisualConfig.active() && ManaStealerOrbParticle.spritesReady();
    }

    public static boolean canPreview() {
        return ManaStealerOrbParticle.spritesReady();
    }

    public static void maintain(Entity entity, float configuredRadius) {
        if (!(entity.level instanceof ClientLevel level) || configuredRadius <= 0.0F) {
            return;
        }

        maintain(entity.getId(), level, entity.getX(), entity.getY(), entity.getZ(), configuredRadius, false);
    }

    static void maintainPreview(
            int sourceId,
            ClientLevel level,
            double centerX,
            double centerY,
            double centerZ,
            float configuredRadius
    ) {
        if (configuredRadius <= 0.0F || !canPreview()) {
            return;
        }
        maintain(sourceId, level, centerX, centerY, centerZ, configuredRadius, true);
    }

    private static void maintain(
            int sourceId,
            ClientLevel level,
            double centerX,
            double centerY,
            double centerZ,
            float configuredRadius,
            boolean preview
    ) {
        ManaStealerDrainStreamController.maintain(
                sourceId,
                level,
                centerX,
                centerY,
                centerZ,
                configuredRadius,
                preview
        );
        Tracker tracker = TRACKERS.compute(sourceId, (id, existing) -> {
            if (existing == null || existing.level != level) {
                return new Tracker(level, sourceId);
            }
            return existing;
        });
        long gameTime = level.getGameTime();
        tracker.live.removeIf(particle -> !particle.isAlive() || !particle.tickedRecently(gameTime));

        int target = targetPopulation(Minecraft.getInstance().options.particles);
        int missing = target - tracker.live.size();
        int spawnCount = Math.min(Math.max(0, missing), ManaStealerVisualConfig.maxSpawnsPerTick());
        for (int index = 0; index < spawnCount; index++) {
            spawn(centerX, centerY, centerZ, configuredRadius, tracker);
        }
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ClientLevel currentLevel = Minecraft.getInstance().level;
        if (currentLevel == null) {
            TRACKERS.clear();
            return;
        }
        long gameTime = currentLevel.getGameTime();
        TRACKERS.entrySet().removeIf(entry -> {
            Tracker tracker = entry.getValue();
            if (tracker.level != currentLevel) {
                return true;
            }
            tracker.live.removeIf(
                    particle -> !particle.isAlive() || !particle.tickedRecently(gameTime)
            );
            return tracker.live.isEmpty();
        });
    }

    static void onParticleRemoved(int sourceEntityId, ManaStealerOrbParticle particle) {
        Tracker tracker = TRACKERS.get(sourceEntityId);
        if (tracker == null) {
            return;
        }
        tracker.live.remove(particle);
        if (tracker.live.isEmpty()) {
            TRACKERS.remove(sourceEntityId, tracker);
        }
    }

    static int trackedPopulation(int sourceEntityId) {
        Tracker tracker = TRACKERS.get(sourceEntityId);
        return tracker == null ? 0 : tracker.live.size();
    }

    private static int targetPopulation(ParticleStatus status) {
        int quality = switch (status) {
            case DECREASED -> ManaStealerPopulationPolicy.DECREASED;
            case MINIMAL -> ManaStealerPopulationPolicy.MINIMAL;
            default -> ManaStealerPopulationPolicy.ALL;
        };
        return ManaStealerPopulationPolicy.target(
                quality,
                ManaStealerVisualConfig.allPopulation(),
                ManaStealerVisualConfig.decreasedPopulation(),
                ManaStealerVisualConfig.minimalPopulation()
        );
    }

    private static void spawn(
            double centerX,
            double centerY,
            double centerZ,
            float radius,
            Tracker tracker
    ) {
        long sequence = tracker.sequence++;
        long seed = ((long) tracker.entityId << 32) ^ sequence;
        ManaStealerOrbKinematics.Sample sample = ManaStealerOrbKinematics.sample(
                seed,
                ManaStealerVisualConfig.minimumSpeed(),
                ManaStealerVisualConfig.maximumSpeed()
        );
        double startX = Math.fma(sample.x(), radius, centerX);
        double startY = Math.fma(sample.y(), radius, centerY);
        double startZ = Math.fma(sample.z(), radius, centerZ);
        int lifetime = ManaStealerOrbKinematics.lifetimeTicks(radius, sample.speedPerTick());
        ManaStealerOrbParticle particle = new ManaStealerOrbParticle(
                tracker.level,
                tracker.entityId,
                startX, startY, startZ,
                centerX, centerY, centerZ,
                lifetime,
                ManaStealerVisualConfig.outerDiameter(),
                ManaStealerVisualConfig.innerRatio()
        );
        tracker.live.add(particle);
        Minecraft.getInstance().particleEngine.add(particle);
    }

    private static final class Tracker {
        private final ClientLevel level;
        private final int entityId;
        private final Set<ManaStealerOrbParticle> live = Collections.newSetFromMap(new IdentityHashMap<>());
        private long sequence;

        private Tracker(ClientLevel level, int entityId) {
            this.level = level;
            this.entityId = entityId;
        }
    }
}
