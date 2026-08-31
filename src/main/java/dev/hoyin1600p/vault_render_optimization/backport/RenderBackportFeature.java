package dev.hoyin1600p.vault_render_optimization.backport;

import java.util.List;

public enum RenderBackportFeature {
    CHUNK_MESHING(
            "chunk_meshing",
            "chunkMeshing",
            "Chunk meshing",
            "render",
            List.of("perf.chunk_meshing.RebuildTaskMixin"),
            List.of(),
            List.of("dev.hoyin1600p.vhaccelerator.mixin.backport.modernfix.client.render.RebuildTaskMixin")
    ),
    BUFFER_BUILDER_LEAK_FIX(
            "buffer_builder_leak_fix",
            "bufferBuilderLeakFix",
            "BufferBuilder leak correction",
            "buffer",
            List.of("bugfix.buffer_builder_leak.RenderBuffersMixin"),
            List.of("org.embeddedt.modernfix.common.mixin.bugfix.buffer_builder_leak.RenderBuffersMixin"),
            List.of("dev.hoyin1600p.vhaccelerator.mixin.backport.modernfix.client.buffer.RenderBuffersMixin")
    ),
    ENTITY_MODEL_COMPACTION(
            "entity_model_compaction",
            "compactEntityModels",
            "Entity-model compaction",
            "entity",
            List.of("perf.compact_entity_models.CubeDefinitionMixin"),
            List.of("org.embeddedt.modernfix.common.mixin.perf.compact_entity_models.CubeDefinitionMixin"),
            List.of(
                    "dev.hoyin1600p.vhaccelerator.mixin.backport.modernfix.client.entity.CubeDefinitionMixin",
                    "dev.hoyin1600p.vhaccelerator.mixin.backport.modernfix.client.entity.EntityModelSetMixin"
            )
    ),
    PROFILE_TEXTURE_HASH_CACHE(
            "profile_texture_hash_cache",
            "profileTextureHashCache",
            "Profile-texture hash cache",
            "skin",
            List.of("perf.cache_profile_texture_url.SkinManagerMixin"),
            List.of("org.embeddedt.modernfix.common.mixin.perf.cache_profile_texture_url.SkinManagerMixin"),
            List.of("dev.hoyin1600p.vhaccelerator.mixin.backport.modernfix.client.skin.SkinManagerMixin")
    ),
    MODEL_SELECTOR_PREDICATE_CACHE(
            "model_selector_predicate_cache",
            "modelSelectorPredicateCache",
            "Multipart model-selector predicate cache",
            "model.selector",
            List.of("perf.model_optimizations.SelectorMixin"),
            List.of("org.embeddedt.modernfix.common.mixin.perf.model_optimizations.SelectorMixin"),
            List.of("dev.hoyin1600p.vhaccelerator.mixin.backport.modernfix.client.model.selector.SelectorMixin")
    ),
    MODEL_VARIANT_TRAVERSAL(
            "model_variant_traversal",
            "modelVariantTraversal",
            "Allocation-light model-variant traversal",
            "model.variant",
            List.of("perf.model_optimizations.MultiVariantMixin"),
            List.of("org.embeddedt.modernfix.common.mixin.perf.model_optimizations.MultiVariantMixin"),
            List.of("dev.hoyin1600p.vhaccelerator.mixin.backport.modernfix.client.model.variant.MultiVariantMixin")
    ),
    MODEL_TRANSFORMATION_HASH_CACHE(
            "model_transformation_hash_cache",
            "modelTransformationHashCache",
            "Model transformation hash cache",
            "model.transformation",
            List.of("perf.model_optimizations.TransformationMatrixMixin"),
            List.of("org.embeddedt.modernfix.common.mixin.perf.model_optimizations.TransformationMatrixMixin"),
            List.of("dev.hoyin1600p.vhaccelerator.mixin.backport.modernfix.client.model.transformation.TransformationMixin")
    ),
    OBJ_MODEL_CACHE_CONCURRENCY(
            "obj_model_cache_concurrency",
            "objModelCacheConcurrency",
            "Thread-safe Forge OBJ model caches",
            "model.obj",
            List.of("perf.model_optimizations.OBJLoaderMixin"),
            List.of("org.embeddedt.modernfix.forge.mixin.perf.model_optimizations.OBJLoaderMixin"),
            List.of("dev.hoyin1600p.vhaccelerator.mixin.backport.modernfix.client.model.obj.OBJLoaderMixin")
    ),
    FASTER_TEXTURE_STITCHING(
            "faster_texture_stitching",
            "fasterTextureStitching",
            "Faster texture atlas stitching",
            "texture",
            List.of("perf.faster_texture_stitching.StitcherMixin"),
            List.of("org.embeddedt.modernfix.common.mixin.perf.faster_texture_stitching.StitcherMixin"),
            List.of("dev.hoyin1600p.vhaccelerator.mixin.backport.modernfix.client.texture.StitcherMixin")
    ),
    MODEL_DATA_MANAGER_CONCURRENCY(
            "model_data_manager_concurrency",
            "modelDataManagerConcurrencyFix",
            "Forge model-data concurrency correction",
            "modeldata",
            List.of("bugfix.model_data_manager_cme.ModelDataManagerMixin"),
            List.of("org.embeddedt.modernfix.forge.mixin.bugfix.model_data_manager_cme.ModelDataManagerMixin"),
            List.of("dev.hoyin1600p.vhaccelerator.mixin.backport.modernfix.client.modeldata.ModelDataManagerMixin")
    ),
    CTM_METADATA_CACHE_CONCURRENCY(
            "ctm_metadata_cache_concurrency",
            "ctmMetadataCacheConcurrencyFix",
            "ConnectedTexturesMod metadata-cache concurrency correction",
            "ctm",
            List.of("bugfix.ctm_resourceutil_cme.ResourceUtilMixin"),
            List.of("org.embeddedt.modernfix.forge.mixin.bugfix.ctm_resourceutil_cme.ResourceUtilMixin"),
            List.of("dev.hoyin1600p.vhaccelerator.mixin.backport.modernfix.client.ctm.ResourceUtilMixin")
    );

    private final String id;
    private final String configKey;
    private final String displayName;
    private final String mixinPackageFragment;
    private final List<String> modernFixMixinKeys;
    private final List<String> modernFixMarkerClasses;
    private final List<String> vhAcceleratorMarkerClasses;

    RenderBackportFeature(
            String id,
            String configKey,
            String displayName,
            String mixinPackageFragment,
            List<String> modernFixMixinKeys,
            List<String> modernFixMarkerClasses,
            List<String> vhAcceleratorMarkerClasses
    ) {
        this.id = id;
        this.configKey = configKey;
        this.displayName = displayName;
        this.mixinPackageFragment = mixinPackageFragment;
        this.modernFixMixinKeys = List.copyOf(modernFixMixinKeys);
        this.modernFixMarkerClasses = List.copyOf(modernFixMarkerClasses);
        this.vhAcceleratorMarkerClasses = List.copyOf(vhAcceleratorMarkerClasses);
    }

    public String id() {
        return id;
    }

    public String configKey() {
        return configKey;
    }

    public String displayName() {
        return displayName;
    }

    public String mixinPackageFragment() {
        return mixinPackageFragment;
    }

    public List<String> modernFixMixinKeys() {
        return modernFixMixinKeys;
    }

    public List<String> modernFixMarkerClasses() {
        return modernFixMarkerClasses;
    }

    public List<String> vhAcceleratorMarkerClasses() {
        return vhAcceleratorMarkerClasses;
    }

    public static RenderBackportFeature forMixin(String mixinClassName) {
        String prefix = ".mixin.backport.modernfix.client.";
        for (RenderBackportFeature feature : values()) {
            if (mixinClassName.contains(prefix + feature.mixinPackageFragment + ".")) {
                return feature;
            }
        }
        return null;
    }
}
