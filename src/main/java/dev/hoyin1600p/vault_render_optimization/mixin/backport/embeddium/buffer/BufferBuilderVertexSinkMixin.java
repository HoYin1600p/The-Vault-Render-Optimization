/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Behavior adapted from HoYin1600p's Embeddium stability fork, commit 7071ee1c.
 */
package dev.hoyin1600p.vault_render_optimization.mixin.backport.embeddium.buffer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.hoyin1600p.vault_render_optimization.renderertransfer.NullBufferVertexSinkGuard;
import java.nio.ByteBuffer;
import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.type.VertexType;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Runs after the renderer's core BufferBuilder mixin (priority 1000) has added
 * createSink. VRO's Flywheel format mixin is priority 1010 and touches begin,
 * so the two VRO integrations do not overwrite one another.
 */
@Mixin(value = BufferBuilder.class, priority = 900)
public abstract class BufferBuilderVertexSinkMixin {
    @Shadow
    private ByteBuffer buffer;

    @Dynamic("createSink is supplied by the validated Embeddium/Rubidium core mixin")
    @Inject(
            method = "createSink(Lme/jellysquid/mods/sodium/client/model/vertex/type/VertexType;)Lme/jellysquid/mods/sodium/client/model/vertex/VertexSink;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 1
    )
    private <T extends VertexSink> void vro$fallbackWithoutBackingBuffer(
            VertexType<T> factory,
            CallbackInfoReturnable<T> callback
    ) {
        if (NullBufferVertexSinkGuard.requiresFallback(this.buffer)) {
            callback.setReturnValue(factory.createFallbackWriter((VertexConsumer) (Object) this));
        }
    }
}
