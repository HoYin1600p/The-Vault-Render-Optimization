/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Behavior adapted from HoYin1600p's Embeddium stability fork, commit 0da4a463.
 * Reimplemented as an optional VRO mixin for Embeddium 0.3.18 and Rubidium 0.5.6.
 */
package dev.hoyin1600p.vault_render_optimization.mixin.backport.embeddium.occlusion;

import me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = BlockOcclusionCache.class, remap = false)
public abstract class BlockOcclusionCacheMixin {
    @Shadow @Final
    private BlockPos.MutableBlockPos cpos;

    @ModifyArg(
            method = "shouldDrawSide",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;hidesNeighborFace(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z",
                    remap = true
            ),
            index = 1,
            require = 1
    )
    private BlockPos vro$useAdjacentPosition(BlockPos ignored) {
        return this.cpos;
    }
}
