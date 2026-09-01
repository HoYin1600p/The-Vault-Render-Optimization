package dev.hoyin1600p.vault_render_optimization.client.compat.manastealer;

import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ManaStealerVisualModule {
    private static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, VaultRenderOptimization.MOD_ID);
    private static final RegistryObject<SimpleParticleType> MANA_STEALER_ORB = PARTICLE_TYPES.register(
            "mana_stealer_orb",
            () -> new SimpleParticleType(false)
    );

    private ManaStealerVisualModule() {
    }

    public static void register(IEventBus modEventBus) {
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.CLIENT,
                ManaStealerVisualConfig.SPEC,
                "vault_render_optimization-mana-stealer-client.toml"
        );
        PARTICLE_TYPES.register(modEventBus);
        modEventBus.addListener(ManaStealerVisualConfig::onLoading);
        modEventBus.addListener(ManaStealerVisualConfig::onReloading);
        modEventBus.addListener(ManaStealerVisualModule::onParticleFactories);
        MinecraftForge.EVENT_BUS.addListener(ManaStealerVisualController::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ManaStealerDrainStreamController::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ManaStealerDrainStreamRenderer::onRenderLevelStage);
        MinecraftForge.EVENT_BUS.addListener(ManaStealerPreviewController::onClientTick);
    }

    private static void onParticleFactories(ParticleFactoryRegisterEvent event) {
        Minecraft.getInstance().particleEngine.register(MANA_STEALER_ORB.get(), sprites -> {
            ManaStealerOrbParticle.bindSprites(sprites);
            return (options, level, x, y, z, xSpeed, ySpeed, zSpeed) -> null;
        });
    }
}
