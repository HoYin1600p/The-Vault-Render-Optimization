package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.flywheel;

public class RenderLayerEventStateManager {
    private static boolean skip = false;

    private static boolean renderingShadow = false;

    public static boolean isSkip() {
        return skip;
    }

    public static void setSkip(boolean skip) {
        RenderLayerEventStateManager.skip = skip;
    }

    public static boolean isRenderingShadow() {
        return renderingShadow;
    }

    public static void setRenderingShadow(boolean renderingShadow) {
        RenderLayerEventStateManager.renderingShadow = renderingShadow;
    }
}
