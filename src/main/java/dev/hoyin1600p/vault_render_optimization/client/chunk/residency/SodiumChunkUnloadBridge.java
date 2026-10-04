package dev.hoyin1600p.vault_render_optimization.client.chunk.residency;

import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import dev.hoyin1600p.vault_render_optimization.util.ModIds;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraftforge.fml.ModList;

/**
 * Forwards bound-initiated chunk forgets to Embeddium/Rubidium. Their unload hook sits at RETURN of
 * the forget handler Farsight cancels, so it never runs on its own. Resolved through method
 * handles (ported from VH Accelerator) so VRO still loads on vanilla; any failure disables only
 * this notification.
 */
final class SodiumChunkUnloadBridge {
    private static final String RENDERER_CLASS = "me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer";
    private static final MethodHandle INSTANCE_NULLABLE;
    private static final MethodHandle ON_CHUNK_REMOVED;
    private static volatile boolean available;

    static {
        MethodHandle instanceHandle = null;
        MethodHandle removedHandle = null;
        if (rendererInstalled()) {
            try {
                Class<?> rendererClass = Class.forName(RENDERER_CLASS, false,
                        SodiumChunkUnloadBridge.class.getClassLoader());
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                instanceHandle = lookup.findStatic(rendererClass, "instanceNullable", MethodType.methodType(rendererClass));
                removedHandle = lookup.findVirtual(rendererClass, "onChunkRemoved",
                        MethodType.methodType(void.class, int.class, int.class));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                VaultRenderOptimization.LOGGER.warn(
                        "Could not bind the renderer chunk-unload hook; Farsight-bounded chunks will not release"
                                + " their render sections", failure);
                instanceHandle = null;
                removedHandle = null;
            }
        }
        INSTANCE_NULLABLE = instanceHandle;
        ON_CHUNK_REMOVED = removedHandle;
        available = instanceHandle != null && removedHandle != null;
    }

    private SodiumChunkUnloadBridge() {
    }

    static boolean available() {
        return available;
    }

    static void onChunkRemoved(int chunkX, int chunkZ) {
        if (!available) {
            return;
        }
        try {
            Object renderer = INSTANCE_NULLABLE.invoke();
            if (renderer != null) {
                ON_CHUNK_REMOVED.invoke(renderer, chunkX, chunkZ);
            }
        } catch (Throwable failure) {
            available = false;
            VaultRenderOptimization.LOGGER.warn(
                    "The renderer rejected a Farsight-bounded chunk unload; disabling the render-section notification",
                    failure);
        }
    }

    private static boolean rendererInstalled() {
        try {
            ModList modList = ModList.get();
            return modList != null && (modList.isLoaded(ModIds.EMBEDDIUM) || modList.isLoaded(ModIds.RUBIDIUM));
        } catch (RuntimeException | LinkageError failure) {
            return false;
        }
    }
}
