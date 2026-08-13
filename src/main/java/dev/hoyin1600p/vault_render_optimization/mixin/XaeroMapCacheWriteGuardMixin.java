package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "xaero.map.file.MapSaveLoad", remap = false)
public abstract class XaeroMapCacheWriteGuardMixin {
    @Unique
    private static final AtomicBoolean vaultRenderOptimization$reportedStaleWrite = new AtomicBoolean();

    @Redirect(
            method = "run",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/region/LeveledRegion;saveCacheTextures(Ljava/io/File;I)Z"
            )
    )
    private boolean vaultRenderOptimization$writeOnlyWhilePrepared(
            @Coerce Object regionObject,
            File tempFile,
            int extraAttempts
    ) {
        XaeroLeveledRegionAccess region = (XaeroLeveledRegionAccess) regionObject;

        synchronized (regionObject) {
            if (!region.vaultRenderOptimization$isAllCachePrepared()) {
                region.vaultRenderOptimization$deleteBuffers();
                if (vaultRenderOptimization$reportedStaleWrite.compareAndSet(false, true)) {
                    VaultRenderOptimization.LOGGER.warn(
                            "Skipped a stale Xaero map cache write after its region was invalidated; "
                                    + "later occurrences will be logged at debug level"
                    );
                } else {
                    VaultRenderOptimization.LOGGER.debug(
                            "Skipped another stale Xaero map cache write after its region was invalidated"
                    );
                }
                return false;
            }

            return region.vaultRenderOptimization$saveCacheTextures(tempFile, extraAttempts);
        }
    }
}
