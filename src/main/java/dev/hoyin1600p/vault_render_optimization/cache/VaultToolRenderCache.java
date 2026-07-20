package dev.hoyin1600p.vault_render_optimization.cache;

import iskallia.vault.gear.attribute.type.VaultGearAttributeTypeMerger;
import iskallia.vault.gear.data.VaultGearData;
import iskallia.vault.init.ModGearAttributes;
import iskallia.vault.item.tool.ToolMaterial;
import iskallia.vault.item.tool.ToolType;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

public final class VaultToolRenderCache {
    private static final int TAG_LONG_ARRAY = 12;
    private static final String VAULT_GEAR_DATA_TAG = "vaultGearData";
    private static final long REFRESH_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(1);
    private static final long[] NO_GEAR_DATA = new long[0];

    private static final Map<ItemStack, ToolEntry> TOOL_CACHE = new WeakHashMap<>();

    private VaultToolRenderCache() {
    }

    public static ToolModels getStaticModels(ItemStack stack) {
        long now = System.nanoTime();
        long[] gearData = readGearDataKey(stack);

        synchronized (TOOL_CACHE) {
            ToolEntry entry = TOOL_CACHE.get(stack);
            if (entry != null && entry.isFreshFor(gearData, now)) {
                return entry.models;
            }

            ToolEntry refreshed = ToolEntry.compute(stack, gearData, now);
            TOOL_CACHE.put(stack, refreshed);
            return refreshed.models;
        }
    }

    private static long[] readGearDataKey(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return NO_GEAR_DATA;
        }

        Tag gearDataTag = tag.get(VAULT_GEAR_DATA_TAG);
        if (!(gearDataTag instanceof LongArrayTag longArrayTag) || gearDataTag.getId() != TAG_LONG_ARRAY) {
            return NO_GEAR_DATA;
        }

        long[] data = longArrayTag.getAsLongArray();
        return data.length == 0 ? NO_GEAR_DATA : data;
    }

    public record ToolModels(ModelResourceLocation handle, ModelResourceLocation head) {
    }

    private static final class ToolEntry {
        private final long[] gearData;
        private final long refreshedAtNanos;
        private final ToolModels models;

        private ToolEntry(long[] gearData, long refreshedAtNanos, ToolModels models) {
            this.gearData = gearData.length == 0 ? NO_GEAR_DATA : gearData.clone();
            this.refreshedAtNanos = refreshedAtNanos;
            this.models = models;
        }

        private static ToolEntry compute(ItemStack stack, long[] gearData, long now) {
            ToolType type = ToolType.of(stack);
            VaultGearData data = VaultGearData.read(stack);
            ToolMaterial material = data.get(
                    ModGearAttributes.TOOL_MATERIAL,
                    VaultGearAttributeTypeMerger.of(() -> null, (current, next) -> next)
            );

            ToolModels models = null;
            if (type != null && material != null) {
                ModelResourceLocation handle = new ModelResourceLocation(
                        "the_vault:tool/" + type.getId() + "/handle#inventory"
                );
                ModelResourceLocation head = new ModelResourceLocation(
                        "the_vault:tool/" + type.getId() + "/head/" + material.getId() + "#inventory"
                );
                models = new ToolModels(handle, head);
            }

            return new ToolEntry(gearData, now, models);
        }

        private boolean isFreshFor(long[] currentGearData, long now) {
            return now - this.refreshedAtNanos < REFRESH_INTERVAL_NANOS && Arrays.equals(this.gearData, currentGearData);
        }
    }
}
