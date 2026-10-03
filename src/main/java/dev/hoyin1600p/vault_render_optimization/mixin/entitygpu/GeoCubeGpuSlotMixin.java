package dev.hoyin1600p.vault_render_optimization.mixin.entitygpu;

import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuMeshSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

/** GeckoLib 3 and Ars Nouveau's shaded copy: one GPU mesh cache field per cube. */
@Pseudo
@Mixin(targets = {"software.bernie.geckolib3.geo.render.built.GeoCube",
        "software.bernie.ars_nouveau.geckolib3.geo.render.built.GeoCube"}, remap = false)
public abstract class GeoCubeGpuSlotMixin implements GpuMeshSlot {
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
}
