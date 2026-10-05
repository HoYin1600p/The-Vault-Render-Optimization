package dev.hoyin1600p.vault_render_optimization;

import dev.hoyin1600p.vault_render_optimization.client.command.VaultRenderOptimizationCommand;
import dev.hoyin1600p.vault_render_optimization.client.benchmark.BenchmarkFrameHook;
import dev.hoyin1600p.vault_render_optimization.client.config.ConfigScreenKey;
import dev.hoyin1600p.vault_render_optimization.client.chunk.residency.FarsightChunkBound;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuEntityModels;
import dev.hoyin1600p.vault_render_optimization.client.diagnostics.ParticleStress;
import dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast.VroImmediatelyFast;
import dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast.compat.IrisCompat;
import dev.hoyin1600p.vault_render_optimization.util.ConfigKeys;
import dev.hoyin1600p.vault_render_optimization.util.ModIds;
import net.minecraftforge.fml.loading.FMLEnvironment;
import dev.hoyin1600p.vault_render_optimization.client.hud.HudTextGeometry;
import net.minecraftforge.eventbus.api.EventPriority;
import dev.hoyin1600p.vault_render_optimization.client.create.FlywheelBackendManager;
import dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage.LimitedBarrelCountRenderer;
import dev.hoyin1600p.vault_render_optimization.client.update.UpdateNoticeService;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import dev.hoyin1600p.vault_render_optimization.config.ConfigChangeListeners;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.FlywheelShaderCompatSwitch;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(VaultRenderOptimization.MOD_ID)
public final class VaultRenderOptimization {
    public static final String MOD_ID = "vault_render_optimization";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public VaultRenderOptimization() {
        ConfigChangeListeners.onUpdateChecksChanged(UpdateNoticeService::setEnabled);
        ConfigChangeListeners.onUpdateFilterChanged(UpdateNoticeService::setFilter);
        ConfigChangeListeners.onHudTextGeometryChanged(HudTextGeometry::clear);
        FlywheelShaderCompatSwitch.register(() -> ClientOptimizationConfig.optimizationsEnabled()
                && ClientOptimizationConfig.createFlywheelShaderCompat);

        ModLoadingContext.get().registerConfig(
                ModConfig.Type.CLIENT,
                ClientOptimizationConfig.SPEC,
                ConfigKeys.CLIENT_CONFIG_FILE
        );

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(ClientOptimizationConfig::onLoading);
        modEventBus.addListener(ClientOptimizationConfig::onReloading);
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterClientReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterClientCommands);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, HudTextGeometry::begin);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, HudTextGeometry::end);
        MinecraftForge.EVENT_BUS.addListener(HudTextGeometry::frameBoundary);
        if (supportsFlywheelBackendManagement()) {
            dev.hoyin1600p.vault_render_optimization.client.create.FlywheelBackendOverride.enableManagement();
            MinecraftForge.EVENT_BUS.addListener(FlywheelBackendManager::onClientTick);
        }
        if (FMLEnvironment.dist.isClient()) {
            MinecraftForge.EVENT_BUS.addListener(BenchmarkFrameHook::onRenderTick);
            MinecraftForge.EVENT_BUS.addListener(BenchmarkFrameHook::onClientTick);
            MinecraftForge.EVENT_BUS.addListener(BenchmarkFrameHook::onWorldUnload);
            MinecraftForge.EVENT_BUS.addListener(BenchmarkFrameHook::onLogout);
            MinecraftForge.EVENT_BUS.addListener(ParticleStress::onClientTick);
            MinecraftForge.EVENT_BUS.addListener(ConfigScreenKey::onClientTick);
            if (VroImmediatelyFast.applied() && ModList.get().isLoaded(ModIds.OCULUS)) {
                IrisCompat.init();
            }
        }
        if (FMLEnvironment.dist.isClient() && ModList.get().isLoaded(ModIds.FARSIGHT_VIEW)) {
            // Same gate as ClientPacketListenerFarsightMixin; the jar's farsight-chunk-bound marker
            // tells VH Accelerator to leave this bound to VRO.
            MinecraftForge.EVENT_BUS.addListener(FarsightChunkBound::onClientTick);
        }
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ConfigScreenKey::register);
        event.enqueueWork(() -> UpdateNoticeService.initialize(
                MOD_ID,
                "VRO",
                "https://raw.githubusercontent.com/HoYin1600p/The-Vault-Render-Optimization/main/update.json",
                "https://www.curseforge.com/minecraft/mc-mods/vault-render-optimization",
                ClientOptimizationConfig.updateChecksEnabled(),
                ClientOptimizationConfig.updateNoticeFilter()
        ));
    }

    private void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        VaultRenderOptimizationCommand.register(event.getDispatcher());
    }

    private void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> HudTextGeometry.clear());
        event.registerReloadListener((ResourceManagerReloadListener) manager ->
                GpuEntityModels.requestReset());
        if (ModList.get().isLoaded(ModIds.SOPHISTICATED_STORAGE)) {
            event.registerReloadListener((ResourceManagerReloadListener) resourceManager ->
                    LimitedBarrelCountRenderer.clear());
        }
    }

    private static boolean supportsFlywheelBackendManagement() {
        if (!ModList.get().isLoaded(ModIds.CREATE) || !ModList.get().isLoaded(ModIds.FLYWHEEL)) {
            return false;
        }
        return ModList.get().getModContainerById(ModIds.FLYWHEEL)
                .map(container -> container.getModInfo().getVersion().toString().startsWith("0.6.11"))
                .orElse(false);
    }
}
