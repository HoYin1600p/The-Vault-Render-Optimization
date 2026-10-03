/*
 * This file is part of ImmediatelyFast Reforged - https://github.com/CCr4ft3r/ImmediatelyFastReforged
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Modified by HoYin1600p for The Vault Render Optimization (VRO), 2026: relocated from
 * net.raphimc.immediatelyfast, configuration and Oculus lookup moved to VRO-owned classes,
 * and disabled when the standalone ImmediatelyFast mod is installed. Source: ImmediatelyFast
 * Reforged 1.18.2 branch, commit 53d41ab043eacf4664be70d54b18b23b690e537e (version 1.1.10).
 */
package dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast.injection.mixins.fast_buffer_upload;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import org.lwjgl.opengl.GL15C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;

@Mixin(value = VertexBuffer.class, priority = 500)
public abstract class MixinVertexBuffer {

    @Unique
    private int vertexBufferSize;

    @Unique
    private int indexBufferSize;

    @Redirect(method = "upload_", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;glBufferData(ILjava/nio/ByteBuffer;I)V", ordinal = 0))
    private void optimizeVertexDataUploading(int target, ByteBuffer data, int usage) {
        if (data.remaining() > this.vertexBufferSize) {
            this.vertexBufferSize = data.remaining();
            RenderSystem.glBufferData(target, data, GL15C.GL_DYNAMIC_DRAW);
        } else {
            GL15C.glBufferSubData(target, 0, data);
        }
    }

    @Redirect(method = "upload_", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;glBufferData(ILjava/nio/ByteBuffer;I)V", ordinal = 1))
    private void optimizeIndexDataUploading(int target, ByteBuffer data, int usage) {
        if (data.remaining() > this.indexBufferSize) {
            this.indexBufferSize = data.remaining();
            RenderSystem.glBufferData(target, data, GL15C.GL_DYNAMIC_DRAW);
        } else {
            GL15C.glBufferSubData(target, 0, data);
        }
    }
}