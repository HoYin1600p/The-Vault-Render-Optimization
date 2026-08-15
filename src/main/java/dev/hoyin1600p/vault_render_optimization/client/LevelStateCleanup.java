package dev.hoyin1600p.vault_render_optimization.client;

import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import dev.hoyin1600p.vault_render_optimization.client.lighting.DynamicLightEngine;
import dev.hoyin1600p.vault_render_optimization.mixin.CreateAdditionEnergyNetworkManagerAccessor;
import dev.hoyin1600p.vault_render_optimization.mixin.PowahCableNetAccessor;
import dev.hoyin1600p.vault_render_optimization.mixin.VaultLootBeamsCacheAccessor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = VaultRenderOptimization.MOD_ID, value = Dist.CLIENT)
public final class LevelStateCleanup {
    private LevelStateCleanup() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onWorldUnload(WorldEvent.Unload event) {
        Object world = event.getWorld();

        DynamicLightEngine.clearIfWorld(world);

        if (ModList.get().isLoaded("createaddition")) {
            removeWorld(CreateAdditionEnergyNetworkManagerAccessor.vaultRenderOptimization$getInstances(), world,
                    "Create Addition energy networks");
        }

        if (ModList.get().isLoaded("powah")) {
            removeWorld(PowahCableNetAccessor.vaultRenderOptimization$getLoadedCables(), world,
                    "Powah cable networks");
        }

        if (ModList.get().isLoaded("vaultlootbeams") && !ModList.get().isLoaded("unobtainium")) {
            Map<?, ?> tooltipCache = VaultLootBeamsCacheAccessor.vaultRenderOptimization$getTooltipCache();
            if (tooltipCache != null && !tooltipCache.isEmpty()) {
                int entries = tooltipCache.size();
                tooltipCache.clear();
                VaultRenderOptimization.LOGGER.debug(
                        "Released {} Vault Loot Beams tooltip entries for an unloaded world", entries);
            }
        }
    }

    private static void removeWorld(Map<Object, Object> worlds, Object world, String description) {
        if (worlds != null && worlds.remove(world) != null) {
            VaultRenderOptimization.LOGGER.debug("Released {} for an unloaded world", description);
        }
    }
}
