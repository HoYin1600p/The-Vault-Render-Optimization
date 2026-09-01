/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Behavior adapted from HoYin1600p's Embeddium stability fork, commit 7071ee1c.
 * VRO adds a configurable retained-capacity ceiling and keeps destroy() cleanup.
 */
package dev.hoyin1600p.vault_render_optimization.mixin.backport.embeddium.buffer;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import dev.hoyin1600p.vault_render_optimization.renderertransfer.VertexBufferRetentionPolicy;
import java.nio.ByteBuffer;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VertexBufferBuilder.class, remap = false)
public abstract class VertexBufferBuilderMixin {
    @Shadow @Final private int initialCapacity;
    @Shadow private ByteBuffer buffer;
    @Shadow private int writerOffset;
    @Shadow private int capacity;
    @Shadow private void setBufferSize(int capacity) { throw new AssertionError(); }

    @Inject(method = "grow", at = @At("HEAD"), cancellable = true, require = 1)
    private void vro$growFromWriterEnd(int requestedLength, CallbackInfo callback) {
        this.setBufferSize(VertexBufferRetentionPolicy.nextCapacity(
                this.capacity, this.writerOffset, requestedLength
        ));
        callback.cancel();
    }

    @Inject(method = "start", at = @At("HEAD"), require = 1)
    private void vro$trimPathologicalRetainedPeak(CallbackInfo callback) {
        int limit = VertexBufferRetentionPolicy.retainedCapacityLimitBytes(
                ClientOptimizationConfig.vertexBufferMaxRetainedMib,
                this.initialCapacity
        );
        if (this.buffer != null && this.capacity > limit) {
            this.setBufferSize(limit);
        }
    }

    @Redirect(
            method = "start",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/model/vertex/buffer/VertexBufferBuilder;setBufferSize(I)V"
            ),
            require = 0
    )
    private void vro$retainAllocatedBuffer(VertexBufferBuilder ignored, int requestedCapacity) {
        if (this.buffer == null || this.capacity < this.initialCapacity) {
            this.setBufferSize(requestedCapacity);
        }
    }
}
