package dev.hoyin1600p.vault_render_optimization.mixin;

import java.io.File;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "xaero.map.region.LeveledRegion", remap = false)
public interface XaeroLeveledRegionAccess {
    @Invoker("isAllCachePrepared")
    boolean vaultRenderOptimization$isAllCachePrepared();

    @Invoker("deleteBuffers")
    void vaultRenderOptimization$deleteBuffers();

    @Invoker("saveCacheTextures")
    boolean vaultRenderOptimization$saveCacheTextures(File tempFile, int extraAttempts);
}
