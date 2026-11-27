/*
 * Adapted from ImmediatelyFast (LGPL-3.0-or-later) 1.20 hud_batching/compat/itemborders/MixinItemBorders_ItemBorders.java.
 * Copyright (C) 2023-2026 RK_01/RaphiMC and contributors. Modified by HoYin1600p for VRO, 2026:
 * batches ItemBorders' multi-quad slot border fills into one fill batch when no outer HUD fill batch is active.
 */
package dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast.injection.mixins.hud_batching.compat;

import dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast.feature.batching.BatchingBuffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("UnresolvedMixinReference")
@Pseudo
@Mixin(targets = "com.anthonyhilyard.itemborders.ItemBorders", remap = false)
public abstract class MixinItemBorders {
    @Unique
    private static boolean vro$startedFillBatch;

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private static void vro$beginItemBorderBatch(CallbackInfo ci) {
        if (!BatchingBuffers.isFillBatching()) {
            BatchingBuffers.beginFillBatching();
            vro$startedFillBatch = true;
        } else {
            vro$startedFillBatch = false;
        }
    }

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private static void vro$endItemBorderBatch(CallbackInfo ci) {
        if (vro$startedFillBatch) {
            vro$startedFillBatch = false;
            BatchingBuffers.endFillBatching();
        }
    }
}
