/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Behavior adapted from HoYin1600p's Embeddium stability fork, commit 0da4a463.
 */
package dev.hoyin1600p.vault_render_optimization.mixin.backport.embeddium.state;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Restores only shader color; all other GL state remains renderer-owned. */
@Mixin(value = SodiumWorldRenderer.class, remap = false)
public abstract class SodiumWorldRendererColorResetMixin {
    @Inject(method = "drawChunkLayer", at = @At("TAIL"), require = 1)
    private void vro$restoreWhiteShaderColor(CallbackInfo callback) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
