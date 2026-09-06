package dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;

/**
 * Exact model-bearing snapshot used to separate block-model changes from
 * count/fill-only client packets. Informed by Sophisticated Storage revision
 * 93656abf01687429c4a5f4407e8f8ff10f5fc29e (GPL-3.0-only), adapted to the
 * installed 1.18.2 data layout with collision-free equality.
 */
public final class BarrelModelSignature {
    private BarrelModelSignature() {
    }

    public static Snapshot capture(BarrelBlockEntity barrel) {
        RenderInfo.ItemDisplayRenderInfo display = barrel.getStorageWrapper()
                .getRenderInfo()
                .getItemDisplayRenderInfo();
        List<DisplayItemSnapshot> items = new ArrayList<>(display.getDisplayItems().size());
        for (RenderInfo.DisplayItem item : display.getDisplayItems()) {
            ItemStack stack = item.getItem();
            CompoundTag stackTag = stack.save(new CompoundTag());
            // Stack size drives quantity/fill displays, not the baked barrel model.
            stackTag.remove("Count");
            items.add(new DisplayItemSnapshot(stackTag, item.getRotation(), item.getSlotIndex()));
        }

        return new Snapshot(
                barrel.getBlockState(),
                barrel.isPacked(),
                barrel.isLocked(),
                barrel.shouldShowLock(),
                barrel.shouldShowTier(),
                barrel.hasDynamicRenderer(),
                barrel.hasFullyDynamicRenderer(),
                barrel.getWoodType(),
                barrel.getStorageWrapper().getMainColor(),
                barrel.getStorageWrapper().getAccentColor(),
                Map.copyOf(barrel.getMaterials()),
                List.copyOf(items),
                Set.copyOf(display.getInaccessibleSlots())
        );
    }

    public record Snapshot(
            BlockState blockState,
            boolean packed,
            boolean locked,
            boolean showLock,
            boolean showTier,
            boolean dynamicRenderer,
            boolean fullyDynamicRenderer,
            Optional<WoodType> woodType,
            int mainColor,
            int accentColor,
            Map<BarrelMaterial, ResourceLocation> materials,
            List<DisplayItemSnapshot> displayItems,
            Set<Integer> inaccessibleSlots
    ) {
    }

    public record DisplayItemSnapshot(CompoundTag stackTag, int rotation, int slotIndex) {
    }
}
