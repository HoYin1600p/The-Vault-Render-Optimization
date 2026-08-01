package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.render.CachedBlockEntityRendererHolder;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nullable;

@Mixin(BlockEntityType.class)
public abstract class BlockEntityTypeRendererCacheMixin implements CachedBlockEntityRendererHolder {
    @Unique
    @Nullable
    private BlockEntityRenderer<?> vro$blockEntityRenderer;

    @Override
    @Nullable
    public BlockEntityRenderer<?> vro$getBlockEntityRenderer() {
        return this.vro$blockEntityRenderer;
    }

    @Override
    public void vro$setBlockEntityRenderer(@Nullable BlockEntityRenderer<?> renderer) {
        this.vro$blockEntityRenderer = renderer;
    }
}
