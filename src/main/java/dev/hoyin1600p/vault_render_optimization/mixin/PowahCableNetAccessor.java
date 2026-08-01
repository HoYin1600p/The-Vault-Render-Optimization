package dev.hoyin1600p.vault_render_optimization.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Pseudo
@Mixin(targets = "owmii.powah.block.cable.CableNet", remap = false)
public interface PowahCableNetAccessor {
    @Accessor("loadedCables")
    static Map<Object, Object> vaultRenderOptimization$getLoadedCables() {
        throw new AssertionError();
    }
}
