package dev.hoyin1600p.vault_render_optimization.client.compat.manastealer;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

final class ManaStealerDrainStreamController {
    private static final Map<StreamKey, StreamState> STREAMS = new LinkedHashMap<>();

    private ManaStealerDrainStreamController() {
    }

    static void maintain(
            int sourceId,
            ClientLevel level,
            double centerX,
            double centerY,
            double centerZ,
            float radius,
            boolean preview
    ) {
        if (!ManaStealerVisualConfig.drainStreamActive() || radius <= 0.0F) {
            removeSource(level, sourceId);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        long gameTime = level.getGameTime();
        Vec3 center = new Vec3(centerX, centerY, centerZ);
        for (Player player : level.players()) {
            if (!eligible(player, minecraft.player, preview)
                    || !ManaStealerDrainStreamPolicy.insideSphere(
                    player.getX(), player.getY(), player.getZ(),
                    centerX, centerY, centerZ, radius)) {
                continue;
            }
            StreamKey key = new StreamKey(sourceId, player.getUUID());
            StreamState existing = STREAMS.get(key);
            if (existing == null || existing.level != level) {
                existing = new StreamState(level, sourceId, player.getUUID());
                STREAMS.put(key, existing);
            }
            existing.center = center;
            existing.radius = radius;
            existing.lastSeenGameTime = gameTime;
        }

        STREAMS.entrySet().removeIf(entry -> {
            StreamState stream = entry.getValue();
            return stream.level == level
                    && stream.sourceId == sourceId
                    && stream.lastSeenGameTime != gameTime;
        });
    }

    static Collection<StreamState> streams() {
        return STREAMS.values();
    }

    static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !ManaStealerVisualConfig.drainStreamActive()) {
            STREAMS.clear();
            return;
        }
        long gameTime = level.getGameTime();
        STREAMS.entrySet().removeIf(entry -> {
            StreamState stream = entry.getValue();
            return stream.level != level
                    || stream.lastSeenGameTime < gameTime - 1L
                    || level.getPlayerByUUID(stream.playerId) == null;
        });
    }

    private static boolean eligible(Player player, Player localPlayer, boolean preview) {
        if (!player.isAlive()) {
            return false;
        }
        if (preview) {
            return player == localPlayer;
        }
        return !player.isSpectator() && !player.isCreative();
    }

    private static void removeSource(ClientLevel level, int sourceId) {
        STREAMS.entrySet().removeIf(entry -> {
            StreamState stream = entry.getValue();
            return stream.level == level && stream.sourceId == sourceId;
        });
    }

    static final class StreamState {
        private final ClientLevel level;
        private final int sourceId;
        private final UUID playerId;
        private final long visualSeed;
        private Vec3 center = Vec3.ZERO;
        private float radius;
        private long lastSeenGameTime;

        private StreamState(ClientLevel level, int sourceId, UUID playerId) {
            this.level = level;
            this.sourceId = sourceId;
            this.playerId = playerId;
            this.visualSeed = ((long) sourceId << 32)
                    ^ playerId.getMostSignificantBits()
                    ^ Long.rotateLeft(playerId.getLeastSignificantBits(), 17);
        }

        ClientLevel level() {
            return this.level;
        }

        UUID playerId() {
            return this.playerId;
        }

        long visualSeed() {
            return this.visualSeed;
        }

        Vec3 center() {
            return this.center;
        }

        float radius() {
            return this.radius;
        }
    }

    private record StreamKey(int sourceId, UUID playerId) {
    }
}
