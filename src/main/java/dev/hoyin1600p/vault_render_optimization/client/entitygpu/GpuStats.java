package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import static dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuEntityModels.*;
import static dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuVerifier.*;
import static dev.hoyin1600p.vault_render_optimization.client.entitygpu.ParkedSegments.*;

/** Status, statistics and counter reset text of the GPU entity path. */
final class GpuStats {
    private GpuStats() {
    }

    // ---- status ---------------------------------------------------------------------------------
    public static String status() {
        StringBuilder text = new StringBuilder();
        text.append("GPU entity models: ");
        if (!ClientOptimizationConfig.gpuEntityModels) text.append("OFF (config render_fast_paths.gpu_entity_models)");
        else if (ClientOptimizationConfig.compareModeEnabled()) text.append("OFF (Compare Mode)");
        else text.append(state).append(" - ").append(reason);
        if (state == State.ACTIVE) {
            if (effectiveness.paused()) text.append("; paused: ").append(effectiveness.reason());
            else if (OculusShaderPackProbe.shaderPackActive() && !shaderMode(true)) {
                text.append("; models paused (Oculus shader pack active").append(irisFormat != null ? "; /vro feature gpushaders on to keep them on)" : ")")
                        .append(particlesOnlyFrame ? "; particles on the GPU" : "");
            }
            else text.append(frameActive ? "; drawing this frame" : "; idle");
        }
        String audit = GpuEntityAudit.blocker();
        text.append("\nMixin audit: ").append(audit == null ? "passed" : audit);
        String sprite = GpuEntityAudit.spriteBlocker();
        text.append("\nBlock entities (sprite wrapper): ").append(sprite == null ? "on" : "CPU only - " + sprite);
        String gecko = GpuEntityAudit.geckoBlocker();
        text.append("\nGeckoLib models: ").append(!GpuEntityAudit.geckoAudited() ? "GeckoLib 3 not installed"
                : gecko == null ? "on" : "CPU only - " + gecko);
        if (GpuEntityAudit.geckoAudited()) {
            String block = GpuEntityAudit.geckoBlockBlocker();
            text.append("; block renderers ").append(block == null ? "on" : "CPU only - " + block);
        }
        String ars = GpuEntityAudit.arsGeckoBlocker();
        text.append("\nArs Nouveau GeckoLib models: ").append(!GpuEntityAudit.arsGeckoAudited() ? "not installed"
                : ars == null ? "on" : "CPU only - " + ars);
        text.append("\nOculus shader packs (extended vertices): ").append(irisReason)
                .append(ClientOptimizationConfig.gpuEntityModelsWithShaders ? "" : "; off (config render_fast_paths.gpu_entity_models_with_shaders)");
        text.append("\n").append(GpuItems.status()).append("; ").append(itemReason);
        text.append("\n").append(GpuBlockModels.status());
        String citadel = GpuEntityAudit.citadelBlocker();
        text.append("\nParticles: ").append(!ClientOptimizationConfig.gpuParticles ? "OFF (config render_fast_paths.gpu_particles)"
                : particleReason);
        text.append("\nCitadel models (Alex's Mobs): ").append(!GpuEntityAudit.citadelAudited() ? "not installed"
                : citadel == null ? "on" : "CPU only - " + citadel);
        if (snapshot != null) {
            text.append("\nDriver: ").append(snapshot.renderer()).append(" / ").append(snapshot.version())
                    .append(" / GLSL ").append(snapshot.glslVersion());
        }
        return text.toString();
    }

    public static String stats() {
        return "parts on GPU " + PARTS_GPU.get() + ", vertices on GPU " + VERTICES_GPU.get() + ", dispatches "
                + DISPATCHES.get() + ", batches filled on CPU " + BATCHES_CPU_FILLED.get() + ", parts not eligible "
                + PARTS_NOT_ELIGIBLE.get() + ", late fills " + LATE_FILLS.get() + " (writes skipped out of range "
                + HoleBatch.FILL_WRITES_SKIPPED.get() + "; last reason: " + lastLateFill + "), fills outside the upload "
                + UNARMED_FILLS.get() + ", small batches filled on CPU " + SMALL_FILLS.get() + ", sorted item batches "
                + SORTED_ITEM_BATCHES.get() + " (sort position mismatches " + SORT_POSITION_MISMATCHES.get() + "), model sort fills "
                + SORT_FILLS_HINTED.get() + " hinted / " + SORT_FILLS_UNHINTED.get() + " never hinted" + ", ineffective pauses " + PAUSES.get() + ", Oculus segments parked "
                + PARKED_BATCHES.get() + " (dropped " + PARKED_DROPPED.get() + "), upload bytes skipped "
                + (BYTES_NOT_UPLOADED.get() >> 20) + " MiB, GeckoLib cubes on GPU " + GECKO_CUBES_GPU.get()
                + " (not eligible " + GECKO_NOT_ELIGIBLE.get() + ", meshes recaptured " + GECKO_MESH_CHANGED.get() + ")"
                + ", Citadel parts on GPU " + CITADEL_PARTS_GPU.get() + " (not eligible " + CITADEL_NOT_ELIGIBLE.get() + ")"
                + ", particles on GPU " + PARTICLES_GPU.get() + ", meshes shared " + MESHES_SHARED.get() + ", Oculus extended draws " + IRIS_DISPATCHES.get()
                + ", " + GpuItems.stats() + ", " + GpuBlockModels.stats()
                + (backend == null ? "" : ", arena " + backend.arenaVertices() + " vertices / "
                + (backend.arenaCapacity() >> 10) + " KiB");
    }

    public static void resetStats() {
        PARTS_GPU.set(0);
        VERTICES_GPU.set(0);
        DISPATCHES.set(0);
        BATCHES_CPU_FILLED.set(0);
        PARTS_NOT_ELIGIBLE.set(0);
        LATE_FILLS.set(0);
        UNARMED_FILLS.set(0);
        SMALL_FILLS.set(0);
        SORTED_ITEM_BATCHES.set(0);
        SORT_FILLS_HINTED.set(0);
        SORT_FILLS_UNHINTED.set(0);
        SORT_POSITION_MISMATCHES.set(0);
        PAUSES.set(0);
        BYTES_NOT_UPLOADED.set(0);
        PARKED_BATCHES.set(0);
        PARKED_DROPPED.set(0);
        VERIFIED_VERTICES.set(0);
        VERIFY_MISMATCHES.set(0);
        GECKO_CUBES_GPU.set(0);
        GECKO_NOT_ELIGIBLE.set(0);
        GECKO_MESH_CHANGED.set(0);
        CITADEL_PARTS_GPU.set(0);
        CITADEL_NOT_ELIGIBLE.set(0);
        PARTICLES_GPU.set(0);
        IRIS_DISPATCHES.set(0);
        GpuItems.resetStats();
        GpuBlockModels.resetStats();
        firstMismatch = null;
    }
}
