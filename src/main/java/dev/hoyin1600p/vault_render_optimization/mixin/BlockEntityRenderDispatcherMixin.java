package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.render.CachedBlockEntityRendererHolder;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Registry;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = BlockEntityRenderDispatcher.class, priority = 700)
public abstract class BlockEntityRenderDispatcherMixin {
    @Shadow
    private Map<BlockEntityType<?>, BlockEntityRenderer<?>> renderers;

    @Inject(method = "getRenderer", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <E extends BlockEntity> void vro$getCachedRenderer(E blockEntity,
                                                               CallbackInfoReturnable<BlockEntityRenderer<E>> cir) {
        if (!ClientOptimizationConfig.optimizationsEnabled()
                || !ClientOptimizationConfig.blockEntityRendererCache) {
            return;
        }

        CachedBlockEntityRendererHolder holder =
                (CachedBlockEntityRendererHolder) (Object) blockEntity.getType();
        BlockEntityRenderer<?> renderer = holder.vro$getBlockEntityRenderer();
        if (renderer == null) {
            renderer = this.renderers.get(blockEntity.getType());
            holder.vro$setBlockEntityRenderer(renderer);
        }
        if (renderer != null) {
            cir.setReturnValue((BlockEntityRenderer) renderer);
        }
    }

    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    private void vro$refreshRendererCache(ResourceManager resourceManager, CallbackInfo ci) {
        for (BlockEntityType<?> blockEntityType : Registry.BLOCK_ENTITY_TYPE) {
            ((CachedBlockEntityRendererHolder) (Object) blockEntityType).vro$setBlockEntityRenderer(null);
        }
        if (ClientOptimizationConfig.optimizationsEnabled()
                && ClientOptimizationConfig.blockEntityRendererCache) {
            this.renderers.forEach((type, renderer) ->
                    ((CachedBlockEntityRendererHolder) (Object) type).vro$setBlockEntityRenderer(renderer));
        }
    }
}
