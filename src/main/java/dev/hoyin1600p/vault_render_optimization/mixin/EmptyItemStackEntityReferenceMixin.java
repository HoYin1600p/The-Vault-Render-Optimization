package dev.hoyin1600p.vault_render_optimization.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemEntity.class)
public abstract class EmptyItemStackEntityReferenceMixin {
    @Redirect(
            method = "onSyncedDataUpdated",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;setEntityRepresentation(Lnet/minecraft/world/entity/Entity;)V"
            )
    )
    private void vaultRenderOptimization$avoidSharedEmptyStackReference(ItemStack stack, Entity entity) {
        if (stack != ItemStack.EMPTY) {
            stack.setEntityRepresentation(entity);
        }
    }
}
