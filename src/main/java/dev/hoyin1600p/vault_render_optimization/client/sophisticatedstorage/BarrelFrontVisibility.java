package dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;

/**
 * VRO-owned visibility policy for Sophisticated Storage front-face block-entity
 * displays. Target behavior was inspected at Sophisticated Storage revision
 * 891b0d9e29350ec78c7b8567285036d2ecdb303c (GPL-3.0-only).
 */
public final class BarrelFrontVisibility {
    private static final double CAMERA_INSIDE_MARGIN = 1.0E-4D;
    private static final double FACE_EPSILON = 1.0E-5D;
    private static final ThreadLocal<BlockPos.MutableBlockPos> ADJACENT_POS =
            ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    private BarrelFrontVisibility() {
    }

    public static boolean shouldRender(BarrelBlockEntity barrel) {
        if (!ClientOptimizationConfig.optimizationsEnabled()
                || !ClientOptimizationConfig.sophisticatedStorageFaceCulling) {
            return true;
        }
        if (barrel.getLevel() == null) {
            return true;
        }

        BlockState state = barrel.getBlockState();
        if (!(state.getBlock() instanceof StorageBlockBase storageBlock)) {
            return true;
        }

        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        BlockPos pos = barrel.getBlockPos();
        if (cameraInsideBlock(camera, pos)) {
            SophisticatedStorageDiagnostics.recordFrontFaceRendered();
            return true;
        }

        Direction facing = storageBlock.getFacing(state);
        if (!cameraIsInFront(
                camera.x, camera.y, camera.z,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                facing.getStepX(), facing.getStepY(), facing.getStepZ()
        )) {
            SophisticatedStorageDiagnostics.recordBackFaceSkipped();
            return false;
        }

        BlockPos.MutableBlockPos adjacentPos = ADJACENT_POS.get().setWithOffset(pos, facing);
        if (!Block.shouldRenderFace(state, barrel.getLevel(), pos, facing, adjacentPos)) {
            SophisticatedStorageDiagnostics.recordCoveredFaceSkipped();
            return false;
        }

        SophisticatedStorageDiagnostics.recordFrontFaceRendered();
        return true;
    }

    static boolean cameraIsInFront(
            double cameraX,
            double cameraY,
            double cameraZ,
            double blockCenterX,
            double blockCenterY,
            double blockCenterZ,
            int normalX,
            int normalY,
            int normalZ
    ) {
        double faceX = blockCenterX + normalX * 0.5D;
        double faceY = blockCenterY + normalY * 0.5D;
        double faceZ = blockCenterZ + normalZ * 0.5D;
        double dot = (cameraX - faceX) * normalX
                + (cameraY - faceY) * normalY
                + (cameraZ - faceZ) * normalZ;
        return dot >= -FACE_EPSILON;
    }

    private static boolean cameraInsideBlock(Vec3 camera, BlockPos pos) {
        return camera.x >= pos.getX() - CAMERA_INSIDE_MARGIN
                && camera.x <= pos.getX() + 1.0D + CAMERA_INSIDE_MARGIN
                && camera.y >= pos.getY() - CAMERA_INSIDE_MARGIN
                && camera.y <= pos.getY() + 1.0D + CAMERA_INSIDE_MARGIN
                && camera.z >= pos.getZ() - CAMERA_INSIDE_MARGIN
                && camera.z <= pos.getZ() + 1.0D + CAMERA_INSIDE_MARGIN;
    }
}
