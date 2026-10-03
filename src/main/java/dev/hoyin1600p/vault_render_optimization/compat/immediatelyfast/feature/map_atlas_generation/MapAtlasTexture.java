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
package dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast.feature.map_atlas_generation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast.VroImmediatelyFast;

public class MapAtlasTexture implements AutoCloseable {

    public static final int ATLAS_SIZE = 4096;
    public static final int MAP_SIZE = 128;
    public static final int MAPS_PER_ATLAS = (ATLAS_SIZE / MAP_SIZE) * (ATLAS_SIZE / MAP_SIZE);

    private final int id;
    private final ResourceLocation identifier;
    private final DynamicTexture texture;
    private int mapCount;

    public MapAtlasTexture(final int id) {
        this.id = id;

        this.identifier = new ResourceLocation(VroImmediatelyFast.NAMESPACE, "immediatelyfast_map_atlas/" + id);
        this.texture = new DynamicTexture(ATLAS_SIZE, ATLAS_SIZE, true);
        Minecraft.getInstance().getTextureManager().register(this.identifier, this.texture);
    }

    public int getNextMapLocation() {
        if (this.mapCount >= MAPS_PER_ATLAS) {
            return -1;
        }

        final byte atlasX = (byte) (this.mapCount % (ATLAS_SIZE / MAP_SIZE));
        final byte atlasY = (byte) (this.mapCount / (ATLAS_SIZE / MAP_SIZE));
        this.mapCount++;

        return (this.id << 16) | (atlasX << 8) | atlasY;
    }

    public int getId() {
        return this.id;
    }

    public ResourceLocation getIdentifier() {
        return this.identifier;
    }

    public DynamicTexture getTexture() {
        return this.texture;
    }

    @Override
    public void close() {
        this.texture.close();
        Minecraft.getInstance().getTextureManager().release(this.identifier);
    }

}