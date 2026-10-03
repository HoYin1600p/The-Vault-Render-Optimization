package dev.hoyin1600p.vault_render_optimization.client.particle;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Exact particle block collision with a per-tick cell cache.
 *
 * <p>{@code Particle.move} calls {@code Entity.collideBoundingBox(null, motion, box, level, List.of())},
 * which collects {@code level.getBlockCollisions(null, box.expandTowards(motion))} and hands them to
 * {@code collideWithShapes}. {@link #collect} visits exactly the cells vanilla's {@code BlockCollisions}
 * visits (the swept box plus a one-block border, skipping corners; border faces only count for tall
 * shapes and border edges only for moving pistons) and applies the same shape tests, so it returns
 * the same shapes. Only block states and collision shapes are cached, keyed by position, and only
 * for the duration of one {@code ParticleEngine.tick} on the client thread, during which no block can
 * change (packets are handled before the particle tick). Hundreds of Nova particles over the same
 * floor then share one lookup per cell instead of repeating ~19 block reads each.
 */
public final class ParticleCollisionCache {
    /** Cell data vanilla's predicates need; the collision shape is resolved lazily, as vanilla does. */
    public abstract static class Cell {
        final int x, y, z;
        private VoxelShape moved;

        protected Cell(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        protected abstract boolean largeCollisionShape();

        protected abstract boolean movingPiston();

        /** Collision shape at block-local coordinates with an empty collision context. */
        protected abstract VoxelShape shape();

        final VoxelShape moved() {
            if (moved == null) moved = shape().move(x, y, z);
            return moved;
        }
    }

    /** Null means the chunk is not available for collisions; vanilla skips such cells. */
    public interface Cells {
        Cell get(int x, int y, int z);
    }

    private ParticleCollisionCache() {
    }

    /** Appends the shapes {@code BlockCollisions(getter, null, swept)} would yield, in the same order. */
    public static void collect(AABB swept, Cells cells, List<VoxelShape> out) {
        int minX = Mth.floor(swept.minX - 1.0E-7D) - 1, maxX = Mth.floor(swept.maxX + 1.0E-7D) + 1;
        int minY = Mth.floor(swept.minY - 1.0E-7D) - 1, maxY = Mth.floor(swept.maxY + 1.0E-7D) + 1;
        int minZ = Mth.floor(swept.minZ - 1.0E-7D) - 1, maxZ = Mth.floor(swept.maxZ + 1.0E-7D) + 1;
        VoxelShape entityShape = null;
        // Same iteration order as Cursor3D: x fastest, then y, then z.
        for (int z = minZ; z <= maxZ; z++) {
            int zEdge = z == minZ || z == maxZ ? 1 : 0;
            for (int y = minY; y <= maxY; y++) {
                int yEdge = y == minY || y == maxY ? 1 : 0;
                for (int x = minX; x <= maxX; x++) {
                    int type = (x == minX || x == maxX ? 1 : 0) + yEdge + zEdge;
                    if (type == 3) continue;
                    Cell cell = cells.get(x, y, z);
                    if (cell == null) continue;
                    if (type == 1 && !cell.largeCollisionShape() || type == 2 && !cell.movingPiston()) continue;
                    VoxelShape shape = cell.shape();
                    if (shape == Shapes.block()) {
                        if (swept.intersects(x, y, z, x + 1.0D, y + 1.0D, z + 1.0D)) out.add(cell.moved());
                        continue;
                    }
                    if (shape.isEmpty()) continue; // joinIsNotEmpty(empty, box, AND) is always false
                    if (entityShape == null) entityShape = Shapes.create(swept);
                    VoxelShape moved = cell.moved();
                    if (Shapes.joinIsNotEmpty(moved, entityShape, BooleanOp.AND)) out.add(moved);
                }
            }
        }
    }

    // --- Runtime scope: one ParticleEngine.tick on the client thread -----------------------------

    private static final Long2ObjectOpenHashMap<Object> CELLS = new Long2ObjectOpenHashMap<>();
    private static final Object NO_CHUNK = new Object();
    private static final ArrayList<VoxelShape> SCRATCH = new ArrayList<>();
    private static Level level;
    private static Thread owner;
    private static BlockGetter lastChunk;
    private static long lastChunkKey = Long.MIN_VALUE;
    private static final Cells RUNTIME_CELLS = ParticleCollisionCache::runtimeCell;

    public static void begin(Level tickLevel) {
        reset();
        level = tickLevel;
        owner = Thread.currentThread();
    }

    public static void end() {
        reset();
    }

    private static void reset() {
        level = null;
        owner = null;
        lastChunk = null;
        lastChunkKey = Long.MIN_VALUE;
        boolean oversized = CELLS.size() > 32_768;
        CELLS.clear();
        if (oversized) CELLS.trim(4_096);
    }

    /** True inside the particle tick of this level on its client thread. */
    public static boolean active(Level candidate) {
        return candidate != null && candidate == level && Thread.currentThread() == owner;
    }

    /** Shapes vanilla would collide with, from the cache. The returned list is reused; copy to retain. */
    public static List<VoxelShape> shapes(AABB swept) {
        SCRATCH.clear();
        collect(swept, RUNTIME_CELLS, SCRATCH);
        return SCRATCH;
    }

    private static Cell runtimeCell(int x, int y, int z) {
        long key = BlockPos.asLong(x, y, z);
        Object cached = CELLS.get(key);
        if (cached == null) {
            BlockGetter chunk = chunk(x, z);
            if (chunk == null) {
                cached = NO_CHUNK;
            } else {
                BlockPos pos = new BlockPos(x, y, z);
                cached = new RuntimeCell(x, y, z, pos, chunk.getBlockState(pos), level);
            }
            CELLS.put(key, cached);
        }
        return cached == NO_CHUNK ? null : (Cell) cached;
    }

    private static BlockGetter chunk(int x, int z) {
        int chunkX = SectionPos.blockToSectionCoord(x);
        int chunkZ = SectionPos.blockToSectionCoord(z);
        long key = ChunkPos.asLong(chunkX, chunkZ);
        if (key != lastChunkKey) {
            lastChunk = level.getChunkForCollisions(chunkX, chunkZ);
            lastChunkKey = key;
        }
        return lastChunk;
    }

    private static final class RuntimeCell extends Cell {
        private final BlockPos pos;
        private final BlockState state;
        private final Level world;
        private VoxelShape shape;

        RuntimeCell(int x, int y, int z, BlockPos pos, BlockState state, Level world) {
            super(x, y, z);
            this.pos = pos;
            this.state = state;
            this.world = world;
        }

        @Override protected boolean largeCollisionShape() { return state.hasLargeCollisionShape(); }

        @Override protected boolean movingPiston() { return state.is(Blocks.MOVING_PISTON); }

        @Override
        protected VoxelShape shape() {
            // Vanilla passes the level (not the chunk) and an empty context for a null entity.
            if (shape == null) shape = state.getCollisionShape(world, pos, CollisionContext.empty());
            return shape;
        }
    }
}
