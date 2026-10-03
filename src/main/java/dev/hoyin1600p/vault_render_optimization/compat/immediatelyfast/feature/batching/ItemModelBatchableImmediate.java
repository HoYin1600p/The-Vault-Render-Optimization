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
package dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast.feature.batching;

import dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast.feature.core.BatchableImmediate;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.inventory.InventoryMenu;

public class ItemModelBatchableImmediate extends BatchableImmediate {

    private final boolean guiDepthLighting;

    public ItemModelBatchableImmediate(final boolean guiDepthLighting) {
        super(BatchingBuffers.createLayerBuffers(
            RenderType.armorGlint(),
            RenderType.armorEntityGlint(),
            RenderType.glint(),
            RenderType.glintDirect(),
            RenderType.glintTranslucent(),
            RenderType.entityGlint(),
            RenderType.entityGlintDirect()
        ));

        this.guiDepthLighting = guiDepthLighting;
    }

    @Override
    public void endBatch() {
        RenderSystem.getModelViewStack().pushPose();
        RenderSystem.getModelViewStack().setIdentity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        RenderSystem.enableBlend();
        if (this.guiDepthLighting) {
            Lighting.setupFor3DItems();
        } else {
            Lighting.setupForFlatItems();
        }
        super.endBatch();
        if (!this.guiDepthLighting) {
            Lighting.setupFor3DItems();
        }
        RenderSystem.disableBlend();
        RenderSystem.getModelViewStack().popPose();
        RenderSystem.applyModelViewMatrix();
    }

}