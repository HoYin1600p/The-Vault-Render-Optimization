package dev.hoyin1600p.vault_render_optimization.client.render;

import net.minecraft.client.gui.components.toasts.ToastComponent;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

public final class ToastVisibilityProbe {
    @Nullable
    private static final Field VISIBLE_TOASTS_FIELD = findVisibleToastsField();

    private ToastVisibilityProbe() {
    }

    public static boolean hasNoVisibleToasts(ToastComponent toastComponent) {
        if (VISIBLE_TOASTS_FIELD == null) {
            return false;
        }

        try {
            Object[] visibleToasts = (Object[]) VISIBLE_TOASTS_FIELD.get(toastComponent);
            for (Object visibleToast : visibleToasts) {
                if (visibleToast != null) {
                    return false;
                }
            }
            return true;
        } catch (IllegalAccessException ignored) {
            return false;
        }
    }

    @Nullable
    private static Field findVisibleToastsField() {
        for (Field field : ToastComponent.class.getDeclaredFields()) {
            Class<?> type = field.getType();
            if (type.isArray() && type.getComponentType().getEnclosingClass() == ToastComponent.class) {
                try {
                    field.setAccessible(true);
                    return field;
                } catch (RuntimeException ignored) {
                    return null;
                }
            }
        }
        return null;
    }
}
