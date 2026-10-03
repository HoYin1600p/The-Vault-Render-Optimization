package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Structural check of VRO's shader-compat code against the installed Oculus jar, run before any
 * of its mixins are applied. Several different Oculus builds (public 1.6.4 and HoYin1600p's
 * dh-compat builds) share the 1.6.x line, and some share a version string, so the version alone
 * cannot say whether the injection targets exist. A missing class or member disables the compat
 * with a message instead of failing a {@code require=1} injection or throwing NoSuchMethodError.
 *
 * <p>Only VRO classes are read as bytes; nothing is loaded. Members inherited from outside Oculus
 * (for example from {@code ShaderInstance}) cannot be verified here and are accepted.
 */
public final class OculusCompatContract {
    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/";
    private static final String CALLBACK_INFO = "org/spongepowered/asm/mixin/injection/callback/";

    private final Function<String, byte[]> rendererClasses;
    private final Map<String, ClassNode> cache = new HashMap<>();
    private final Set<String> problems = new LinkedHashSet<>();

    private OculusCompatContract(Function<String, byte[]> rendererClasses) {
        this.rendererClasses = rendererClasses;
    }

    /**
     * @param vroClasses      bytes of the VRO classes to check
     * @param rendererClasses Oculus class bytes by internal name (for example {@code net/coderbot/iris/Iris}),
     *                        or null when absent
     * @return human-readable problems; empty when every Oculus reference resolves
     */
    public static List<String> problems(Iterable<byte[]> vroClasses, Function<String, byte[]> rendererClasses) {
        OculusCompatContract contract = new OculusCompatContract(rendererClasses);
        for (byte[] bytes : vroClasses) {
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, ClassReader.SKIP_FRAMES);
            contract.checkReferences(node);
            contract.checkMixin(node);
        }
        return new ArrayList<>(contract.problems);
    }

    static boolean isOculus(String internalName) {
        return internalName != null
                && (internalName.startsWith("net/coderbot/iris/") || internalName.startsWith("net/irisshaders/"));
    }

    private void checkReferences(ClassNode node) {
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode call && isOculus(call.owner)) {
                    requireMethod(call.owner, call.name, call.desc, node.name);
                } else if (insn instanceof FieldInsnNode field && isOculus(field.owner)) {
                    requireField(field.owner, field.name, field.desc, node.name);
                } else if (insn instanceof TypeInsnNode type) {
                    requireType(Type.getObjectType(type.desc), node.name);
                } else if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Type type) {
                    requireType(type, node.name);
                } else if (insn instanceof InvokeDynamicInsnNode indy) {
                    for (Object argument : indy.bsmArgs) {
                        if (argument instanceof Handle handle && isOculus(handle.getOwner())) {
                            if (handle.getTag() <= 4) {
                                requireField(handle.getOwner(), handle.getName(), handle.getDesc(), node.name);
                            } else {
                                requireMethod(handle.getOwner(), handle.getName(), handle.getDesc(), node.name);
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkMixin(ClassNode mixin) {
        AnnotationNode annotation = annotation(mixin.visibleAnnotations, mixin.invisibleAnnotations, "Mixin;");
        if (annotation == null) {
            return;
        }
        List<String> targets = new ArrayList<>();
        for (Object value : values(annotation, "value")) {
            targets.add(((Type) value).getInternalName());
        }
        for (Object value : values(annotation, "targets")) {
            targets.add(((String) value).replace('.', '/'));
        }
        for (String target : targets) {
            if (!isOculus(target)) {
                continue;
            }
            ClassNode targetNode = load(target);
            if (targetNode == null) {
                problems.add("missing class " + target + " (mixin " + simple(mixin.name) + ")");
                continue;
            }
            for (FieldNode field : mixin.fields) {
                if (annotation(field.visibleAnnotations, field.invisibleAnnotations, "Shadow;") != null
                        && findField(target, field.name, field.desc) == null) {
                    problems.add("missing field " + simple(target) + "." + field.name + " (mixin " + simple(mixin.name) + ")");
                }
            }
            for (MethodNode method : mixin.methods) {
                checkMixinMethod(mixin, target, method);
            }
        }
    }

    private void checkMixinMethod(ClassNode mixin, String target, MethodNode method) {
        String where = " (mixin " + simple(mixin.name) + ")";
        if (annotation(method.visibleAnnotations, method.invisibleAnnotations, "Shadow;") != null) {
            if (findMethod(target, method.name, method.desc) == null) {
                problems.add("missing method " + simple(target) + "." + method.name + method.desc + where);
            }
            return;
        }
        AnnotationNode invoker = annotation(method.visibleAnnotations, method.invisibleAnnotations, "gen/Invoker;");
        if (invoker != null) {
            String name = first(invoker, "value", stripPrefix(method.name, "call", "invoke"));
            if (findMethod(target, name, method.desc) == null) {
                problems.add("missing method " + simple(target) + "." + name + method.desc + where);
            }
            return;
        }
        AnnotationNode accessor = annotation(method.visibleAnnotations, method.invisibleAnnotations, "gen/Accessor;");
        if (accessor != null) {
            String name = first(accessor, "value", stripPrefix(method.name, "get", "set", "is"));
            Type methodType = Type.getMethodType(method.desc);
            Type fieldType = methodType.getArgumentTypes().length == 1
                    ? methodType.getArgumentTypes()[0] : methodType.getReturnType();
            if (findField(target, name, fieldType.getDescriptor()) == null) {
                problems.add("missing field " + simple(target) + "." + name + where);
            }
            return;
        }
        for (String injector : new String[]{"Inject;", "Redirect;", "ModifyArg;", "ModifyArgs;", "ModifyVariable;", "ModifyConstant;"}) {
            AnnotationNode inject = annotation(method.visibleAnnotations, method.invisibleAnnotations, "injection/" + injector);
            if (inject == null) {
                continue;
            }
            for (Object selector : values(inject, "method")) {
                checkSelector(target, (String) selector, injector.equals("Inject;") ? method.desc : null, where);
            }
        }
    }

    /** Selector forms used by VRO: {@code name}, {@code name*} and {@code name(desc)}. */
    private void checkSelector(String target, String selector, String injectHandlerDesc, String where) {
        String name = selector;
        String desc = null;
        int paren = selector.indexOf('(');
        if (paren >= 0) {
            name = selector.substring(0, paren);
            desc = selector.substring(paren);
        } else if (selector.endsWith("*")) {
            name = selector.substring(0, selector.length() - 1);
        }
        ClassNode node = load(target);
        if (node == null) {
            return;
        }
        boolean found = false;
        for (MethodNode candidate : node.methods) {
            if (!candidate.name.equals(name) || desc != null && !candidate.desc.equals(desc)) {
                continue;
            }
            if (desc == null && injectHandlerDesc != null && !argumentsPrefix(injectHandlerDesc, candidate.desc)) {
                continue;
            }
            found = true;
            break;
        }
        if (!found) {
            problems.add("missing injection target " + simple(target) + "." + selector + where);
        }
    }

    /** An {@code @Inject} handler's leading arguments (before CallbackInfo) must match the target's. */
    private static boolean argumentsPrefix(String handlerDesc, String targetDesc) {
        Type[] handler = Type.getArgumentTypes(handlerDesc);
        Type[] targetArguments = Type.getArgumentTypes(targetDesc);
        int count = 0;
        while (count < handler.length && !handler[count].getInternalName().startsWith(CALLBACK_INFO)) {
            count++;
        }
        if (count == 0) {
            return true;
        }
        if (count > targetArguments.length) {
            return false;
        }
        for (int index = 0; index < count; index++) {
            if (!handler[index].equals(targetArguments[index])) {
                return false;
            }
        }
        return true;
    }

    private void requireType(Type type, String from) {
        while (type.getSort() == Type.ARRAY) {
            type = type.getElementType();
        }
        if (type.getSort() == Type.OBJECT && isOculus(type.getInternalName()) && load(type.getInternalName()) == null) {
            problems.add("missing class " + type.getInternalName() + " (used by " + simple(from) + ")");
        }
    }

    private void requireMethod(String owner, String name, String desc, String from) {
        if (load(owner) == null) {
            problems.add("missing class " + owner + " (used by " + simple(from) + ")");
        } else if (findMethod(owner, name, desc) == null) {
            problems.add("missing method " + simple(owner) + "." + name + desc + " (used by " + simple(from) + ")");
        }
    }

    private void requireField(String owner, String name, String desc, String from) {
        if (load(owner) == null) {
            problems.add("missing class " + owner + " (used by " + simple(from) + ")");
        } else if (findField(owner, name, desc) == null) {
            problems.add("missing field " + simple(owner) + "." + name + " (used by " + simple(from) + ")");
        }
    }

    /** Returns a non-null marker when found, or when the hierarchy leaves Oculus and cannot be checked. */
    private Object findMethod(String owner, String name, String desc) {
        ClassNode node = load(owner);
        if (node == null) {
            return isOculus(owner) ? null : Boolean.TRUE;
        }
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) {
                return method;
            }
        }
        if (name.equals("<init>")) {
            return null;
        }
        List<String> parents = new ArrayList<>(node.interfaces);
        if (node.superName != null && !node.superName.equals("java/lang/Object")) {
            parents.add(0, node.superName);
        }
        for (String parent : parents) {
            Object found = findMethod(parent, name, desc);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private Object findField(String owner, String name, String desc) {
        ClassNode node = load(owner);
        if (node == null) {
            return isOculus(owner) ? null : Boolean.TRUE;
        }
        for (FieldNode field : node.fields) {
            if (field.name.equals(name) && field.desc.equals(desc)) {
                return field;
            }
        }
        List<String> parents = new ArrayList<>(node.interfaces);
        if (node.superName != null && !node.superName.equals("java/lang/Object")) {
            parents.add(0, node.superName);
        }
        for (String parent : parents) {
            Object found = findField(parent, name, desc);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private ClassNode load(String internalName) {
        if (!isOculus(internalName)) {
            return null;
        }
        return cache.computeIfAbsent(internalName, name -> {
            byte[] bytes = rendererClasses.apply(name);
            if (bytes == null) {
                return null;
            }
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
            return node;
        });
    }

    /**
     * {@code path} is relative to the mixin package, for example {@code "injection/Inject;"}. Mixin
     * annotations mix retentions ({@code @Mixin} is CLASS, {@code @Inject}/{@code @Shadow} are
     * RUNTIME), so both lists are searched.
     */
    private static AnnotationNode annotation(List<AnnotationNode> visible, List<AnnotationNode> invisible, String path) {
        String desc = MIXIN + path;
        for (List<AnnotationNode> annotations : List.of(
                visible == null ? List.<AnnotationNode>of() : visible,
                invisible == null ? List.<AnnotationNode>of() : invisible)) {
            for (AnnotationNode annotation : annotations) {
                if (annotation.desc.equals(desc)) {
                    return annotation;
                }
            }
        }
        return null;
    }

    private static List<Object> values(AnnotationNode annotation, String key) {
        if (annotation.values == null) {
            return List.of();
        }
        for (int index = 0; index + 1 < annotation.values.size(); index += 2) {
            if (key.equals(annotation.values.get(index))) {
                Object value = annotation.values.get(index + 1);
                if (value instanceof List<?> list) {
                    return new ArrayList<>(list);
                }
                return List.of(value);
            }
        }
        return List.of();
    }

    private static String first(AnnotationNode annotation, String key, String fallback) {
        List<Object> found = values(annotation, key);
        return found.isEmpty() || ((String) found.get(0)).isEmpty() ? fallback : (String) found.get(0);
    }

    private static String stripPrefix(String name, String... prefixes) {
        for (String prefix : prefixes) {
            if (name.startsWith(prefix) && name.length() > prefix.length()) {
                String rest = name.substring(prefix.length());
                return Character.toLowerCase(rest.charAt(0)) + rest.substring(1);
            }
        }
        return name;
    }

    private static String simple(String internalName) {
        return internalName.substring(internalName.lastIndexOf('/') + 1);
    }
}
