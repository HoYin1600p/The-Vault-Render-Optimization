package dev.hoyin1600p.vault_render_optimization.client.config;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import dev.hoyin1600p.vault_render_optimization.util.ModIds;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModList;

/**
 * The only way to open VRO's settings screen: an unbound key in Controls under VRO's category.
 * Cloth Config is optional; without it the key explains where the settings still live.
 */
public final class ConfigScreenKey {
    public static final String CLOTH_CONFIG_MOD_ID = ModIds.CLOTH_CONFIG;
    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.vault_render_optimization.open_config",
            KeyConflictContext.IN_GAME,
            InputConstants.UNKNOWN,
            "key.categories.vault_render_optimization"
    );

    private ConfigScreenKey() {
    }

    public static void register() {
        ClientRegistry.registerKeyBinding(OPEN_CONFIG);
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        while (OPEN_CONFIG.consumeClick()) {
            open();
        }
    }

    private static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ModList.get().isLoaded(CLOTH_CONFIG_MOD_ID)) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        new TranslatableComponent("vro.config.message.cloth_missing"), false);
            }
            return;
        }
        try {
            // Only reached with Cloth installed, so its classes resolve.
            dev.hoyin1600p.vault_render_optimization.client.config.cloth.VroClothConfigScreen.open(minecraft.screen);
        } catch (LinkageError | RuntimeException incompatible) {
            // Built against Cloth 6.5.102; an older or changed Cloth API (a missing class, or one that now
            // throws while the screen is built) must not crash the game.
            VaultRenderOptimization.LOGGER.warn("Could not open the settings screen with this Cloth Config", incompatible);
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        new TranslatableComponent("vro.config.message.cloth_incompatible"), false);
            }
        }
    }
}
