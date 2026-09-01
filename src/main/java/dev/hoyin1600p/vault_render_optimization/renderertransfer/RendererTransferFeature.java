package dev.hoyin1600p.vault_render_optimization.renderertransfer;

import java.util.List;

/** Renderer-fork corrections transferred into VRO under explicit ownership gates. */
public enum RendererTransferFeature {
    ADJACENT_BLOCK_OCCLUSION(
            "VRO-EMB-04", "adjacentBlockOcclusion", "Adjacent-position block occlusion correction",
            true, true, true, List.of("BlockOcclusionCacheMixin")
    ),
    NULL_BUFFER_VERTEX_SINK(
            "VRO-EMB-05", "nullBufferVertexSink", "Null-buffer optimized vertex-sink guard",
            true, true, true, List.of("BufferBuilderVertexSinkMixin")
    ),
    DIRECT_CCL_RENDERER_LOOKUP(
            "VRO-EMB-10", "directCclRendererLookup", "Direct CodeChickenLib renderer lookup",
            false, true, false, List.of("CCLCompatMixin")
    ),
    VERTEX_BUFFER_RETENTION(
            "VRO-EMB-02", "vertexBufferRetention", "Bounded vertex-buffer retention",
            false, true, true, List.of("VertexBufferBuilderMixin")
    ),
    ASYNC_ARENA_GROWTH(
            "VRO-EMB-03", "asyncArenaGrowth", "Preemptive asynchronous buffer-arena growth",
            false, true, true, List.of("AsyncBufferArenaMixin")
    ),
    SMOOTH_FLUID_LIGHTING(
            "VRO-EMB-06", "smoothFluidLighting", "Cached smooth-lighting selection for fluids",
            false, true, true, List.of("FluidRendererMixin")
    ),
    CHUNK_LAYER_COLOR_RESET(
            "VRO-EMB-08", "chunkLayerColorReset", "Chunk-layer shader-color reset",
            true, true, true, List.of("SodiumWorldRendererColorResetMixin")
    ),
    CHUNK_REBUILD_DEDUPLICATION(
            "VRO-EMB-01", "chunkRebuildDeduplication", "Chunk rebuild de-duplication",
            false, true, true,
            List.of("EmbeddiumRenderSectionMixin", "RubidiumRenderSectionMixin", "RenderSectionManagerMixin")
    );

    private final String id;
    private final String configKey;
    private final String displayName;
    private final boolean activeInCompareMode;
    private final boolean embeddiumSupported;
    private final boolean rubidiumSupported;
    private final List<String> mixins;

    RendererTransferFeature(
            String id,
            String configKey,
            String displayName,
            boolean activeInCompareMode,
            boolean embeddiumSupported,
            boolean rubidiumSupported,
            List<String> mixins
    ) {
        this.id = id;
        this.configKey = configKey;
        this.displayName = displayName;
        this.activeInCompareMode = activeInCompareMode;
        this.embeddiumSupported = embeddiumSupported;
        this.rubidiumSupported = rubidiumSupported;
        this.mixins = List.copyOf(mixins);
    }

    public String id() { return id; }
    public String configKey() { return configKey; }
    public String displayName() { return displayName; }
    public boolean activeInCompareMode() { return activeInCompareMode; }

    public boolean supports(RendererFamily family) {
        return family == RendererFamily.EMBEDDIUM ? embeddiumSupported
                : family == RendererFamily.RUBIDIUM && rubidiumSupported;
    }

    public boolean mixinMatchesFamily(String simpleName, RendererFamily family) {
        if (!mixins.contains(simpleName)) {
            return false;
        }
        if (simpleName.startsWith("Embeddium")) {
            return family == RendererFamily.EMBEDDIUM;
        }
        if (simpleName.startsWith("Rubidium")) {
            return family == RendererFamily.RUBIDIUM;
        }
        return true;
    }

    public static RendererTransferFeature forMixin(String mixinClassName) {
        if (!mixinClassName.contains(".mixin.backport.embeddium.")) {
            return null;
        }
        String simpleName = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        for (RendererTransferFeature feature : values()) {
            if (feature.mixins.contains(simpleName)) {
                return feature;
            }
        }
        return null;
    }
}
