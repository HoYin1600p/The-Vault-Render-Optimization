package dev.hoyin1600p.vault_render_optimization.client.chunk;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;

/** Small status snapshot only: no owned jobs, futures, executors, or per-frame logging. */
public final class ChunkUpdateState {
    private static volatile ChunkUpdateBackend backend = ChunkUpdateBackend.BLOCKED;
    private static volatile boolean observed;
    private static volatile boolean nativeDeferred;

    private ChunkUpdateState() {
    }

    public static void configure(ChunkUpdateBackend selected) {
        backend = selected;
        observed = false;
    }

    public static boolean defer(boolean rendererDeferred) {
        // Avoid a volatile write per section/job when the renderer preference is unchanged.
        if (!observed || nativeDeferred != rendererDeferred) {
            nativeDeferred = rendererDeferred;
            observed = true;
        }
        return ChunkUpdatePolicy.effectiveDeferral(rendererDeferred,
                ClientOptimizationConfig.deferChunkUpdates, ClientOptimizationConfig.compareModeEnabled());
    }

    public static String status() {
        if (backend == ChunkUpdateBackend.BLOCKED) {
            return "BLOCKED: unsupported/ambiguous renderer, non-client, or failed discovery; native behavior unchanged";
        }
        boolean requested = ChunkUpdatePolicy.vroRequestsDeferral(
                ClientOptimizationConfig.deferChunkUpdates, ClientOptimizationConfig.compareModeEnabled());
        return backend + ": " + (requested ? "VRO requests native asynchronous updates" : "VRO yields to native settings")
                + "; scheduling hook " + (observed ? "observed" : "not observed yet")
                + "; last native preference " + (!observed ? "unknown" : nativeDeferred ? "deferred" : "may block")
                + "; Compare Mode " + (ClientOptimizationConfig.compareModeEnabled() ? "on" : "off");
    }
}
