package dev.hoyin1600p.vault_render_optimization.client.entitygpu.geckolib;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuEntityAudit;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuEntityModels;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuHoleBuilder;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuMeshSlot;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.ModelMesh;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import software.bernie.geckolib3.geo.render.built.GeoCube;
import software.bernie.geckolib3.geo.render.built.GeoQuad;
import software.bernie.geckolib3.renderers.geo.IGeoRenderer;
import software.bernie.geckolib3.util.RenderUtils;

/**
 * GeckoLib 3 entity cubes on the GPU. {@code GeoEntityRenderer.renderRecursively} wraps each
 * {@code renderCube} in its own push/pop; for an eligible cube VRO runs {@code renderCube}'s pose steps
 * ({@code translateToPivotPoint}, {@code rotateMatrixAroundCube}, {@code translateAwayFromPivotPoint}) and
 * reserves the cube's cached vertices instead of computing them, so the compute shader writes exactly the
 * vertices {@code createVerticesOfQuad} would. Anything unusual stays on GeckoLib's own path: another
 * buffer than a plain {@code BufferBuilder}, a renderer class that overrides the cube or quad methods, a
 * failed {@code IGeoRenderer} audit, a quad that is not four vertices, or a full mesh arena.
 */
public final class GeckoLibGpuModels {
    private record Entry(int generation, GeoQuad[] quads, ModelMesh mesh, int first, int flips) {
        boolean gpu() {
            return first >= 0;
        }
    }

    /** GeoCube does not override equals/hashCode, so this is keyed by identity and drops unloaded models. */
    private static final Map<GeoCube, Entry> CUBES = new WeakHashMap<>();

    /**
     * Renderer classes that declare their own cube or quad code, below GeckoLib's own classes (GeckoLib's
     * GeoBlockRenderer carries VRO's merged renderCube, and GeoEntityRenderer is hooked at its call site).
     */
    private static final ClassValue<Boolean> OVERRIDES = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            for (Class<?> c = type; c != null && !c.getName().startsWith("software.bernie.geckolib3."); c = c.getSuperclass()) {
                for (Method method : c.getDeclaredMethods()) {
                    String name = method.getName();
                    if (name.equals("renderCube") || name.equals("createVerticesOfQuad")) {
                        VaultRenderOptimization.LOGGER.info("GPU entity models: {} declares {}; its GeckoLib cubes stay on the CPU",
                                type.getName(), name);
                        return true;
                    }
                }
            }
            return false;
        }
    };

    private GeckoLibGpuModels() {
    }

    /**
     * @return true when the cube's vertices were reserved for the GPU and {@code renderCube} must not run;
     *         false when the caller must run GeckoLib's own {@code renderCube} (the pose is then untouched)
     */
    public static boolean tryReserve(IGeoRenderer<?> renderer, GeoCube cube, PoseStack poseStack,
                                     VertexConsumer consumer, int light, int overlay, float red, float green,
                                     float blue, float alpha) {
        if (!GpuEntityModels.frameActive() || !RenderSystem.isOnRenderThread()) return false;
        if (consumer.getClass() != BufferBuilder.class) return false;
        if (GpuEntityAudit.geckoBlocker() != null || OVERRIDES.get(renderer.getClass())
                || !(renderer instanceof software.bernie.geckolib3.renderers.geo.GeoEntityRenderer<?>)
                        && GpuEntityAudit.geckoBlockBlocker() != null) {
            GpuEntityModels.GECKO_NOT_ELIGIBLE.incrementAndGet();
            return false;
        }
        GpuHoleBuilder builder = (GpuHoleBuilder) consumer;
        if (!builder.vro$canReserve()) return false;
        Entry entry = entry(cube);
        if (!entry.gpu()) {
            GpuEntityModels.GECKO_NOT_ELIGIBLE.incrementAndGet();
            return false;
        }
        RenderUtils.translateToPivotPoint(poseStack, cube);
        RenderUtils.rotateMatrixAroundCube(poseStack, cube);
        RenderUtils.translateAwayFromPivotPoint(poseStack, cube);
        if (entry.mesh().vertexCount() > 0) {
            GpuEntityModels.reserveMesh(builder, entry.mesh(), entry.first(), poseStack.last(), light, overlay,
                    red, green, blue, alpha, entry.flips());
        }
        GpuEntityModels.GECKO_CUBES_GPU.incrementAndGet();
        return true;
    }

    private static Entry entry(GeoCube cube) {
        GpuMeshSlot slot = (Object) cube instanceof GpuMeshSlot s ? s : null;
        Entry entry = slot != null ? (Entry) slot.vro$gpuMesh() : CUBES.get(cube);
        int generation = GpuEntityModels.generation();
        if (entry != null && entry.generation() == generation && entry.quads() == cube.quads) {
            if (!GpuEntityModels.verifying() || unchanged(entry, cube)) return entry;
            GpuEntityModels.GECKO_MESH_CHANGED.incrementAndGet();
        }
        ModelMesh mesh = GeckoMeshCapture.capture(cube);
        int first = mesh == null ? -1 : GpuEntityModels.uploadMesh(mesh);
        entry = new Entry(generation, cube.quads, mesh == null ? ModelMesh.EMPTY : mesh, first,
                GeckoMeshCapture.flips(cube));
        if (slot != null) slot.vro$setGpuMesh(entry);
        else CUBES.put(cube, entry);
        return entry;
    }

    /** Verify mode only: GeckoLib's cube fields are mutable, so check the cached copy still matches. */
    private static boolean unchanged(Entry entry, GeoCube cube) {
        ModelMesh live = GeckoMeshCapture.capture(cube);
        return live != null && entry.flips() == GeckoMeshCapture.flips(cube)
                && Arrays.equals(live.data(), entry.mesh().data());
    }
}
