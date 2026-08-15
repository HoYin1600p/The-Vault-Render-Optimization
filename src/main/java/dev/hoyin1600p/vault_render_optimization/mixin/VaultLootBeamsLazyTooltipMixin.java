package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import me.justahuman.vaultlootbeams.client.ClientSetup;
import me.justahuman.vaultlootbeams.utils.Utils;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ClientSetup.class, remap = false)
public abstract class VaultLootBeamsLazyTooltipMixin {
    @Redirect(
            method = "onItemCreation",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/justahuman/vaultlootbeams/utils/Utils;cache(Lnet/minecraft/world/entity/item/ItemEntity;)V"
            ),
            remap = false
    )
    private static void vaultRenderOptimization$deferTooltipCreation(ItemEntity itemEntity) {
        if (!ClientOptimizationConfig.optimizationsEnabled()) {
            Utils.cache(itemEntity);
        }
    }
}
