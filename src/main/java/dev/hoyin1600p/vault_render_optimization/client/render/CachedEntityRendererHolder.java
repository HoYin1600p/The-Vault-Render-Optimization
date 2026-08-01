package dev.hoyin1600p.vault_render_optimization.client.render;

import net.minecraft.client.renderer.entity.EntityRenderer;

import javax.annotation.Nullable;

public interface CachedEntityRendererHolder {
    @Nullable
    EntityRenderer<?> vro$getEntityRenderer();

    void vro$setEntityRenderer(@Nullable EntityRenderer<?> renderer);
}
