package dev.hoyin1600p.vault_render_optimization.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Pseudo
@Mixin(targets = "com.mrh0.createaddition.energy.network.EnergyNetworkManager", remap = false)
public interface CreateAdditionEnergyNetworkManagerAccessor {
    @Accessor("instances")
    static Map<Object, Object> vaultRenderOptimization$getInstances() {
        throw new AssertionError();
    }
}
