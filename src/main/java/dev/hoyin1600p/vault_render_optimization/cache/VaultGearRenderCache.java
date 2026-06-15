package dev.hoyin1600p.vault_render_optimization.cache;

import iskallia.vault.gear.VaultGearState;
import iskallia.vault.gear.attribute.type.VaultGearAttributeTypeMerger;
import iskallia.vault.gear.data.GearDataCache;
import iskallia.vault.gear.data.VaultGearData;
import iskallia.vault.init.ModGearAttributes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

public final class VaultGearRenderCache {
    private static final int TAG_LONG_ARRAY = 12;
    private static final String VAULT_GEAR_DATA_TAG = "vaultGearData";
    private static final long REFRESH_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(1);
    private static final long[] NO_GEAR_DATA = new long[0];

    private static final Map<ItemStack, ArmorEntry> ARMOR_CACHE = new WeakHashMap<>();

    private VaultGearRenderCache() {
    }

    public static int getArmorMaxDamage(ItemStack stack) {
        return getArmorEntry(stack).maxDamage;
    }

    public static boolean isArmorDamageable(ItemStack stack) {
        return GearDataCache.of(stack).getState() == VaultGearState.IDENTIFIED;
    }

    public static void clear() {
        synchronized (ARMOR_CACHE) {
            ARMOR_CACHE.clear();
        }
    }

    private static ArmorEntry getArmorEntry(ItemStack stack) {
        long now = System.nanoTime();
        long[] gearData = readGearDataKey(stack);

        synchronized (ARMOR_CACHE) {
            ArmorEntry entry = ARMOR_CACHE.get(stack);
            if (entry != null && entry.isFreshFor(gearData, now)) {
                return entry;
            }

            ArmorEntry refreshed = ArmorEntry.compute(stack, gearData, now);
            ARMOR_CACHE.put(stack, refreshed);
            return refreshed;
        }
    }

    private static long[] readGearDataKey(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(VAULT_GEAR_DATA_TAG, TAG_LONG_ARRAY)) {
            return NO_GEAR_DATA;
        }

        long[] data = tag.getLongArray(VAULT_GEAR_DATA_TAG);
        return data.length == 0 ? NO_GEAR_DATA : data;
    }

    private static final class ArmorEntry {
        private final long[] gearData;
        private final long refreshedAtNanos;
        private final int maxDamage;

        private ArmorEntry(long[] gearData, long refreshedAtNanos, int maxDamage) {
            this.gearData = gearData.length == 0 ? NO_GEAR_DATA : gearData.clone();
            this.refreshedAtNanos = refreshedAtNanos;
            this.maxDamage = maxDamage;
        }

        private static ArmorEntry compute(ItemStack stack, long[] gearData, long now) {
            VaultGearData data = VaultGearData.read(stack);
            int maxDamage = data.get(ModGearAttributes.DURABILITY, VaultGearAttributeTypeMerger.intSum());

            return new ArmorEntry(gearData, now, maxDamage);
        }

        private boolean isFreshFor(long[] currentGearData, long now) {
            return now - this.refreshedAtNanos < REFRESH_INTERVAL_NANOS && Arrays.equals(this.gearData, currentGearData);
        }
    }
}
