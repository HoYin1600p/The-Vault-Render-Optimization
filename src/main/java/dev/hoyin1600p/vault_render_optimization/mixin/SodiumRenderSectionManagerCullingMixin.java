package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.render.SectionDistanceCulling;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class SodiumRenderSectionManagerCullingMixin {
    @Shadow
    private float cameraX;

    @Shadow
    private float cameraY;

    @Shadow
    private float cameraZ;

    @Inject(
            method = {
                    "addChunkToVisible(Lme/jellysquid/mods/sodium/client/render/chunk/RenderSection;)V",
                    "addEntitiesToRenderLists(Lme/jellysquid/mods/sodium/client/render/chunk/RenderSection;)V"
            },
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void vault_render_optimization$hideDistantSection(
            RenderSection renderSection, CallbackInfo callbackInfo) {
        SodiumRenderSectionAccessor section = (SodiumRenderSectionAccessor) renderSection;
        if (SectionDistanceCulling.shouldCull(
                this.cameraX,
                this.cameraY,
                this.cameraZ,
                section.vault_render_optimization$getChunkX() << 4,
                section.vault_render_optimization$getChunkY() << 4,
                section.vault_render_optimization$getChunkZ() << 4)) {
            callbackInfo.cancel();
        }
    }
}
