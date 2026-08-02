package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.render.CachedEntityRendererHolder;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.Registry;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = EntityRenderDispatcher.class, priority = 700)
public abstract class EntityRenderDispatcherMixin {
    @Shadow
    public Map<EntityType<?>, EntityRenderer<?>> renderers;

    @Inject(method = "getRenderer", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T extends Entity> void vro$getCachedRenderer(T entity,
                                                          CallbackInfoReturnable<EntityRenderer<? super T>> cir) {
        if (!ClientOptimizationConfig.optimizationsEnabled()
                || !ClientOptimizationConfig.entityRendererCache
                || entity instanceof AbstractClientPlayer) {
            return;
        }

        CachedEntityRendererHolder holder = (CachedEntityRendererHolder) (Object) entity.getType();
        EntityRenderer<?> renderer = holder.vro$getEntityRenderer();
        if (renderer == null) {
            renderer = this.renderers.get(entity.getType());
            holder.vro$setEntityRenderer(renderer);
        }
        if (renderer != null) {
            cir.setReturnValue((EntityRenderer) renderer);
        }
    }

    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    private void vro$refreshRendererCache(ResourceManager resourceManager, CallbackInfo ci) {
        for (EntityType<?> entityType : Registry.ENTITY_TYPE) {
            ((CachedEntityRendererHolder) (Object) entityType).vro$setEntityRenderer(null);
        }
        if (ClientOptimizationConfig.optimizationsEnabled()
                && ClientOptimizationConfig.entityRendererCache) {
            this.renderers.forEach((type, renderer) ->
                    ((CachedEntityRendererHolder) (Object) type).vro$setEntityRenderer(renderer));
        }
    }
}
