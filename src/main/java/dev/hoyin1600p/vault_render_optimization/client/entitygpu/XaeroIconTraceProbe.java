package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * Reads Xaero's Minimap {@code EntityRenderTracer.TRACING_MODEL_RENDERS}. While it is true, Xaero's
 * {@code ModelPart.compile} hook records the parts it sees to build a radar icon, so the GPU path
 * must leave every part to vanilla.
 */
final class XaeroIconTraceProbe {
    private static final String TRACER = "xaero.hud.minimap.radar.icon.creator.render.trace.EntityRenderTracer";
    private static MethodHandle flag;
    private static String problem = "not resolved";

    private XaeroIconTraceProbe() {
    }

    /** @return null when the flag can be read, otherwise why not */
    static synchronized String resolve() {
        if (flag != null) return null;
        try {
            Class<?> tracer = Class.forName(TRACER, false, XaeroIconTraceProbe.class.getClassLoader());
            flag = MethodHandles.publicLookup().findStaticGetter(tracer, "TRACING_MODEL_RENDERS", boolean.class);
            problem = null;
        } catch (Throwable failure) {
            problem = "Xaero's icon-trace flag could not be read (" + failure + ")";
        }
        return problem;
    }

    /** True while Xaero traces model renders; also true (fail closed) if the flag cannot be read. */
    static boolean tracing() {
        MethodHandle handle = flag;
        if (handle == null) return true;
        try {
            return (boolean) handle.invokeExact();
        } catch (Throwable failure) {
            return true;
        }
    }
}
