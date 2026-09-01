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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AsyncBufferArena.class, remap = false)
public abstract class AsyncBufferArenaMixin {
    @Shadow private int capacity;
    @Shadow private int used;
    @Unique private int vro$requestedElements;
    @Unique private int vro$growthIncrement;

    @Inject(method = "ensureCapacity", at = @At("HEAD"), require = 1)
    private void vro$captureRequestedElements(
            CommandList commandList,
            int requestedElements,
            CallbackInfo callback
    ) {
        this.vro$requestedElements = requestedElements;
        if (this.vro$growthIncrement == 0) {
            this.vro$growthIncrement = AsyncArenaGrowthPolicy.fixedGrowthIncrement(
                    this.capacity,
                    ClientOptimizationConfig.asyncArenaGrowthDivisor,
                    ClientOptimizationConfig.asyncArenaMaxHeadroomMib
            );
        }
    }

    @ModifyArg(
            method = "ensureCapacity",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/gl/arena/AsyncBufferArena;resize(Lme/jellysquid/mods/sodium/client/gl/device/CommandList;I)V"
            ),
            index = 1,
            require = 1
    )
    private int vro$preemptiveBoundedGrowth(int originalCapacity) {
        return AsyncArenaGrowthPolicy.nextCapacity(
                this.capacity,
                this.used,
                this.vro$requestedElements,
                this.vro$growthIncrement
        );
    }
}
