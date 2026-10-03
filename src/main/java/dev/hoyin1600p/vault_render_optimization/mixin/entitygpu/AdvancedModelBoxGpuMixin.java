package dev.hoyin1600p.vault_render_optimization.mixin.entitygpu;

import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuMeshSlot;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.citadel.CitadelGpuModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Citadel's {@code AdvancedModelBox.render} (Alex's Mobs): its call to the private {@code doRender} is replaced,
 * for an eligible part, by a GPU reservation of the same vertices; otherwise the original {@code doRender} runs.
 * The rest of {@code render} (visibility, push, translate/rotate/scale, children, pop) is untouched. The mixin
 * plugin audits {@code doRender} itself: another mod's change there keeps Citadel parts on the CPU. Without
 * Citadel this mixin does not apply.
 */
@Pseudo
@Mixin(targets = "com.github.alexthe666.citadel.client.model.AdvancedModelBox", remap = false)
public abstract class AdvancedModelBoxGpuMixin implements GpuMeshSlot {
    @Unique
    private Object vro$gpuMesh;

    @Override
    public Object vro$gpuMesh() {
        return vro$gpuMesh;
    }

    @Override
    public void vro$setGpuMesh(Object entry) {
        vro$gpuMesh = entry;
    }

    @Shadow(remap = false)
    private void doRender(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay, float red, float green,
                          float blue, float alpha) {
        throw new AssertionError("shadowed");
    }

    @Redirect(method = "render", require = 0, remap = false,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lcom/github/alexthe666/citadel/client/model/AdvancedModelBox;doRender(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void vro$doRenderOnGpu(AdvancedModelBox box, PoseStack.Pose pose, VertexConsumer consumer, int light,
                                   int overlay, float red, float green, float blue, float alpha) {
        if (!CitadelGpuModels.tryReserve(box, pose, consumer, light, overlay, red, green, blue, alpha)) {
            this.doRender(pose, consumer, light, overlay, red, green, blue, alpha);
        }
    }
}
