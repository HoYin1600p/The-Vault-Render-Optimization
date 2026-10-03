package dev.hoyin1600p.vault_render_optimization.client.entitygpu.citadel;

import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuEntityAudit;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuEntityModels;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuHoleBuilder;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuMeshSlot;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.ModelMesh;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Citadel {@code AdvancedModelBox} parts (Alex's Mobs and other Citadel mods) on the GPU. {@code render} keeps
 * its own push, {@code translateAndRotate}, child and scale logic; only its call to the private
 * {@code doRender(pose, ...)} is replaced for an eligible part by reserving the part's cached cubes for the
 * pose {@code doRender} would have used. Anything else runs Citadel's own {@code doRender}.
 */
public final class CitadelGpuModels {
    private record Entry(int generation, List<?> cubes, int cubeCount, ModelMesh mesh, int first) {
        boolean gpu() {
            return first >= 0;
        }
    }

    private static final Map<AdvancedModelBox, Entry> PARTS = new WeakHashMap<>();

    private CitadelGpuModels() {
    }

    /** @return true when the part's vertices were reserved and {@code doRender} must not run */
    public static boolean tryReserve(AdvancedModelBox box, PoseStack.Pose pose, VertexConsumer consumer, int light,
                                     int overlay, float red, float green, float blue, float alpha) {
        if (!GpuEntityModels.frameActive() || !RenderSystem.isOnRenderThread()) return false;
        if (consumer.getClass() != BufferBuilder.class) return false;
        if (GpuEntityAudit.citadelBlocker() != null) {
            GpuEntityModels.CITADEL_NOT_ELIGIBLE.incrementAndGet();
            return false;
        }
        GpuHoleBuilder builder = (GpuHoleBuilder) consumer;
        if (!builder.vro$canReserve()) return false;
        Entry entry = entry(box);
        if (!entry.gpu()) {
            GpuEntityModels.CITADEL_NOT_ELIGIBLE.incrementAndGet();
            return false;
        }
        if (entry.mesh().vertexCount() > 0) {
            GpuEntityModels.reserveMesh(builder, entry.mesh(), entry.first(), pose, light, overlay, red, green, blue,
                    alpha, 0);
        }
        GpuEntityModels.CITADEL_PARTS_GPU.incrementAndGet();
        return true;
    }

    private static Entry entry(AdvancedModelBox box) {
        GpuMeshSlot slot = (Object) box instanceof GpuMeshSlot s ? s : null;
        Entry entry = slot != null ? (Entry) slot.vro$gpuMesh() : PARTS.get(box);
        int generation = GpuEntityModels.generation();
        if (entry != null && entry.generation() == generation && entry.cubes() == box.cubeList
                && entry.cubeCount() == box.cubeList.size()) {
            if (!GpuEntityModels.verifying() || unchanged(entry, box)) return entry;
            GpuEntityModels.GECKO_MESH_CHANGED.incrementAndGet();
        }
        ModelMesh mesh = CitadelMeshCapture.capture(box);
        int first = mesh == null ? -1 : GpuEntityModels.uploadMesh(mesh);
        entry = new Entry(generation, box.cubeList, box.cubeList == null ? 0 : box.cubeList.size(),
                mesh == null ? ModelMesh.EMPTY : mesh, first);
        if (slot != null) slot.vro$setGpuMesh(entry);
        else PARTS.put(box, entry);
        return entry;
    }

    /** Verify mode only: Citadel's cube fields are mutable, so check the cached copy still matches. */
    private static boolean unchanged(Entry entry, AdvancedModelBox box) {
        ModelMesh live = CitadelMeshCapture.capture(box);
        return live != null && Arrays.equals(live.data(), entry.mesh().data());
    }
}
