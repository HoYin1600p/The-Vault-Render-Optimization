package dev.hoyin1600p.vault_render_optimization.mixin.sophisticatedstorage;

import dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage.StorageRenderUpdateState;
import java.util.function.Consumer;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.IDynamicRenderTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = BarrelBlockEntity.class, remap = false)
public abstract class BarrelRenderInfoUpdateMixin {
    @Shadow
    private IDynamicRenderTracker dynamicRenderTracker;

    @ModifyArg(
            method = "<init>(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntityType;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/p3pp3rf1y/sophisticatedcore/renderdata/RenderInfo;setChangeListener(Ljava/util/function/Consumer;)V"
            ),
            index = 0,
            require = 1
    )
    private Consumer<RenderInfo> vro$installFilteredRenderInfoListener(Consumer<RenderInfo> original) {
        return renderInfo -> {
            dynamicRenderTracker.onRenderInfoUpdated(renderInfo);
            BarrelBlockEntity barrel = (BarrelBlockEntity) (Object) this;
            StorageRenderUpdateState state = (StorageRenderUpdateState) this;
            if (!state.vro$deferRenderUpdateIfFilteringPacket()) {
                WorldHelper.notifyBlockUpdate(barrel);
            }
        };
    }
}
