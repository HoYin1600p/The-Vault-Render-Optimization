package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader;

/** A pipeline gap is not equivalent to ordinary non-shader geometry. */
public final class ModelBuildReadiness {
    private ModelBuildReadiness() { }
    public static boolean defer(boolean extendedGeometry, boolean pipelineReady) {
        return extendedGeometry && !pipelineReady;
    }
}
