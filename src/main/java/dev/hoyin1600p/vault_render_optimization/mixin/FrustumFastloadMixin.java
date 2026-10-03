package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.render.FastloadFrustum;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Only applied when Fastload is installed. While {@link FastloadFrustum#bypass()} is true, {@code isVisible(AABB)}
 * does exactly what vanilla's double overload does - six {@code (float) (bound - camera)} conversions - and calls
 * the float {@code cubeInFrustum} (with every hook on it), skipping Fastload's per-check event lookup.
 */
@Mixin(Frustum.class)
public abstract class FrustumFastloadMixin {
    @Shadow
    private double camX;

    @Shadow
    private double camY;

    @Shadow
    private double camZ;

    @Shadow
    private boolean cubeInFrustum(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        throw new AssertionError("shadowed");
    }

    @Inject(method = "isVisible(Lnet/minecraft/world/phys/AABB;)Z", at = @At("HEAD"), cancellable = true)
    private void vro$skipFastloadWhileIdle(AABB box, CallbackInfoReturnable<Boolean> cir) {
        if (!FastloadFrustum.bypass()) return;
        FastloadFrustum.bypassedChecks++;
        cir.setReturnValue(this.cubeInFrustum(
                (float) (box.minX - this.camX), (float) (box.minY - this.camY), (float) (box.minZ - this.camZ),
                (float) (box.maxX - this.camX), (float) (box.maxY - this.camY), (float) (box.maxZ - this.camZ)));
    }
}
