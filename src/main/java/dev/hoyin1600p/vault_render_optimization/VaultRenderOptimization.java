package dev.hoyin1600p.vault_render_optimization;

import dev.hoyin1600p.vault_render_optimization.client.VaultRenderOptimizationCommand;
import dev.hoyin1600p.vault_render_optimization.client.create.FlywheelBackendManager;
import dev.hoyin1600p.vault_render_optimization.client.lighting.DynamicLightEngine;
import dev.hoyin1600p.vault_render_optimization.client.lighting.DynamicLightResourceLoader;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(VaultRenderOptimization.MOD_ID)
public final class VaultRenderOptimization {
    public static final String MOD_ID = "vault_render_optimization";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public VaultRenderOptimization() {
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.CLIENT,
                ClientOptimizationConfig.SPEC,
                "vault_render_optimization-client.toml"
        );

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(ClientOptimizationConfig::onLoading);
        modEventBus.addListener(ClientOptimizationConfig::onReloading);
        modEventBus.addListener(this::onRegisterClientReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterClientCommands);
        MinecraftForge.EVENT_BUS.addListener(DynamicLightEngine::onClientTick);
        if (supportsFlywheelBackendManagement()) {
            MinecraftForge.EVENT_BUS.addListener(FlywheelBackendManager::onClientTick);
        }
    }

    private void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        VaultRenderOptimizationCommand.register(event.getDispatcher());
    }

    private void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(DynamicLightResourceLoader.INSTANCE);
    }

    private static boolean supportsFlywheelBackendManagement() {
        if (!ModList.get().isLoaded("create") || !ModList.get().isLoaded("flywheel")) {
            return false;
        }
        return ModList.get().getModContainerById("flywheel")
                .map(container -> container.getModInfo().getVersion().toString().startsWith("0.6.11"))
                .orElse(false);
    }
}
