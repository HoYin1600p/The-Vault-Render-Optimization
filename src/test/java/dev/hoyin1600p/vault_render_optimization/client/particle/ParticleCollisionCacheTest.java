package dev.hoyin1600p.vault_render_optimization.client.particle;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

class ParticleCollisionCacheTest {
    private record Spec(VoxelShape shape, boolean large, boolean piston) { }

    private static final class TestCell extends ParticleCollisionCache.Cell {
        private final Spec spec;
        int shapeCalls;

        TestCell(int x, int y, int z, Spec spec) {
            super(x, y, z);
            this.spec = spec;
        }

        @Override protected boolean largeCollisionShape() { return spec.large(); }
        @Override protected boolean movingPiston() { return spec.piston(); }
        @Override protected VoxelShape shape() { shapeCalls++; return spec.shape(); }
    }

    private static final Spec[] PALETTE = {
            new Spec(Shapes.empty(), false, false),                                    // air
            new Spec(Shapes.block(), false, false),                                    // full block
            new Spec(Shapes.box(0, 0, 0, 1, 0.5, 1), false, false),                    // slab
            new Spec(Shapes.box(0, 0, 0, 1, 1.0 / 16, 1), false, false),               // carpet
            new Spec(Shapes.box(0.375, 0, 0.375, 0.625, 1.5, 0.625), true, false),     // fence post
            new Spec(Shapes.box(0, 0, 0, 1, 1.5, 1), true, false),                     // tall full wall
            new Spec(Shapes.block(), false, true),                                     // moving piston
            new Spec(Shapes.empty(), true, false),                                     // tall but empty
    };

    /** Literal transcription of vanilla 1.18.2 BlockCollisions.computeNext for a null entity. */
    private static List<VoxelShape> vanilla(AABB box, Map<Long, Spec> world) {
        List<VoxelShape> out = new ArrayList<>();
        VoxelShape entityShape = Shapes.create(box);
        Cursor3D cursor = new Cursor3D(
                Mth.floor(box.minX - 1.0E-7D) - 1, Mth.floor(box.minY - 1.0E-7D) - 1, Mth.floor(box.minZ - 1.0E-7D) - 1,
                Mth.floor(box.maxX + 1.0E-7D) + 1, Mth.floor(box.maxY + 1.0E-7D) + 1, Mth.floor(box.maxZ + 1.0E-7D) + 1);
        while (cursor.advance()) {
            int i = cursor.nextX(), j = cursor.nextY(), k = cursor.nextZ(), l = cursor.getNextType();
            if (l == 3) continue;
            if (!world.containsKey(BlockPos.asLong(i, j, k))) continue; // unloaded chunk
            Spec state = world.get(BlockPos.asLong(i, j, k));
            if (l == 1 && !state.large() || l == 2 && !state.piston()) continue;
            VoxelShape voxelshape = state.shape();
            if (voxelshape == Shapes.block()) {
                if (!box.intersects(i, j, k, i + 1.0D, j + 1.0D, k + 1.0D)) continue;
                out.add(voxelshape.move(i, j, k));
                continue;
            }
            VoxelShape moved = voxelshape.move(i, j, k);
            if (!Shapes.joinIsNotEmpty(moved, entityShape, BooleanOp.AND)) continue;
            out.add(moved);
        }
        return out;
    }

    private static List<VoxelShape> cached(AABB box, Map<Long, Spec> world, Map<Long, TestCell> cells) {
        List<VoxelShape> out = new ArrayList<>();
        ParticleCollisionCache.collect(box, (x, y, z) -> {
            long key = BlockPos.asLong(x, y, z);
            Spec spec = world.get(key);
            return spec == null ? null : cells.computeIfAbsent(key, ignored -> new TestCell(x, y, z, spec));
        }, out);
        return out;
    }

    private static void assertSameShapes(List<VoxelShape> expected, List<VoxelShape> actual, String context) {
        assertEquals(expected.size(), actual.size(), context);
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).toAabbs(), actual.get(i).toAabbs(), context + " shape " + i);
        }
    }

    @Test
    void matchesVanillaBlockCollisionsOnRandomWorlds() {
        Random random = new Random(1600);
        for (int world = 0; world < 200; world++) {
            Map<Long, Spec> blocks = new HashMap<>();
            for (int x = -4; x <= 4; x++) for (int y = -4; y <= 4; y++) for (int z = -4; z <= 4; z++) {
                if (random.nextInt(12) == 0) continue; // unloaded chunk column cell
                blocks.put(BlockPos.asLong(x, y, z), PALETTE[random.nextInt(4) == 0 ? random.nextInt(PALETTE.length) : 0]);
            }
            Map<Long, TestCell> cells = new HashMap<>(); // shared across queries, like one particle tick
            for (int query = 0; query < 50; query++) {
                double size = random.nextInt(3) == 0 ? 2.0 : random.nextDouble() * 0.4 + 0.02;
                // Snap some positions to exact block boundaries, where the 1e-7 halo matters.
                double x = snap(random, random.nextDouble() * 4 - 2);
                double y = snap(random, random.nextDouble() * 4 - 2);
                double z = snap(random, random.nextDouble() * 4 - 2);
                AABB box = new AABB(x - size / 2, y, z - size / 2, x + size / 2, y + size, z + size / 2);
                AABB swept = box.expandTowards(motion(random), motion(random), motion(random));
                assertSameShapes(vanilla(swept, blocks), cached(swept, blocks, cells), "world " + world + " query " + query);
            }
        }
    }

    @Test
    void particleResting_on_aFloor_findsNoShapesJustLikeVanilla() {
        Map<Long, Spec> blocks = new HashMap<>();
        for (int x = -3; x <= 3; x++) for (int y = -3; y <= 3; y++) for (int z = -3; z <= 3; z++) {
            blocks.put(BlockPos.asLong(x, y, z), y < 0 ? PALETTE[1] : PALETTE[0]);
        }
        AABB box = new AABB(0.4, 0.05, 0.4, 0.6, 0.25, 0.6).expandTowards(0.01, -0.02, 0.0);
        assertTrue(vanilla(box, blocks).isEmpty());
        assertTrue(cached(box, blocks, new HashMap<>()).isEmpty());
    }

    @Test
    void sharedCellsResolveEachShapeOnlyOnce() {
        Map<Long, Spec> blocks = new HashMap<>();
        for (int x = -3; x <= 3; x++) for (int y = -3; y <= 3; y++) for (int z = -3; z <= 3; z++) {
            blocks.put(BlockPos.asLong(x, y, z), PALETTE[2]);
        }
        Map<Long, TestCell> cells = new HashMap<>();
        AABB box = new AABB(0.2, 0.2, 0.2, 0.4, 0.4, 0.4);
        List<VoxelShape> first = cached(box, blocks, cells);
        int built = cells.size();
        for (int i = 0; i < 100; i++) {
            List<VoxelShape> again = cached(box, blocks, cells);
            assertEquals(built, cells.size(), "cells are built once per tick scope");
            for (int s = 0; s < first.size(); s++) assertSame(first.get(s), again.get(s), "moved shape reused");
        }
    }

    private static double snap(Random random, double value) {
        return random.nextInt(4) == 0 ? Math.round(value * 2) / 2.0 : value;
    }

    private static double motion(Random random) {
        return random.nextInt(3) == 0 ? 0.0 : (random.nextDouble() - 0.5) * (random.nextInt(5) == 0 ? 1.5 : 0.2);
    }
}
