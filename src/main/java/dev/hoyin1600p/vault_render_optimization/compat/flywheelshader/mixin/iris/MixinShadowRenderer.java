package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.mixin.iris;

import net.coderbot.iris.mixin.LevelRendererAccessor;
import net.coderbot.iris.pipeline.ShadowRenderer;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.FlywheelShaderCompatState;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.flywheel.RenderLayerEventStateManager;

@Mixin(value = ShadowRenderer.class,remap = false)
public class MixinShadowRenderer {

    @Final
    @Shadow
    private boolean shouldRenderBlockEntities;

    @Inject(method = "renderShadows",at = @At("HEAD"))
    private void injectRenderShadow(LevelRendererAccessor levelRendererAccessor, Camera camera, CallbackInfo ci){
        if (shouldRenderBlockEntities && FlywheelShaderCompatState.isRenderPathActive()){
            RenderLayerEventStateManager.setRenderingShadow(true);
            RenderLayerEventStateManager.setSkip(false);
        }
    }


    @Inject(method = "renderShadows",at = @At("TAIL"))
    private void injectRenderShadowTail(LevelRendererAccessor levelRendererAccessor, Camera camera, CallbackInfo ci){
        if (!RenderLayerEventStateManager.isRenderingShadow()) {
            return;
        }
        RenderLayerEventStateManager.setRenderingShadow(false);
        RenderLayerEventStateManager.setSkip(FlywheelShaderCompatState.isRenderPathActive());
    }
}
