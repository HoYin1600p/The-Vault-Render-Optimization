package dev.hoyin1600p.vault_render_optimization.mixin.chunk;

import dev.hoyin1600p.vault_render_optimization.client.render.AngleSectionOcclusion;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.util.frustum.Frustum;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class EmbeddiumAngleOcclusionManagerMixin {
    @Inject(method = "iterateChunks", at = @At("HEAD"), remap = false)
    private void vro$beginAngleOcclusion(
            Camera camera, Frustum frustum, int frame, boolean spectator, CallbackInfo ci
    ) {
        Vec3 pos = camera.getPosition();
        AngleSectionOcclusion.beginPass(pos.x, pos.y, pos.z, spectator);
    }

    @Inject(method = "iterateChunks", at = @At("RETURN"), remap = false)
    private void vro$endAngleOcclusion(
            Camera camera, Frustum frustum, int frame, boolean spectator, CallbackInfo ci
    ) {
        AngleSectionOcclusion.endPass();
    }
}
