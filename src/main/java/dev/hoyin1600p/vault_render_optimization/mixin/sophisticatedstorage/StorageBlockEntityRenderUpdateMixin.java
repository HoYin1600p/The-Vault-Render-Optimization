package dev.hoyin1600p.vault_render_optimization.mixin.sophisticatedstorage;

import dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage.BarrelModelSignature;
import dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage.StorageRenderUpdateState;
import dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage.SophisticatedStorageDiagnostics;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StorageBlockEntity.class, remap = false)
public abstract class StorageBlockEntityRenderUpdateMixin implements StorageRenderUpdateState {
    @Unique
    private boolean vro$filteringPacket;
    @Unique
    private boolean vro$deferredRenderUpdate;
    @Unique
    private BarrelModelSignature.Snapshot vro$beforePacketModel;

    @Inject(
            method = "onDataPacket(Lnet/minecraft/network/Connection;Lnet/minecraft/network/protocol/game/ClientboundBlockEntityDataPacket;)V",
            at = @At("HEAD"),
            require = 1
    )
    private void vro$beginRenderUpdateFilter(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            CallbackInfo ci
    ) {
        StorageBlockEntity storage = (StorageBlockEntity) (Object) this;
        if (!ClientOptimizationConfig.optimizationsEnabled()
                || !ClientOptimizationConfig.sophisticatedStorageRenderUpdateFilter
                || !(storage instanceof BarrelBlockEntity barrel)
                || barrel.getLevel() == null
                || !barrel.getLevel().isClientSide()) {
            vro$filteringPacket = false;
            vro$deferredRenderUpdate = false;
            vro$beforePacketModel = null;
            return;
        }

        vro$beforePacketModel = BarrelModelSignature.capture(barrel);
        vro$deferredRenderUpdate = false;
        vro$filteringPacket = true;
    }

    @Inject(
            method = "onDataPacket(Lnet/minecraft/network/Connection;Lnet/minecraft/network/protocol/game/ClientboundBlockEntityDataPacket;)V",
            at = @At("RETURN"),
            require = 1
    )
    private void vro$finishRenderUpdateFilter(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            CallbackInfo ci
    ) {
        if (!vro$filteringPacket) {
            return;
        }

        // End filtering before forwarding the one required notification.
        vro$filteringPacket = false;
        boolean updateRequested = vro$deferredRenderUpdate;
        vro$deferredRenderUpdate = false;
        BarrelModelSignature.Snapshot before = vro$beforePacketModel;
        vro$beforePacketModel = null;
        if (!updateRequested) {
            return;
        }

        BarrelBlockEntity barrel = (BarrelBlockEntity) (Object) this;
        BarrelModelSignature.Snapshot after = BarrelModelSignature.capture(barrel);
        if (!after.equals(before)) {
            SophisticatedStorageDiagnostics.recordModelRebuildAllowed();
            WorldHelper.notifyBlockUpdate(barrel);
        } else {
            SophisticatedStorageDiagnostics.recordCountOnlyRebuildSkipped();
        }
    }

    @Redirect(
            method = "loadSynchronizedData(Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/p3pp3rf1y/sophisticatedcore/util/WorldHelper;notifyBlockUpdate(Lnet/minecraft/world/level/block/entity/BlockEntity;)V"
            ),
            require = 1
    )
    private void vro$skipCountOnlyChunkRebuild(BlockEntity blockEntity) {
        if (!vro$deferRenderUpdateIfFilteringPacket()) {
            WorldHelper.notifyBlockUpdate(blockEntity);
        }
    }

    @Override
    public boolean vro$deferRenderUpdateIfFilteringPacket() {
        if (!vro$filteringPacket) {
            return false;
        }
        vro$deferredRenderUpdate = true;
        return true;
    }
}
