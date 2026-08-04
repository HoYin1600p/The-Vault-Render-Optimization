package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.lighting.DynamicLightEngine;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(Level.class)
public abstract class LevelDynamicLightMixin {
    @Shadow
    @Final
    protected List<TickingBlockEntity> blockEntityTickers;

    @Shadow
    @Nullable
    public abstract BlockEntity getBlockEntity(BlockPos pos);

    @Inject(method = "tickBlockEntities", at = @At("TAIL"))
    private void vro$observeDynamicBlockEntities(CallbackInfo ci) {
        Level self = (Level) (Object) this;
        if (!(self instanceof ClientLevel clientLevel) || !DynamicLightEngine.shouldObserveBlockEntities()) {
            return;
        }
        Object[] tickers = this.blockEntityTickers.toArray();
        for (Object entry : tickers) {
            TickingBlockEntity ticker = (TickingBlockEntity) entry;
            if (ticker.isRemoved()) {
                continue;
            }
            BlockEntity blockEntity = this.getBlockEntity(ticker.getPos());
            if (blockEntity != null) {
                DynamicLightEngine.observeBlockEntity(clientLevel, blockEntity);
            }
        }
    }
}
