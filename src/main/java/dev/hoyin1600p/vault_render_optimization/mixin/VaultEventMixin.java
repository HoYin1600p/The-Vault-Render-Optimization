package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import iskallia.vault.core.event.Event;
import iskallia.vault.core.event.client.AmbientLightEvent;
import iskallia.vault.core.event.client.BiomeColorsEvent;
import iskallia.vault.core.event.client.DimensionEffectEvent;
import iskallia.vault.core.event.client.RenderLevelLastEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings({"rawtypes", "unchecked"})
@Mixin(value = Event.class, remap = false)
public abstract class VaultEventMixin {
    @Shadow
    protected boolean child;

    @Shadow
    protected Event parent;

    @Shadow
    protected Map<Integer, Map<Object, List<Consumer<Object>>>> listeners;

    @Unique
    private volatile List<Consumer<Object>> vault_render_optimization$listenerSnapshot;

    @Inject(method = "invoke", at = @At("HEAD"), cancellable = true)
    private void vault_render_optimization$invokeFromCachedSnapshot(Object data, CallbackInfoReturnable<Object> cir) {
        if (!this.vault_render_optimization$usesCachedSnapshot()) {
            return;
        }

        if (this.child) {
            this.parent.invoke(data);
            cir.setReturnValue(data);
            return;
        }

        for (Consumer<Object> listener : this.vault_render_optimization$getListenerSnapshot()) {
            try {
                listener.accept(data);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        cir.setReturnValue(data);
    }

    @Unique
    private boolean vault_render_optimization$usesCachedSnapshot() {
        if (!ClientOptimizationConfig.optimizationsEnabled()) {
            return false;
        }
        Object event = this;
        return event instanceof BiomeColorsEvent
                || event instanceof DimensionEffectEvent
                || event instanceof AmbientLightEvent
                || event instanceof RenderLevelLastEvent;
    }

    @Inject(
            method = "register(Ljava/lang/Object;Ljava/util/function/Consumer;I)Liskallia/vault/core/event/Event;",
            at = @At("RETURN")
    )
    private void vault_render_optimization$invalidateAfterRegister(Object owner, Consumer<Object> listener, int priority, CallbackInfoReturnable<Event> cir) {
        this.vault_render_optimization$listenerSnapshot = null;
    }

    @Inject(method = "release", at = @At("RETURN"))
    private void vault_render_optimization$invalidateAfterRelease(Object owner, CallbackInfoReturnable<Event> cir) {
        this.vault_render_optimization$listenerSnapshot = null;
    }

    @Unique
    private List<Consumer<Object>> vault_render_optimization$getListenerSnapshot() {
        List<Consumer<Object>> snapshot = this.vault_render_optimization$listenerSnapshot;
        if (snapshot != null) {
            return snapshot;
        }

        synchronized (this) {
            snapshot = this.vault_render_optimization$listenerSnapshot;
            if (snapshot == null) {
                snapshot = this.vault_render_optimization$buildListenerSnapshot();
                this.vault_render_optimization$listenerSnapshot = snapshot;
            }
        }

        return snapshot;
    }

    @Unique
    private List<Consumer<Object>> vault_render_optimization$buildListenerSnapshot() {
        List<Integer> priorities;
        synchronized (this.listeners) {
            priorities = new ArrayList<>(this.listeners.keySet());
        }

        priorities.sort(Comparator.reverseOrder());

        List<Consumer<Object>> snapshot = new ArrayList<>();
        for (Integer priority : priorities) {
            Map<Object, List<Consumer<Object>>> priorityListeners;
            synchronized (this.listeners) {
                priorityListeners = this.listeners.get(priority);
            }

            if (priorityListeners == null) {
                continue;
            }

            List<List<Consumer<Object>>> listenerGroups;
            synchronized (priorityListeners) {
                listenerGroups = new ArrayList<>(priorityListeners.values());
            }

            for (List<Consumer<Object>> listenerGroup : listenerGroups) {
                synchronized (listenerGroup) {
                    snapshot.addAll(listenerGroup);
                }
            }
        }

        return Collections.unmodifiableList(snapshot);
    }
}
