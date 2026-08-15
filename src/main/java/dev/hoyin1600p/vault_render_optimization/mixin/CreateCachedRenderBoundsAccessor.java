package dev.hoyin1600p.vault_render_optimization.mixin;

import com.simibubi.create.foundation.blockEntity.CachedRenderBBBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = CachedRenderBBBlockEntity.class, remap = false)
public interface CreateCachedRenderBoundsAccessor {
    @Invoker("invalidateRenderBoundingBox")
    void vro$invalidateRenderBoundingBox();
}
