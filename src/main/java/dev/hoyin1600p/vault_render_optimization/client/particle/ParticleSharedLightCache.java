package dev.hoyin1600p.vault_render_optimization.client.particle;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;

public final class ParticleSharedLightCache {
    private static final int MAX_POSITIONS_PER_TICK = 8192;
    private static final ThreadLocal<State> LOCAL = ThreadLocal.withInitial(State::new);

    private ParticleSharedLightCache() {
    }

    public static int get(ClientLevel level, long tick, int x, int y, int z) {
        State state = LOCAL.get();
        if (state.level != level || state.tick != tick) {
            state.level = level;
            state.tick = tick;
            state.values.clear();
        }

        long position = BlockPos.asLong(x, y, z);
        int cached = state.values.get(position);
        if (cached != Integer.MIN_VALUE) {
            ParticleDiagnostics.recordSharedLightHit();
            return cached;
        }

        state.position.set(x, y, z);
        int light = level.hasChunkAt(state.position)
                ? LevelRenderer.getLightColor(level, state.position)
                : 0;
        if (state.values.size() < MAX_POSITIONS_PER_TICK) {
            state.values.put(position, light);
        }
        ParticleDiagnostics.recordLightLookup();
        return light;
    }

    public static void clearCurrentThread() {
        LOCAL.remove();
    }

    private static final class State {
        private final Long2IntOpenHashMap values = new Long2IntOpenHashMap();
        private final BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        private ClientLevel level;
        private long tick = Long.MIN_VALUE;

        private State() {
            values.defaultReturnValue(Integer.MIN_VALUE);
        }
    }
}
