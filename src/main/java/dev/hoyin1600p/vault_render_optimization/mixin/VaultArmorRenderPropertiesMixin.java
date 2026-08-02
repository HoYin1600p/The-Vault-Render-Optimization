package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import iskallia.vault.dynamodel.model.armor.ArmorPieceModel;
import iskallia.vault.gear.data.GearDataCache;
import iskallia.vault.gear.renderer.VaultArmorRenderProperties;
import iskallia.vault.init.ModDynamicModels;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = VaultArmorRenderProperties.class, remap = false)
public abstract class VaultArmorRenderPropertiesMixin {
    @Inject(method = "getArmorModel", at = @At("HEAD"), cancellable = true)
    private void vault_render_optimization$reuseArmorModel(
            LivingEntity entity,
            ItemStack stack,
            EquipmentSlot slot,
            HumanoidModel<?> fallbackModel,
            CallbackInfoReturnable<HumanoidModel<?>> cir
    ) {
        if (!ClientOptimizationConfig.optimizationsEnabled()) {
            return;
        }
        ResourceLocation modelId = GearDataCache.of(stack).getGearModel().orElse(null);
        if (modelId == null) {
            cir.setReturnValue(null);
            return;
        }

        VaultArmorRenderProperties.ArmorLayerFactory factory = ModDynamicModels.Armor.PIECE_REGISTRY
                .get(modelId)
                .map(ArmorPieceModel::getId)
                .map(VaultArmorRenderProperties.BAKED_LAYERS::get)
                .orElse(null);

        cir.setReturnValue(factory == null ? null : factory.getOrCreateModel(entity, fallbackModel));
    }
}
