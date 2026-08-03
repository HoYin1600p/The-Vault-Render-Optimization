package dev.hoyin1600p.vault_render_optimization.client.lighting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.HashMap;
import java.util.Map;

public final class DynamicLightResourceLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    public static final DynamicLightResourceLoader INSTANCE = new DynamicLightResourceLoader();

    private volatile Map<Item, ItemDefinition> itemDefinitions = Map.of();
    private volatile Map<BlockEntityType<?>, Integer> blockEntityDefinitions = Map.of();

    private DynamicLightResourceLoader() {
        super(GSON, "vro_dynamic_lights");
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> resources,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        Map<Item, ItemDefinition> loadedItems = new HashMap<>();
        Map<BlockEntityType<?>, Integer> loadedBlockEntities = new HashMap<>();

        resources.forEach((resourceId, element) -> {
            try {
                JsonObject root = GsonHelper.convertToJsonObject(element, resourceId.toString());
                JsonArray entries = GsonHelper.getAsJsonArray(root, "entries");
                for (JsonElement entryElement : entries) {
                    loadEntry(resourceId, GsonHelper.convertToJsonObject(entryElement, "entry"),
                            loadedItems, loadedBlockEntities);
                }
            } catch (RuntimeException exception) {
                VaultRenderOptimization.LOGGER.warn(
                        "Ignored invalid dynamic-light definitions in {}: {}",
                        resourceId,
                        exception.getMessage()
                );
            }
        });

        this.itemDefinitions = Map.copyOf(loadedItems);
        this.blockEntityDefinitions = Map.copyOf(loadedBlockEntities);
        VaultRenderOptimization.LOGGER.info(
                "Loaded {} dynamic-light item definitions and {} block-entity definitions",
                loadedItems.size(),
                loadedBlockEntities.size()
        );
    }

    private static void loadEntry(
            ResourceLocation resourceId,
            JsonObject entry,
            Map<Item, ItemDefinition> items,
            Map<BlockEntityType<?>, Integer> blockEntities
    ) {
        int luminance = Math.max(0, Math.min(15, GsonHelper.getAsInt(entry, "luminance")));
        if (entry.has("item")) {
            ResourceLocation itemId = requireResourceLocation(GsonHelper.getAsString(entry, "item"));
            Item item = Registry.ITEM.get(itemId);
            if (item == Items.AIR) {
                VaultRenderOptimization.LOGGER.debug("Dynamic-light item {} from {} is not installed", itemId, resourceId);
                return;
            }
            boolean waterSensitive = GsonHelper.getAsBoolean(entry, "water_sensitive", false);
            items.put(item, new ItemDefinition(luminance, waterSensitive));
            return;
        }

        if (entry.has("block_entity")) {
            ResourceLocation typeId = requireResourceLocation(GsonHelper.getAsString(entry, "block_entity"));
            BlockEntityType<?> type = Registry.BLOCK_ENTITY_TYPE.get(typeId);
            if (type == null) {
                VaultRenderOptimization.LOGGER.debug(
                        "Dynamic-light block entity {} from {} is not installed", typeId, resourceId);
                return;
            }
            blockEntities.put(type, luminance);
            return;
        }

        throw new IllegalArgumentException("entry must define item or block_entity");
    }

    private static ResourceLocation requireResourceLocation(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            throw new IllegalArgumentException("invalid resource location: " + value);
        }
        return id;
    }

    public int itemLuminance(ItemStack stack, boolean submerged) {
        if (stack.isEmpty()) {
            return 0;
        }

        ItemDefinition definition = this.itemDefinitions.get(stack.getItem());
        if (definition != null) {
            return definition.waterSensitive && submerged ? 0 : definition.luminance;
        }

        if (stack.getItem() instanceof BlockItem blockItem) {
            return blockItem.getBlock().defaultBlockState().getLightEmission();
        }
        return 0;
    }

    public int blockEntityLuminance(BlockEntity blockEntity) {
        return this.blockEntityDefinitions.getOrDefault(blockEntity.getType(), 0);
    }

    public int itemDefinitionCount() {
        return this.itemDefinitions.size();
    }

    public int blockEntityDefinitionCount() {
        return this.blockEntityDefinitions.size();
    }

    private record ItemDefinition(int luminance, boolean waterSensitive) {
    }
}
