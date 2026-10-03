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
package dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast.feature.core;

import dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast.injection.interfaces.IBufferBuilder;
import com.mojang.blaze3d.vertex.BufferBuilder;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

public class BufferBuilderPool {

    private static final int INITIAL_SIZE = 256;
    private static final Set<Pair<BufferBuilder, Long>> POOL = new ReferenceArraySet<>(INITIAL_SIZE);

    private static long lastCleanup = 0;

    private BufferBuilderPool() {
    }

    public static BufferBuilder get() {
        if (lastCleanup < System.currentTimeMillis() - 5_000) {
            lastCleanup = System.currentTimeMillis();
            cleanup();
        }

        for (Pair<BufferBuilder, Long> entry : POOL) {
            final BufferBuilder bufferBuilder = entry.getKey();
            if (!bufferBuilder.building() && !((IBufferBuilder) bufferBuilder).isReleased()) {
                entry.setValue(System.currentTimeMillis());
                return bufferBuilder;
            }
        }

        final BufferBuilder bufferBuilder = new BufferBuilder(256);
        POOL.add(new MutablePair<>(bufferBuilder, System.currentTimeMillis()));
        return bufferBuilder;
    }

    public static int getAllocatedSize() {
        cleanup();
        return POOL.size();
    }

    private static void cleanup() {
        POOL.removeIf(b -> ((IBufferBuilder) b.getKey()).isReleased());
        POOL.removeIf(b -> {
            if (b.getValue() < System.currentTimeMillis() - 120_000) {
                ((IBufferBuilder) b.getKey()).release();
                return true;
            }
            return false;
        });
    }

}