package dev.hoyin1600p.vault_render_optimization.client.render;

import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Fastload (Reforged) hooks {@code Frustum.cubeInFrustum(double...)} to run its {@code frustum_box_bool} event,
 * and that hook calls a {@code synchronized} event lookup on every visibility check: every particle, entity and
 * block entity, every frame (about a third of the render thread under heavy particles). The event only has
 * listeners while Fastload preloads the world; otherwise the hook changes nothing.
 *
 * <p>So while the event is empty (checked once per frame) and the audit shows Fastload's hook is the only foreign
 * change to that method, {@code isVisible(AABB)} computes the same six floats vanilla's double overload computes
 * and calls the real float {@code cubeInFrustum}, with every hook on it. The answer is identical.
 */
public final class FastloadFrustum {
    static final String FASTLOAD_MIXIN = "io.github.bumblesoftware.fastload.mixin.client.FrustumMixin";
    private static final List<String> DOUBLE_OVERLOAD = List.of("cubeInFrustum", "m_113006_");
    private static final String DOUBLE_DESC = "(DDDDDD)Z";

    private static volatile String auditResult = "Frustum was not audited";
    private static Method isNotEmpty;
    private static Object boxEvent;
    private static boolean reflectionFailed;
    private static volatile boolean bypass;
    public static long bypassedChecks;

    private FastloadFrustum() {
    }

    public static boolean bypass() {
        return bypass;
    }

    /** Called at the start of every level frame on the render thread. */
    public static void beginFrame() {
        bypass = auditResult == null && ClientOptimizationConfig.fastloadFrustumBypass
                && ClientOptimizationConfig.optimizationsEnabled() && !eventActive();
    }

    private static boolean eventActive() {
        if (reflectionFailed) return true;
        try {
            if (isNotEmpty == null) {
                ClassLoader loader = FastloadFrustum.class.getClassLoader();
                Class<?> events = Class.forName("io.github.bumblesoftware.fastload.client.FLClientEvents$Events", true, loader);
                boxEvent = events.getField("BOX_BOOLEAN_EVENT").get(null);
                isNotEmpty = boxEvent.getClass().getMethod("isNotEmpty", String[].class);
                isNotEmpty.setAccessible(true);
            }
            return (Boolean) isNotEmpty.invoke(boxEvent, (Object) new String[] {"frustum_box_bool"});
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            reflectionFailed = true;
            VaultRenderOptimization.LOGGER.warn("Fastload frustum bypass off: Fastload's event API is not the expected one ({})",
                    failure.toString());
            return true;
        }
    }

    /** From the mixin plugin's postApply on {@code Frustum}: only Fastload's own hook may change the double overload. */
    public static void audit(ClassNode node) {
        auditResult = auditDoubleOverload(node);
        if (auditResult != null) {
            VaultRenderOptimization.LOGGER.info("Fastload frustum bypass off: {}", auditResult);
        }
    }

    static String auditDoubleOverload(ClassNode node) {
        MethodNode target = null;
        for (MethodNode method : node.methods) {
            if (DOUBLE_OVERLOAD.contains(method.name) && method.desc.equals(DOUBLE_DESC)) target = method;
        }
        if (target == null) return "Frustum.cubeInFrustum(double...) is missing";
        if (mergedFrom(target) != null) return "Frustum.cubeInFrustum(double...) is overwritten by " + mergedFrom(target);
        boolean fastloadSeen = false;
        for (AbstractInsnNode insn : target.instructions) {
            if (!(insn instanceof MethodInsnNode call) || !call.owner.equals(node.name)) continue;
            MethodNode callee = null;
            for (MethodNode method : node.methods) {
                if (method.name.equals(call.name) && method.desc.equals(call.desc)) callee = method;
            }
            String owner = callee == null ? null : mergedFrom(callee);
            if (owner == null) continue;
            if (owner.equals(FASTLOAD_MIXIN)) fastloadSeen = true;
            else return "Frustum.cubeInFrustum(double...) is also changed by " + owner;
        }
        return fastloadSeen ? null : "Fastload's frustum hook is not present";
    }

    private static String mergedFrom(MethodNode method) {
        for (List<AnnotationNode> list : Arrays.asList(method.visibleAnnotations, method.invisibleAnnotations)) {
            if (list == null) continue;
            for (AnnotationNode annotation : list) {
                if (!"Lorg/spongepowered/asm/mixin/transformer/meta/MixinMerged;".equals(annotation.desc) || annotation.values == null) continue;
                for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                    if ("mixin".equals(annotation.values.get(i))) return String.valueOf(annotation.values.get(i + 1));
                }
            }
        }
        return null;
    }

    public static String status() {
        return "Fastload frustum bypass: " + (auditResult != null ? "OFF (" + auditResult + ")"
                : bypass ? "ON" : "idle (Fastload event active, disabled, or Compare Mode)")
                + "; checks bypassed " + bypassedChecks;
    }
}
