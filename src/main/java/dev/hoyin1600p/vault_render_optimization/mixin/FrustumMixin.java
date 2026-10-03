package dev.hoyin1600p.vault_render_optimization.mixin;

import com.mojang.math.Vector4f;
import dev.hoyin1600p.vault_render_optimization.client.render.FrustumPlanes;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Frustum.class)
public abstract class FrustumMixin {
    @Shadow
    @Final
    private Vector4f[] frustumData;

    /** Same answer as vanilla's eight-corner test per plane, without allocating 48 vectors. */
    @Inject(method = "cubeInFrustum(FFFFFF)Z", at = @At("HEAD"), cancellable = true)
    private void vro$positiveVertexTest(float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (ClientOptimizationConfig.allocationFreeFrustum && ClientOptimizationConfig.optimizationsEnabled()
                && FrustumPlanes.finite(minX, minY, minZ, maxX, maxY, maxZ)) {
            boolean visible = FrustumPlanes.intersects(this.frustumData, minX, minY, minZ, maxX, maxY, maxZ);
            if (FrustumPlanes.verify) {
                FrustumPlanes.verifyChecks++;
                if (visible != FrustumPlanes.reference(this.frustumData, minX, minY, minZ, maxX, maxY, maxZ)) {
                    FrustumPlanes.verifyMismatches++;
                }
            }
            cir.setReturnValue(visible);
        }
    }
}
