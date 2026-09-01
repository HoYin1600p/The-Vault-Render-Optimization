/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Behavior adapted from HoYin1600p's Embeddium stability fork, commit 0da4a463,
 * itself derived from newer Embeddium arena-resize work.
 */
package dev.hoyin1600p.vault_render_optimization.mixin.backport.embeddium.buffer;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import dev.hoyin1600p.vault_render_optimization.renderertransfer.AsyncArenaGrowthPolicy;
import me.jellysquid.mods.sodium.client.gl.arena.AsyncBufferArena;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = AsyncBufferArena.class, remap = false)
public abstract class AsyncBufferArenaMixin {
    @Shadow private int capacity;
    @Shadow private int used;

    @ModifyArg(
            method = "ensureCapacity",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/gl/arena/AsyncBufferArena;resize(Lme/jellysquid/mods/sodium/client/gl/device/CommandList;I)V"
            ),
            index = 1,
            require = 1
    )
    private int vro$preemptiveBoundedGrowth(
            int originalCapacity,
            CommandList commandList,
            int requestedElements
    ) {
        return AsyncArenaGrowthPolicy.nextCapacity(
                this.capacity,
                this.used,
                requestedElements,
                ClientOptimizationConfig.asyncArenaGrowthDivisor,
                ClientOptimizationConfig.asyncArenaMaxHeadroomMib
        );
    }
}
