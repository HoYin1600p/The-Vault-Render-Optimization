package dev.hoyin1600p.vault_render_optimization.client.render;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;

public final class SectionDistanceCulling {
    private static final int SECTION_SIZE = 16;
    private static final ThreadLocal<RenderContext> CONTEXT =
            ThreadLocal.withInitial(RenderContext::new);

    private SectionDistanceCulling() {
    }

    public static void begin(double cameraX, double cameraY, double cameraZ) {
        RenderContext context = CONTEXT.get();
        context.cameraX = cameraX;
        context.cameraY = cameraY;
        context.cameraZ = cameraZ;
        context.active = true;
    }

    public static void end() {
        CONTEXT.get().active = false;
    }

    public static boolean shouldCullActiveContext(int originX, int originY, int originZ) {
        RenderContext context = CONTEXT.get();
        return context.active && shouldCull(context.cameraX, context.cameraY, context.cameraZ,
                originX, originY, originZ);
    }

    public static boolean shouldCull(
            double cameraX,
            double cameraY,
            double cameraZ,
            int originX,
            int originY,
            int originZ) {
        if (!ClientOptimizationConfig.optimizationsEnabled()) {
            return false;
        }

        if (ClientOptimizationConfig.verticalSectionCulling) {
            double verticalDistance = axisDistance(cameraY, originY, originY + SECTION_SIZE);
            double verticalLimit = ClientOptimizationConfig.verticalSectionDistance * SECTION_SIZE;
            if (verticalDistance > verticalLimit) {
                return true;
            }
        }

        if (ClientOptimizationConfig.horizontalSectionCulling) {
            double dx = axisDistance(cameraX, originX, originX + SECTION_SIZE);
            double dz = axisDistance(cameraZ, originZ, originZ + SECTION_SIZE);
            double horizontalLimit = ClientOptimizationConfig.horizontalSectionDistance * SECTION_SIZE;
            return dx * dx + dz * dz > horizontalLimit * horizontalLimit;
        }

        return false;
    }

    private static double axisDistance(double point, double minimum, double maximum) {
        if (point < minimum) {
            return minimum - point;
        }
        if (point > maximum) {
            return point - maximum;
        }
        return 0.0D;
    }

    private static final class RenderContext {
        private boolean active;
        private double cameraX;
        private double cameraY;
        private double cameraZ;
    }
}
