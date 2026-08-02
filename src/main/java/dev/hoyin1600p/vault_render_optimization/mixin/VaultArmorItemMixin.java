package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.cache.VaultGearRenderCache;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import iskallia.vault.item.gear.VaultArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = VaultArmorItem.class, remap = false)
public abstract class VaultArmorItemMixin {
    @Inject(method = "getMaxDamage", at = @At("HEAD"), cancellable = true)
    private void vault_render_optimization$getMaxDamage(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (ClientOptimizationConfig.optimizationsEnabled()) {
            cir.setReturnValue(VaultGearRenderCache.getArmorMaxDamage(stack));
        }
    }

    @Inject(method = "isDamageable", at = @At("HEAD"), cancellable = true)
    private void vault_render_optimization$isDamageable(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (ClientOptimizationConfig.optimizationsEnabled()) {
            cir.setReturnValue(VaultGearRenderCache.isArmorDamageable(stack));
        }
    }

    @Inject(method = "getArmorTexture", at = @At("HEAD"), cancellable = true)
    private void vault_render_optimization$getArmorTexture(
            ItemStack stack,
            Entity entity,
            EquipmentSlot slot,
            String type,
            CallbackInfoReturnable<String> cir
    ) {
        if (ClientOptimizationConfig.optimizationsEnabled()) {
            cir.setReturnValue(VaultGearRenderCache.getArmorTexture(stack, slot, type));
        }
    }
}
