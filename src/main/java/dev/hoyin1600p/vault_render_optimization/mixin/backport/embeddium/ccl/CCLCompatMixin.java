/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Behavior adapted from HoYin1600p's Embeddium stability fork, commit 25e57646.
 */
package dev.hoyin1600p.vault_render_optimization.mixin.backport.embeddium.ccl;

import codechicken.lib.render.block.ICCBlockRenderer;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistryEntry;
import net.minecraftforge.registries.IRegistryDelegate;
import org.embeddedt.embeddium.api.BlockRendererRegistry;
import org.embeddedt.embeddium.compat.ccl.CCLCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces only Embeddium's validated CCL render-populator lambda. */
@Mixin(value = CCLCompat.class, remap = false)
public abstract class CCLCompatMixin {
    @Shadow private static Map<IRegistryDelegate<Block>, ICCBlockRenderer> customBlockRenderers;
    @Shadow private static Map<IRegistryDelegate<Fluid>, ICCBlockRenderer> customFluidRenderers;
    @Shadow private static List<ICCBlockRenderer> customGlobalRenderers;
    @Shadow private static BlockRendererRegistry.Renderer createBridge(ICCBlockRenderer renderer) {
        throw new AssertionError();
    }

    @Inject(method = "lambda$onClientSetup$2", at = @At("HEAD"), cancellable = true, require = 1)
    private static void vro$directRendererLookup(
            List<BlockRendererRegistry.Renderer> result,
            BlockState state,
            BlockPos pos,
            BlockAndTintGetter world,
            CallbackInfo callback
    ) {
        for (ICCBlockRenderer renderer : customGlobalRenderers) {
            addWhenHandled(result, renderer, world, pos, state);
        }

        Block block = state.getBlock();
        ICCBlockRenderer blockRenderer = customBlockRenderers.get(((ForgeRegistryEntry<Block>) block).delegate);
        addWhenHandled(result, blockRenderer, world, pos, state);

        Fluid fluid = state.getFluidState().getType();
        ICCBlockRenderer fluidRenderer = customFluidRenderers.get(((ForgeRegistryEntry<Fluid>) fluid).delegate);
        addWhenHandled(result, fluidRenderer, world, pos, state);

        callback.cancel();
    }

    private static void addWhenHandled(
            List<BlockRendererRegistry.Renderer> result,
            ICCBlockRenderer renderer,
            BlockAndTintGetter world,
            BlockPos pos,
            BlockState state
    ) {
        if (renderer != null && renderer.canHandleBlock(world, pos, state)) {
            result.add(createBridge(renderer));
        }
    }
}
