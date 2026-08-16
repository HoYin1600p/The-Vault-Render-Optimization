package dev.hoyin1600p.vault_render_optimization.mixin;

import com.jozufozu.flywheel.backend.model.ArrayModelRenderer;
import com.simibubi.create.content.contraptions.render.ContraptionProgram;
import com.simibubi.create.content.contraptions.render.FlwContraption;
import dev.hoyin1600p.vault_render_optimization.client.create.SectionedContraptionRenderer;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = FlwContraption.class, remap = false)
public abstract class CreateFlwContraptionMixin {
    @Shadow @Final private Map<RenderType, ArrayModelRenderer> renderLayers;

    @Shadow abstract void setup(ContraptionProgram shader);

    @Unique
    private SectionedContraptionRenderer.FlywheelData vro$sectionedMeshes;

    @Inject(method = "buildLayers", at = @At("HEAD"), cancellable = true, remap = false)
    private void vro$buildSectionedLayers(CallbackInfo ci) {
        FlwContraption self = (FlwContraption) (Object) this;
        if (!SectionedContraptionRenderer.shouldSection(self.contraption)) {
            vro$deleteSectionedMeshes();
            return;
        }

        for (ArrayModelRenderer renderer : renderLayers.values()) {
            renderer.delete();
            renderer.getModel().delete();
        }
        renderLayers.clear();
        vro$deleteSectionedMeshes();
        vro$sectionedMeshes = SectionedContraptionRenderer.buildFlywheel(self.contraption, self.renderWorld);
        ci.cancel();
    }

    @Inject(method = "renderStructureLayer", at = @At("HEAD"), cancellable = true, remap = false)
    private void vro$renderSectionedLayer(RenderType layer, ContraptionProgram shader, CallbackInfo ci) {
        if (vro$sectionedMeshes == null) {
            return;
        }
        FlwContraption self = (FlwContraption) (Object) this;
        setup(shader);
        vro$sectionedMeshes.render(layer, self.getMatrices(), shader);
        ci.cancel();
    }

    @Inject(method = "invalidate", at = @At("HEAD"), remap = false)
    private void vro$deleteSectionedLayers(CallbackInfo ci) {
        vro$deleteSectionedMeshes();
    }

    @Unique
    private void vro$deleteSectionedMeshes() {
        if (vro$sectionedMeshes != null) {
            vro$sectionedMeshes.delete();
            vro$sectionedMeshes = null;
        }
    }
}
