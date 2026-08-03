package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.memory.ModelFaceCompactor;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(SimpleBakedModel.class)
public abstract class SimpleBakedModelMemoryMixin {
    @Shadow
    @Final
    @Mutable
    protected Map<Direction, List<BakedQuad>> culledFaces;

    @Shadow
    @Final
    @Mutable
    protected List<BakedQuad> unculledFaces;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void vault_render_optimization$compactFaceLists(CallbackInfo callbackInfo) {
        this.unculledFaces = ModelFaceCompactor.compactUnculled(this.unculledFaces);
        this.culledFaces = ModelFaceCompactor.compactCulled(this.culledFaces);
    }
}
