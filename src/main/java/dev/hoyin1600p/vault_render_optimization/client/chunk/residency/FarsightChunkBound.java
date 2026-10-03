package dev.hoyin1600p.vault_render_optimization.client.chunk.residency;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.lang.ref.WeakReference;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraftforge.event.TickEvent;

/**
 * Bounds the chunks Farsight 1.9 keeps resident on the client. Ported from VH Accelerator
 * (HoYin1600p, commits e0a95d34/ccd9a179); VRO declares ownership with
 * {@code META-INF/vro-features/farsight-chunk-bound}, and VH Accelerator then leaves the bound to VRO.
 *
 * <p>Farsight cancels {@code ClientPacketListener.handleForgetLevelChunk} at HEAD, so while the
 * client stays in one level no chunk is ever dropped, no light {@code DataLayer} is released and
 * Embeddium never removes the chunk's render sections. Once a second, chunks the server has already
 * told the client to forget and that are farther than {@code max(server radius, render distance) + 1}
 * from the player are forgotten exactly as the vanilla handler would:
 * {@link ClientChunkCache#drop(int, int)} (which posts {@code ChunkEvent.Unload}), the queued light
 * release, and the Embeddium/Rubidium notification. Chunks inside that radius stay, so Farsight still
 * shows terrain out to the render distance.
 *
 * <p>Only a chunk the server forgot may be dropped. The server tracks the player behind the client
 * (most of all when flying), and resends a chunk only after it has forgotten it; dropping one it still
 * counts as sent leaves an empty, unwalkable hole until the player relogs (seen in a vault hallway,
 * found by VH Accelerator, commit 9909763).
 *
 * <p>Unlike VRO's optimizations this is a leak fix and ignores Compare Mode: VH Accelerator yields
 * whenever the marker is present, so switching it off here leaves Farsight unbounded.
 * All state lives on the client thread and resets when the level changes.
 */
public final class FarsightChunkBound {
    static final int SWEEP_INTERVAL_TICKS = 20;
    static final int BOUND_MARGIN = 1;

    /** Chunks the server forgot that may still be resident client-side. */
    private static final LongOpenHashSet TRACKED = new LongOpenHashSet();
    private static WeakReference<ClientLevel> trackedLevel = new WeakReference<>(null);
    private static int serverChunkRadius;
    private static int ticksSinceSweep;
    private static long evictedThisLevel;
    private static int lastBound;

    private FarsightChunkBound() {
    }

    public static boolean enabled() {
        return ClientOptimizationConfig.farsightChunkBound;
    }

    /** Called at HEAD of handleLogin/handleSetChunkCacheRadius, before Farsight's redirect. */
    public static void recordServerChunkRadius(int radius) {
        if (!Minecraft.getInstance().isSameThread()) {
            return;
        }
        serverChunkRadius = Math.max(0, radius);
    }

    /**
     * Called at RETURN of handleLevelChunkWithLight: the server resent the chunk, so it counts it as
     * sent again and the chunk must stay.
     */
    public static void onChunkLoaded(ClientLevel level, int chunkX, int chunkZ) {
        if (level == null || !Minecraft.getInstance().isSameThread()) {
            return;
        }
        syncLevel(level);
        TRACKED.remove(ChunkPos.asLong(chunkX, chunkZ));
    }

    /**
     * Called from the forget packet itself: Farsight cancels the handler at HEAD before it moves to
     * the client thread, so this may arrive on the network thread and is then queued behind the
     * packets already scheduled on the client.
     */
    public static void onServerForget(int chunkX, int chunkZ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(() -> onServerForget(chunkX, chunkZ));
            return;
        }
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        syncLevel(level);
        TRACKED.add(ChunkPos.asLong(chunkX, chunkZ));
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            reset();
            return;
        }
        syncLevel(level);
        if (!enabled() || ++ticksSinceSweep < SWEEP_INTERVAL_TICKS) {
            return;
        }
        ticksSinceSweep = 0;
        sweep(minecraft, level);
    }

    static int bound(int serverRadius, int clientRenderDistance) {
        return Math.max(serverRadius, clientRenderDistance) + BOUND_MARGIN;
    }

    static boolean exceedsBound(int chunkX, int chunkZ, int centerX, int centerZ, int bound) {
        return Math.abs(chunkX - centerX) > bound || Math.abs(chunkZ - centerZ) > bound;
    }

    private static void sweep(Minecraft minecraft, ClientLevel level) {
        int bound = bound(serverChunkRadius, minecraft.options.renderDistance);
        lastBound = bound;
        if (TRACKED.isEmpty()) {
            return;
        }
        ChunkPos center = minecraft.player.chunkPosition();
        LongArrayList evict = null;
        for (LongIterator iterator = TRACKED.iterator(); iterator.hasNext(); ) {
            long key = iterator.nextLong();
            if (exceedsBound(ChunkPos.getX(key), ChunkPos.getZ(key), center.x, center.z, bound)) {
                if (evict == null) {
                    evict = new LongArrayList();
                }
                evict.add(key);
            }
        }
        if (evict == null) {
            return;
        }
        ClientChunkCache chunkCache = level.getChunkSource();
        for (int index = 0; index < evict.size(); index++) {
            long key = evict.getLong(index);
            TRACKED.remove(key);
            int chunkX = ChunkPos.getX(key);
            int chunkZ = ChunkPos.getZ(key);
            // Farsight did not keep it, or it is gone already.
            if (chunkCache.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) == null) {
                continue;
            }
            forget(level, chunkCache, chunkX, chunkZ);
        }
        evictedThisLevel += evict.size();
    }

    /** Mirrors vanilla 1.18.2 handleForgetLevelChunk plus the renderer's RETURN hook. */
    private static void forget(ClientLevel level, ClientChunkCache chunkCache, int chunkX, int chunkZ) {
        chunkCache.drop(chunkX, chunkZ);
        level.queueLightUpdate(() -> {
            // The server may have resent it before this queued update ran.
            if (chunkCache.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) != null) {
                return;
            }
            LevelLightEngine lightEngine = level.getLightEngine();
            for (int section = level.getMinSection(); section < level.getMaxSection(); section++) {
                lightEngine.updateSectionStatus(SectionPos.of(chunkX, section, chunkZ), true);
            }
            lightEngine.enableLightSources(new ChunkPos(chunkX, chunkZ), false);
            level.setLightReady(chunkX, chunkZ);
        });
        SodiumChunkUnloadBridge.onChunkRemoved(chunkX, chunkZ);
    }

    private static void syncLevel(ClientLevel level) {
        if (trackedLevel.get() == level) {
            return;
        }
        TRACKED.clear();
        trackedLevel = new WeakReference<>(level);
        ticksSinceSweep = 0;
        evictedThisLevel = 0;
    }

    private static void reset() {
        if (trackedLevel.get() == null && TRACKED.isEmpty()) {
            return;
        }
        TRACKED.clear();
        TRACKED.trim();
        trackedLevel = new WeakReference<>(null);
        ticksSinceSweep = 0;
        evictedThisLevel = 0;
    }

    public static String status() {
        return String.format(Locale.ROOT,
                "%s; server radius %d, bound %d chunks, tracked %d, forgotten this level %d, renderer notification %s",
                enabled() ? "ON" : "OFF (Farsight keeps every chunk until the level changes)",
                serverChunkRadius, lastBound, TRACKED.size(), evictedThisLevel,
                SodiumChunkUnloadBridge.available() ? "bound" : "unavailable");
    }
}
