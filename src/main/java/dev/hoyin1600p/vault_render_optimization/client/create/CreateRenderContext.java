package dev.hoyin1600p.vault_render_optimization.client.create;

import com.mojang.math.Matrix4f;
import dev.hoyin1600p.vault_render_optimization.mixin.Matrix4fAccessor;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public final class CreateRenderContext {
    private static Frustum frustum;
    private static int blockEntitiesTested;
    private static int blockEntitiesCulled;
    private static int actorsTested;
    private static int actorsCulled;
    private static int sectionsTested;
    private static int sectionsCulled;
    private static int emptyFlushesSkipped;
    private static FrameStats lastFrame = FrameStats.EMPTY;

    private CreateRenderContext() {
    }

    public static void beginFrame(Frustum currentFrustum) {
        lastFrame = new FrameStats(
                blockEntitiesTested,
                blockEntitiesCulled,
                actorsTested,
                actorsCulled,
                sectionsTested,
                sectionsCulled,
                emptyFlushesSkipped
        );
        blockEntitiesTested = 0;
        blockEntitiesCulled = 0;
        actorsTested = 0;
        actorsCulled = 0;
        sectionsTested = 0;
        sectionsCulled = 0;
        emptyFlushesSkipped = 0;
        frustum = currentFrustum;
    }

    public static boolean isVisible(AABB localBounds, Matrix4f localToWorld) {
        return frustum == null || frustum.isVisible(transformBounds(localBounds, localToWorld));
    }

    public static AABB transformBounds(AABB box, Matrix4f matrix) {
        Matrix4fAccessor m = (Matrix4fAccessor) (Object) matrix;
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (int corner = 0; corner < 8; corner++) {
            float x = (float) ((corner & 1) == 0 ? box.minX : box.maxX);
            float y = (float) ((corner & 2) == 0 ? box.minY : box.maxY);
            float z = (float) ((corner & 4) == 0 ? box.minZ : box.maxZ);
            float transformedX = m.vro$m00() * x + m.vro$m01() * y + m.vro$m02() * z + m.vro$m03();
            float transformedY = m.vro$m10() * x + m.vro$m11() * y + m.vro$m12() * z + m.vro$m13();
            float transformedZ = m.vro$m20() * x + m.vro$m21() * y + m.vro$m22() * z + m.vro$m23();
            minX = Math.min(minX, transformedX);
            minY = Math.min(minY, transformedY);
            minZ = Math.min(minZ, transformedZ);
            maxX = Math.max(maxX, transformedX);
            maxY = Math.max(maxY, transformedY);
            maxZ = Math.max(maxZ, transformedZ);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static BlockPos.MutableBlockPos transformCenter(Matrix4f matrix, BlockPos pos,
                                                            BlockPos.MutableBlockPos destination) {
        Matrix4fAccessor m = (Matrix4fAccessor) (Object) matrix;
        float x = pos.getX() + 0.5f;
        float y = pos.getY() + 0.5f;
        float z = pos.getZ() + 0.5f;
        destination.set(
                m.vro$m00() * x + m.vro$m01() * y + m.vro$m02() * z + m.vro$m03(),
                m.vro$m10() * x + m.vro$m11() * y + m.vro$m12() * z + m.vro$m13(),
                m.vro$m20() * x + m.vro$m21() * y + m.vro$m22() * z + m.vro$m23()
        );
        return destination;
    }

    public static void recordBlockEntity(boolean culled) {
        blockEntitiesTested++;
        if (culled) {
            blockEntitiesCulled++;
        }
    }

    public static void recordActor(boolean culled) {
        actorsTested++;
        if (culled) {
            actorsCulled++;
        }
    }

    public static void recordSection(boolean culled) {
        sectionsTested++;
        if (culled) {
            sectionsCulled++;
        }
    }

    public static void recordEmptyFlushSkipped() {
        emptyFlushesSkipped++;
    }

    public static FrameStats lastFrame() {
        return lastFrame;
    }

    public record FrameStats(int blockEntitiesTested, int blockEntitiesCulled,
                             int actorsTested, int actorsCulled,
                             int sectionsTested, int sectionsCulled,
                             int emptyFlushesSkipped) {
        private static final FrameStats EMPTY = new FrameStats(0, 0, 0, 0, 0, 0, 0);
    }
}
