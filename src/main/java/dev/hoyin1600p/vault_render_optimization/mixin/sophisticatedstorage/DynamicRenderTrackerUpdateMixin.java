package dev.hoyin1600p.vault_render_optimization.mixin.sophisticatedstorage;

import dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage.StorageRenderUpdateState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.DynamicRenderTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DynamicRenderTracker.class, remap = false)
public abstract class DynamicRenderTrackerUpdateMixin {
    @Redirect(
            method = "onRenderInfoUpdated(Lnet/p3pp3rf1y/sophisticatedcore/renderdata/RenderInfo;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/p3pp3rf1y/sophisticatedcore/util/WorldHelper;notifyBlockUpdate(Lnet/minecraft/world/level/block/entity/BlockEntity;)V"
            ),
            require = 1
    )
    private void vro$deferUnsupportedDynamicUpdate(BlockEntity blockEntity) {
        vro$deferOrNotify(blockEntity);
    }

    @Redirect(
            method = "updateDynamicFlags(Ljava/util/List;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/p3pp3rf1y/sophisticatedcore/util/WorldHelper;notifyBlockUpdate(Lnet/minecraft/world/level/block/entity/BlockEntity;)V"
            ),
            require = 1
    )
    private void vro$deferDynamicOwnershipUpdate(BlockEntity blockEntity) {
        vro$deferOrNotify(blockEntity);
    }

    private static void vro$deferOrNotify(BlockEntity blockEntity) {
        if (blockEntity instanceof StorageRenderUpdateState state
                && state.vro$deferRenderUpdateIfFilteringPacket()) {
            return;
        }
        WorldHelper.notifyBlockUpdate(blockEntity);
    }
}
