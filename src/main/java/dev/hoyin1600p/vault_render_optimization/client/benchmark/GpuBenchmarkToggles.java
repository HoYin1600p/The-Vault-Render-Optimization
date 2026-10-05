package dev.hoyin1600p.vault_render_optimization.client.benchmark;

import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuEntityModels;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.OculusShaderPackProbe;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import dev.hoyin1600p.vault_render_optimization.util.ConfigKeys;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** The GPU switches the benchmark measures, with the switches each one needs on or off while it is measured. */
public final class GpuBenchmarkToggles {
    private GpuBenchmarkToggles() { }

    public static List<BenchmarkToggle> create() {
        return create(OculusShaderPackProbe.shaderPackActive());
    }

    static List<BenchmarkToggle> create(boolean shaders) {
        List<BenchmarkToggle> toggles = new ArrayList<>();
        BenchmarkToggle base = toggle(ConfigKeys.GPU_ENTITY_MODELS, "GPU entity models",
                () -> ClientOptimizationConfig.gpuEntityModels,
                enabled -> ClientOptimizationConfig.gpuEntityModels = enabled);
        BenchmarkToggle shaderEntities = toggle(ConfigKeys.GPU_ENTITY_MODELS_WITH_SHADERS, "GPU models with shaders",
                () -> ClientOptimizationConfig.gpuEntityModelsWithShaders,
                enabled -> ClientOptimizationConfig.gpuEntityModelsWithShaders = enabled);
        BenchmarkToggle particles = toggle(ConfigKeys.GPU_PARTICLES, "GPU particles",
                () -> ClientOptimizationConfig.gpuParticles,
                enabled -> ClientOptimizationConfig.gpuParticles = enabled);
        BenchmarkToggle shaderParticles = toggle(ConfigKeys.GPU_PARTICLES_WITH_SHADERS, "GPU particles with shaders",
                () -> ClientOptimizationConfig.gpuParticlesWithShaders,
                enabled -> ClientOptimizationConfig.gpuParticlesWithShaders = enabled);
        toggles.add(base);
        if (shaders) toggles.add(dependent(shaderEntities, List.of(base), List.of()));
        BenchmarkToggle items = toggle(ConfigKeys.GPU_ITEMS, "GPU items", () -> ClientOptimizationConfig.gpuItems,
                enabled -> ClientOptimizationConfig.gpuItems = enabled);
        toggles.add(dependent(items, shaders ? List.of(base, shaderEntities) : List.of(base), List.of()));
        // With shaders, isolate particles from the entity variant which otherwise includes them.
        toggles.add(dependent(particles, shaders ? List.of(base, shaderParticles) : List.of(base),
                shaders ? List.of(shaderEntities) : List.of()));
        if (shaders) toggles.add(dependent(shaderParticles, List.of(base, particles), List.of(shaderEntities)));
        return List.copyOf(toggles);
    }

    private static BenchmarkToggle dependent(BenchmarkToggle toggle, List<BenchmarkToggle> on,
                                              List<BenchmarkToggle> off) {
        return new BenchmarkToggle() {
            private List<Boolean> originals;
            public String id() { return toggle.id(); }
            public String displayName() { return toggle.displayName(); }
            public boolean current() { return toggle.current(); }
            public void applySession(boolean enabled) { toggle.applySession(enabled); }
            public boolean available() { return toggle.available(); }
            public String unavailableReason() { return toggle.unavailableReason(); }
            public void beginMeasure() {
                List<BenchmarkToggle> dependencies = new ArrayList<>(on);
                dependencies.addAll(off);
                originals = dependencies.stream().map(BenchmarkToggle::current).toList();
                on.forEach(parent -> parent.applySession(true));
                off.forEach(parent -> parent.applySession(false));
            }
            public void endMeasure() {
                if (originals == null) return;
                List<BenchmarkToggle> dependencies = new ArrayList<>(on);
                dependencies.addAll(off);
                for (int i = 0; i < dependencies.size(); i++) dependencies.get(i).applySession(originals.get(i));
                originals = null;
            }
        };
    }

    private static BenchmarkToggle toggle(String id, String name, BooleanSupplier get, Consumer<Boolean> apply) {
        return new BenchmarkToggle() {
            public String id() { return id; }
            public String displayName() { return name; }
            public boolean current() { return get.getAsBoolean(); }
            public void applySession(boolean enabled) { apply.accept(enabled); }
            public boolean available() { return unavailableReason().isEmpty(); }
            public String unavailableReason() {
                if (!ClientOptimizationConfig.optimizationsEnabled()) return "Compare Mode is enabled";
                GpuEntityModels.State state = GpuEntityModels.state();
                if (state != GpuEntityModels.State.ACTIVE && state != GpuEntityModels.State.UNINITIALIZED) {
                    return "GPU entity path is " + state;
                }
                return "";
            }
        };
    }
}
