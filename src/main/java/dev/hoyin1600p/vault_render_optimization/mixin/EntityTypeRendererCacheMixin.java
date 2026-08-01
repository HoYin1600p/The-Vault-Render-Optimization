package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.render.CachedEntityRendererHolder;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nullable;

@Mixin(EntityType.class)
public abstract class EntityTypeRendererCacheMixin implements CachedEntityRendererHolder {
    @Unique
    @Nullable
    private EntityRenderer<?> vro$entityRenderer;

    @Override
    @Nullable
    public EntityRenderer<?> vro$getEntityRenderer() {
        return this.vro$entityRenderer;
    }

    @Override
    public void vro$setEntityRenderer(@Nullable EntityRenderer<?> renderer) {
        this.vro$entityRenderer = renderer;
    }
}
