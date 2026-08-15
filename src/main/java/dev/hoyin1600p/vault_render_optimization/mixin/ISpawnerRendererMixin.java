package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import iskallia.ispawner.block.SurvivalSpawnerBlock;
import iskallia.ispawner.block.entity.SpawnerBlockEntity;
import iskallia.ispawner.block.render.SpawnerBlockRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SpawnerBlockRenderer.class, remap = false)
public abstract class ISpawnerRendererMixin {
    @Inject(method = "getRenderedItem", at = @At("HEAD"), cancellable = true, remap = false)
    private void vaultRenderOptimization$selectDisplayedItemWithoutStreams(SpawnerBlockEntity spawner,
                                                                            CallbackInfoReturnable<ItemStack> cir) {
        if (!ClientOptimizationConfig.optimizationsEnabled()) {
            return;
        }

        int occupiedSlots = 0;
        int slotCount = spawner.inventory.getContainerSize();
        for (int slot = 0; slot < slotCount; slot++) {
            if (!spawner.inventory.getItem(slot).isEmpty()) {
                occupiedSlots++;
            }
        }

        if (occupiedSlots == 0 || spawner.getLevel() == null) {
            cir.setReturnValue(null);
            return;
        }

        int selected = (int) ((spawner.getLevel().getGameTime() / 40L) % occupiedSlots);
        for (int slot = 0; slot < slotCount; slot++) {
            ItemStack stack = spawner.inventory.getItem(slot);
            if (!stack.isEmpty() && selected-- == 0) {
                cir.setReturnValue(stack.copy());
                return;
            }
        }

        cir.setReturnValue(null);
    }

    @Inject(method = "rendersOutsideBoundingBox", at = @At("HEAD"), cancellable = true, remap = false)
    private void vaultRenderOptimization$allowSpawnerFrustumCulling(SpawnerBlockEntity spawner,
                                                                    CallbackInfoReturnable<Boolean> cir) {
        if (ClientOptimizationConfig.optimizationsEnabled() && !isSurvivalSpawner(spawner)) {
            cir.setReturnValue(false);
        }
    }

    private static boolean isSurvivalSpawner(SpawnerBlockEntity spawner) {
        return spawner.getBlockState().getBlock() instanceof SurvivalSpawnerBlock;
    }
}
