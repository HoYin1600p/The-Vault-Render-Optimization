package dev.hoyin1600p.vault_render_optimization;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Mixin rejects, at class-load time, an injector handler whose {@code static} modifier does not
 * match its target (Inject, INVOKE/field Redirect, ModifyArg), or an instance handler on a static
 * target (NEW Redirect, ModifyArgs, ModifyConstant, ModifyVariable). Such a mismatch only shows up
 * when the game loads the target class - for a particle, possibly mid-session - so check every VRO
 * mixin against its target's bytecode here. Targets not on the test classpath are checked only for
 * constructors, which are always instance methods.
 */
class MixinHandlerModifierTest {
    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String INJECTION = "Lorg/spongepowered/asm/mixin/injection/";

    @Test
    void handlerStaticModifiersMatchTheirTargets() throws Exception {
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (ClassNode mixin : vroMixins()) {
            List<String> targets = targets(mixin);
            for (MethodNode handler : mixin.methods) {
                if (handler.visibleAnnotations == null) continue;
                for (AnnotationNode annotation : handler.visibleAnnotations) {
                    if (!annotation.desc.startsWith(INJECTION) || annotation.desc.contains("/callback/")) continue;
                    String kind = annotation.desc.substring(INJECTION.length(), annotation.desc.length() - 1);
                    if (!List.of("Inject", "Redirect", "ModifyArg", "ModifyArgs", "ModifyConstant", "ModifyVariable")
                            .contains(kind)) continue;
                    boolean exact = kind.equals("Inject") || kind.equals("ModifyArg")
                            || (kind.equals("Redirect") && !"NEW".equals(atValue(annotation)));
                    boolean handlerStatic = (handler.access & Opcodes.ACC_STATIC) != 0;
                    for (String selector : stringList(annotation, "method")) {
                        String name = selector.contains("(") ? selector.substring(0, selector.indexOf('(')) : selector;
                        String desc = selector.contains("(") ? selector.substring(selector.indexOf('(')) : null;
                        for (String target : targets) {
                            ClassNode node = load(target);
                            List<Boolean> statics = new ArrayList<>();
                            if (node != null) {
                                for (MethodNode method : node.methods) {
                                    if (method.name.equals(name) && (desc == null || method.desc.equals(desc))) {
                                        statics.add((method.access & Opcodes.ACC_STATIC) != 0);
                                    }
                                }
                            } else if (name.equals("<init>")) {
                                statics.add(false);
                            }
                            for (boolean targetStatic : statics) {
                                checked++;
                                boolean invalid = exact ? targetStatic != handlerStatic : !handlerStatic && targetStatic;
                                if (invalid) {
                                    problems.add(mixin.name + "." + handler.name + " (@" + kind + ", static=" + handlerStatic
                                            + ") -> " + target + "." + selector + " (static=" + targetStatic + ")");
                                }
                            }
                        }
                    }
                }
            }
        }
        assertTrue(checked > 50, "too few handlers resolved: " + checked);
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    private static List<ClassNode> vroMixins() throws IOException, URISyntaxException {
        Path root = Path.of(MixinHandlerModifierTest.class.getClassLoader()
                .getResource("dev/hoyin1600p/vault_render_optimization/VaultRenderOptimization.class").toURI())
                .getParent().getParent().getParent();
        List<ClassNode> mixins = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root.resolve("hoyin1600p"))) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".class"))::iterator) {
                ClassNode node = new ClassNode();
                new ClassReader(Files.readAllBytes(file)).accept(node, ClassReader.SKIP_CODE);
                if (node.invisibleAnnotations != null
                        && node.invisibleAnnotations.stream().anyMatch(a -> a.desc.equals(MIXIN))) {
                    mixins.add(node);
                }
            }
        }
        return mixins;
    }

    private static List<String> targets(ClassNode mixin) {
        List<String> targets = new ArrayList<>();
        for (AnnotationNode annotation : mixin.invisibleAnnotations) {
            if (!annotation.desc.equals(MIXIN) || annotation.values == null) continue;
            for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                Object value = annotation.values.get(i + 1);
                if (!(value instanceof List<?> list)) continue;
                for (Object entry : list) {
                    if (entry instanceof Type type) targets.add(type.getInternalName());
                    else if (entry instanceof String name) targets.add(name.replace('.', '/'));
                }
            }
        }
        return targets;
    }

    private static List<String> stringList(AnnotationNode annotation, String key) {
        List<String> values = new ArrayList<>();
        if (annotation.values == null) return values;
        for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
            if (key.equals(annotation.values.get(i)) && annotation.values.get(i + 1) instanceof List<?> list) {
                for (Object entry : list) values.add(String.valueOf(entry));
            }
        }
        return values;
    }

    private static String atValue(AnnotationNode annotation) {
        if (annotation.values == null) return null;
        for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
            if (!"at".equals(annotation.values.get(i))) continue;
            Object at = annotation.values.get(i + 1);
            if (at instanceof List<?> list && !list.isEmpty()) at = list.get(0);
            if (at instanceof AnnotationNode node && node.values != null) {
                for (int k = 0; k + 1 < node.values.size(); k += 2) {
                    if ("value".equals(node.values.get(k))) return String.valueOf(node.values.get(k + 1));
                }
            }
        }
        return null;
    }

    private static ClassNode load(String internalName) throws IOException {
        try (InputStream in = MixinHandlerModifierTest.class.getClassLoader().getResourceAsStream(internalName + ".class")) {
            if (in == null) return null;
            ClassNode node = new ClassNode();
            new ClassReader(in.readAllBytes()).accept(node, ClassReader.SKIP_CODE);
            return node;
        }
    }
}
