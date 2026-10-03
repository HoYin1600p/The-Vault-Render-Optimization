package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.mixin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.service.ServiceNotAvailableError;
import sun.misc.Unsafe;

/**
 * VRO's Create shader compatibility is a newer adaptation of Oculus Flywheel Compat
 * ({@code irisflw}). Both patch the same Flywheel and Oculus methods, so only one may run. When VRO's
 * compatibility is available, irisflw's two mixin configs are emptied before Mixin prepares them;
 * its mod class only logs, so nothing else of it runs.
 *
 * <p>Mixin's config classes live in a package that its module does not export or open, so normal
 * reflection fails under Forge's module layers. {@link Unsafe} field access is used instead, on
 * exactly three list fields of configs named {@code irisflw.mixins.*}, and only while they are
 * unprepared. Any failure returns false so the caller can disable VRO's copy instead; the two are
 * never allowed to run together.
 */
final class IrisFlwOverride {
    static final String CONFIG_PREFIX = "irisflw.mixins.";
    static final Set<String> LIST_FIELDS = Set.of("mixinClasses", "mixinClassesClient", "mixinClassesServer");

    private IrisFlwOverride() {
    }

    /** Result of the attempt; {@code disabledConfigs} lists the emptied configs. */
    record Result(boolean success, List<String> disabledConfigs, String detail) {
    }

    static Result disableIrisFlwMixins() {
        try {
            Unsafe unsafe = unsafe();
            List<String> disabled = new ArrayList<>();
            for (Object config : Mixins.getConfigs()) {
                Object mixinConfig = read(unsafe, config, "config");
                String name = mixinConfig == null ? null : (String) read(unsafe, mixinConfig, "name");
                if (name == null || !name.startsWith(CONFIG_PREFIX)) {
                    continue;
                }
                if (Boolean.TRUE.equals(readBoolean(unsafe, mixinConfig, "prepared"))) {
                    return new Result(false, disabled, name + " was already prepared");
                }
                clearLists(unsafe, mixinConfig);
                disabled.add(name);
            }
            if (disabled.isEmpty()) {
                return new Result(false, disabled, "no irisflw mixin configs were registered");
            }
            return new Result(true, disabled, "emptied " + disabled);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError | ServiceNotAvailableError failure) {
            return new Result(false, List.of(), failure.toString());
        }
    }

    /** Package-private for tests: empties the three mixin-class lists of a MixinConfig-shaped object. */
    static void clearLists(Unsafe unsafe, Object mixinConfig) throws NoSuchFieldException {
        for (String fieldName : LIST_FIELDS) {
            Field field = field(mixinConfig.getClass(), fieldName);
            unsafe.putObject(mixinConfig, unsafe.objectFieldOffset(field), new ArrayList<String>());
        }
    }

    static Unsafe unsafe() throws ReflectiveOperationException {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    private static Object read(Unsafe unsafe, Object owner, String fieldName) throws NoSuchFieldException {
        return unsafe.getObject(owner, unsafe.objectFieldOffset(field(owner.getClass(), fieldName)));
    }

    private static Boolean readBoolean(Unsafe unsafe, Object owner, String fieldName) throws NoSuchFieldException {
        return unsafe.getBoolean(owner, unsafe.objectFieldOffset(field(owner.getClass(), fieldName)));
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getName().equals(name) && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    return field;
                }
            }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }
}
