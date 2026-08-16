package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.mixin.flw;


import com.jozufozu.flywheel.backend.instancing.InstancedRenderDispatcher;
import com.jozufozu.flywheel.event.RenderLayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.flywheel.RenderLayerEventStateManager;

@Mixin(value = InstancedRenderDispatcher.class,remap = false)
public class MixinInstancedRenderDispatcher {
    @Inject(method = "renderLayer",at=@At("HEAD"),cancellable = true)
    private static void vroFlywheel$renderLayer(RenderLayerEvent event, CallbackInfo ci){
        if(RenderLayerEventStateManager.isSkip())
            ci.cancel();
    }
}
