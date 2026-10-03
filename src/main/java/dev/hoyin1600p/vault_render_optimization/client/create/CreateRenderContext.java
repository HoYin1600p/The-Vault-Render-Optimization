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
        return transformBounds(box, (Matrix4fAccessor) (Object) matrix);
    }

    static AABB transformBounds(AABB box, Matrix4fAccessor m) {
        // An affine row is monotone in each coordinate. Select its two extrema
        // instead of transforming all eight corners. Keep the original float
        // casts and operation order, including large-coordinate rounding.
        // No cross-frame visibility cache: shadow/camera passes stay independent.
        return new AABB(
                extreme(box, m.vro$m00(), m.vro$m01(), m.vro$m02(), m.vro$m03(), false),
                extreme(box, m.vro$m10(), m.vro$m11(), m.vro$m12(), m.vro$m13(), false),
                extreme(box, m.vro$m20(), m.vro$m21(), m.vro$m22(), m.vro$m23(), false),
                extreme(box, m.vro$m00(), m.vro$m01(), m.vro$m02(), m.vro$m03(), true),
                extreme(box, m.vro$m10(), m.vro$m11(), m.vro$m12(), m.vro$m13(), true),
                extreme(box, m.vro$m20(), m.vro$m21(), m.vro$m22(), m.vro$m23(), true));
    }

    private static float extreme(AABB box, float a, float b, float c, float d, boolean maximum) {
        float x = (float) ((a < 0) == maximum ? box.minX : box.maxX);
        float y = (float) ((b < 0) == maximum ? box.minY : box.maxY);
        float z = (float) ((c < 0) == maximum ? box.minZ : box.maxZ);
        return a * x + b * y + c * z + d;
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
