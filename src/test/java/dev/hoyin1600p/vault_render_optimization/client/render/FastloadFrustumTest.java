package dev.hoyin1600p.vault_render_optimization.client.render;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class FastloadFrustumTest {
    private static final String OWNER = "net/minecraft/client/renderer/culling/Frustum";
    private static final String HANDLER_DESC = "(DDDDDDLorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V";

    private static MethodNode handler(String name, String mixin) {
        MethodNode handler = new MethodNode(Opcodes.ACC_PRIVATE, name, HANDLER_DESC, null, null);
        AnnotationNode merged = new AnnotationNode("Lorg/spongepowered/asm/mixin/transformer/meta/MixinMerged;");
        merged.values = new ArrayList<>(List.of("mixin", mixin));
        handler.visibleAnnotations = new ArrayList<>(List.of(merged));
        return handler;
    }

    private static ClassNode frustum(String... mixins) {
        ClassNode node = new ClassNode();
        node.name = OWNER;
        MethodNode overload = new MethodNode(Opcodes.ACC_PRIVATE, "m_113006_", "(DDDDDD)Z", null, null);
        for (int i = 0; i < mixins.length; i++) {
            String name = "handler$" + i;
            node.methods.add(handler(name, mixins[i]));
            overload.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, OWNER, name, HANDLER_DESC));
        }
        node.methods.add(overload);
        return node;
    }

    @Test
    void onlyFastloadsOwnHookIsAccepted() {
        assertNull(FastloadFrustum.auditDoubleOverload(frustum(FastloadFrustum.FASTLOAD_MIXIN)));
        assertEquals("Fastload's frustum hook is not present", FastloadFrustum.auditDoubleOverload(frustum()));
        assertTrue(FastloadFrustum.auditDoubleOverload(frustum(FastloadFrustum.FASTLOAD_MIXIN, "other.mod.FrustumMixin"))
                .contains("other.mod.FrustumMixin"));
        ClassNode missing = new ClassNode();
        missing.name = OWNER;
        assertEquals("Frustum.cubeInFrustum(double...) is missing", FastloadFrustum.auditDoubleOverload(missing));
    }
}
