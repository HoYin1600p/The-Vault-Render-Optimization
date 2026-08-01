package dev.hoyin1600p.vault_render_optimization.client.render;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;

import javax.annotation.Nullable;

public interface CachedBlockEntityRendererHolder {
    @Nullable
    BlockEntityRenderer<?> vro$getBlockEntityRenderer();

    void vro$setBlockEntityRenderer(@Nullable BlockEntityRenderer<?> renderer);
}
