package dev.hoyin1600p.vault_render_optimization.mixin;

import me.justahuman.vaultlootbeams.utils.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(value = Utils.class, remap = false)
public interface VaultLootBeamsCacheAccessor {
    @Accessor("TOOLTIP_CACHE")
    static Map<ItemEntity, List<Component>> vaultRenderOptimization$getTooltipCache() {
        throw new AssertionError();
    }
}
