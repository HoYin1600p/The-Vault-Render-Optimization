/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Behavior adapted from HoYin1600p's Embeddium stability fork, commits
 * 7071ee1c and 075e1886.
 */
package dev.hoyin1600p.vault_render_optimization.mixin.backport.embeddium.fluid;

import dev.hoyin1600p.vault_render_optimization.renderertransfer.SmoothFluidLightingCache;
import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadViewMutable;
import me.jellysquid.mods.sodium.client.model.quad.blender.ColorSampler;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.pipeline.FluidRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FluidRenderer.class, remap = false)
public abstract class FluidRendererMixin {
    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/model/light/LightPipelineProvider;getLighter(Lme/jellysquid/mods/sodium/client/model/light/LightMode;)Lme/jellysquid/mods/sodium/client/model/light/LightPipeline;"
            ),
            index = 0,
            require = 1
    )
    private LightMode vro$selectFluidLighting(
            LightMode original,
            BlockAndTintGetter world,
            FluidState fluidState,
            BlockPos pos,
            BlockPos offset,
            ChunkModelBuilder buffers
    ) {
        return SmoothFluidLightingCache.usesSmoothLighting(
                fluidState.getType(), Minecraft.useAmbientOcclusion()
        ) ? LightMode.SMOOTH : LightMode.FLAT;
    }

    @Inject(method = "calculateQuadColors", at = @At("HEAD"), require = 1)
    private void vro$correctFluidQuadFlags(
            ModelQuadView quad,
            BlockAndTintGetter world,
            BlockPos pos,
            LightPipeline lighter,
            Direction direction,
            float brightness,
            ColorSampler<FluidState> colorSampler,
            FluidState fluidState,
            CallbackInfo callback
    ) {
        int flags = ModelQuadFlags.IS_PARALLEL;
        if (direction.getAxis().isHorizontal()) {
            flags |= ModelQuadFlags.IS_ALIGNED;
        } else if (direction == Direction.DOWN && quad.getY(0) == 0.0F) {
            flags |= ModelQuadFlags.IS_ALIGNED;
        }
        ((ModelQuadViewMutable) quad).setFlags(flags);
    }
}
