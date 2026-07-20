package dev.hoyin1600p.vault_render_optimization.cache;

import iskallia.vault.gear.VaultGearState;
import iskallia.vault.gear.attribute.type.VaultGearAttributeTypeMerger;
import iskallia.vault.gear.data.GearDataCache;
import iskallia.vault.gear.data.VaultGearData;
import iskallia.vault.init.ModGearAttributes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
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

    public static String getArmorTexture(ItemStack stack, EquipmentSlot slot, String type) {
        long now = System.nanoTime();
        long[] gearData = readGearDataKey(stack);

        synchronized (ARMOR_CACHE) {
            ArmorEntry entry = getArmorEntryLocked(stack, gearData, now);
            return entry.getTexture(slot, type);
        }
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
            return getArmorEntryLocked(stack, gearData, now);
        }
    }

    private static ArmorEntry getArmorEntryLocked(ItemStack stack, long[] gearData, long now) {
        ArmorEntry entry = ARMOR_CACHE.get(stack);
        if (entry != null && entry.isFreshFor(gearData, now)) {
            return entry;
        }

        ArmorEntry refreshed = ArmorEntry.compute(stack, gearData, now);
        ARMOR_CACHE.put(stack, refreshed);
        return refreshed;
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

    private static final class ArmorEntry {
        private final long[] gearData;
        private final long refreshedAtNanos;
        private final int maxDamage;
        private final ResourceLocation gearModel;
        private final Map<TextureKey, String> textures = new HashMap<>();

        private ArmorEntry(long[] gearData, long refreshedAtNanos, int maxDamage, ResourceLocation gearModel) {
            this.gearData = gearData.length == 0 ? NO_GEAR_DATA : gearData.clone();
            this.refreshedAtNanos = refreshedAtNanos;
            this.maxDamage = maxDamage;
            this.gearModel = gearModel;
        }

        private static ArmorEntry compute(ItemStack stack, long[] gearData, long now) {
            VaultGearData data = VaultGearData.read(stack);
            int maxDamage = data.get(ModGearAttributes.DURABILITY, VaultGearAttributeTypeMerger.intSum());
            ResourceLocation gearModel = GearDataCache.of(stack).getGearModel().orElse(null);

            return new ArmorEntry(gearData, now, maxDamage, gearModel);
        }

        private boolean isFreshFor(long[] currentGearData, long now) {
            return now - this.refreshedAtNanos < REFRESH_INTERVAL_NANOS && Arrays.equals(this.gearData, currentGearData);
        }

        private String getTexture(EquipmentSlot slot, String type) {
            if (this.gearModel == null) {
                return "";
            }

            TextureKey key = new TextureKey(slot == EquipmentSlot.LEGS, type);
            return this.textures.computeIfAbsent(key, ignored -> buildTexture(this.gearModel, slot, type));
        }

        private static String buildTexture(ResourceLocation model, EquipmentSlot slot, String type) {
            String modelTexture = model.getNamespace() + ":textures/item/" + model.getPath();
            int finalSlash = modelTexture.lastIndexOf('/');
            if (finalSlash < 0) {
                return "";
            }

            String texture = modelTexture.substring(0, finalSlash)
                    + "/armor"
                    + (slot == EquipmentSlot.LEGS ? "_layer2" : "_layer1");
            if (type != null) {
                texture += "_" + type;
            }
            return texture + ".png";
        }
    }

    private record TextureKey(boolean leggings, String type) {
    }
}
