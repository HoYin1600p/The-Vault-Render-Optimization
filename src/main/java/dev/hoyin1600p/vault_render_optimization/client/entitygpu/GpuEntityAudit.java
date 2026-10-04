package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Audits the final, fully mixed classes the GPU path depends on (from the mixin plugin's postApply,
 * which runs after every mixin of every mod has been applied to the class).
 *
 * <ul>
 *   <li>{@code ModelPart.render} and {@code compile}, and {@code ModelPart$Cube.compile}: the GPU path
 *   skips these bodies, so any other mod's injection, redirect or overwrite in them would be silently
 *   bypassed. Only VRO's own hooks and Embeddium/Rubidium's compile overwrite (which writes the same
 *   vertices) are allowed.</li>
 *   <li>{@code BufferBuilder} and {@code BufferUploader}: every hook that fills reserved vertices must
 *   actually be present (they are applied with {@code require = 0} so a mismatch cannot crash the
 *   game), and no other mod may overwrite those methods.</li>
 * </ul>
 */
public final class GpuEntityAudit {
    static final String MIXIN_MERGED = "Lorg/spongepowered/asm/mixin/transformer/meta/MixinMerged;";
    static final String VRO_PREFIX = "dev.hoyin1600p.vault_render_optimization.";
    static final List<String> RENDERER_COMPILE_OVERWRITES = List.of(
            "me.jellysquid.mods.sodium.mixin.features.entity.fast_render.",
            "org.embeddedt.embeddium.impl.mixin.features.entity.fast_render."
    );

    /**
     * Foreign {@code ModelPart.render} HEAD hooks whose bytecode either renders the part in full
     * and cancels, or returns untouched. They are compatible exactly when they run before VRO's
     * hook: a cancelled part never reaches VRO, and one that falls through is still vanilla. VRO's
     * mixin uses a later priority so its callback is placed after theirs; the audit checks the order
     * in the final bytecode. Reviewed: Skin Layers 3D 1.11.1 (3D skin mesh, head/hat reorder) and
     * wildbackport 1.2.3 ({@code skipDraw}: children only).
     */
    static final List<String> PRECEDING_RENDER_HOOKS = List.of(
            "dev.tr7zw.skinlayers.mixin.ModelPartMixin",
            "com.cursedcauldron.wildbackport.core.mixin.client.ModelPartMixin"
    );

    /**
     * Foreign {@code ModelPart.compile} HEAD hooks that only observe, and only while a condition VRO
     * can read is true. Reviewed: Xaero's Minimap 25.2.10 calls
     * {@code XaeroMinimapCore.onEntityIconsModelPartRenderDetection}, a no-op unless
     * {@code EntityRenderTracer.TRACING_MODEL_RENDERS} (radar icon tracing) is set; VRO leaves every
     * part to vanilla while that flag is true ({@link XaeroIconTraceProbe}).
     */
    static final List<String> CONDITIONAL_COMPILE_OBSERVERS = List.of("xaero.common.mixin.MixinModelPart");

    private static volatile boolean xaeroTraceHook;

    /** True when the audited ModelPart carries Xaero's icon-trace hook. */
    public static boolean xaeroTraceHookPresent() {
        return xaeroTraceHook;
    }

    public static final String MODEL_PART = "net/minecraft/client/model/geom/ModelPart";
    public static final String MODEL_PART_CUBE = "net/minecraft/client/model/geom/ModelPart$Cube";
    public static final String BUFFER_BUILDER = "com/mojang/blaze3d/vertex/BufferBuilder";
    public static final String BUFFER_UPLOADER = "com/mojang/blaze3d/vertex/BufferUploader";
    public static final String SPRITE_EXPANDER = "net/minecraft/client/renderer/SpriteCoordinateExpander";
    public static final String GEO_RENDERER = "software/bernie/geckolib3/renderers/geo/IGeoRenderer";
    public static final String ARS_GEO_RENDERER = "software/bernie/ars_nouveau/geckolib3/renderers/geo/IGeoRenderer";
    public static final String CITADEL_BOX = "com/github/alexthe666/citadel/client/model/AdvancedModelBox";
    public static final String ITEM_RENDERER = "net/minecraft/client/renderer/entity/ItemRenderer";
    public static final String MODEL_BLOCK_RENDERER = "net/minecraft/client/renderer/block/ModelBlockRenderer";
    static final Target BLOCK_RENDER_MODEL = new Target("ModelBlockRenderer.renderModel", List.of("renderModel"),
            "(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/resources/model/BakedModel;"
                    + "FFFIILnet/minecraftforge/client/model/data/IModelData;)V");
    /** Embeddium's overwrite of {@code renderModel} (its own writer, reproduced by the item program). */
    static final List<String> BLOCK_MODEL_OVERWRITES = List.of(
            "me.jellysquid.mods.sodium.mixin.features.block.",
            "org.embeddedt.embeddium.impl.mixin.features.block."
    );
    private static volatile ItemWriter blockModelWriter;

    public static ItemWriter blockModelWriter() {
        return blockModelWriter;
    }

    /** @return null when {@code ModelBlockRenderer.renderModel} was audited and usable, otherwise why not */
    public static String blockModelBlocker() {
        String result = RESULTS.get(MODEL_BLOCK_RENDERER);
        if (result == null) return "ModelBlockRenderer was not audited";
        return result.isEmpty() ? null : result;
    }

    public static final String GEO_BLOCK_RENDERER = "software/bernie/geckolib3/renderers/geo/GeoBlockRenderer";
    static final Target GEO_BLOCK_RENDER_CUBE = new Target("GeoBlockRenderer.renderCube", List.of("renderCube"),
            "(Lsoftware/bernie/geckolib3/geo/render/built/GeoCube;Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V");

    /** Embeddium's overwrite of {@code renderQuadList} (its own writer, reproduced by {@link ItemReference}). */
    static final List<String> ITEM_WRITER_OVERWRITES = List.of(
            "me.jellysquid.mods.sodium.mixin.features.item.",
            "org.embeddedt.embeddium.impl.mixin.features.item."
    );

    /**
     * Foreign {@code renderQuadList} HEAD hooks that either draw the list themselves and cancel, or return
     * untouched; compatible exactly when they run before VRO's hook. Reviewed: Quark 3.2-358 (item-sharing
     * fade: draws with its alpha and cancels only while {@code ItemSharingModule.alphaValue != 1}).
     */
    static final List<String> PRECEDING_ITEM_HOOKS = List.of("vazkii.quark.mixin.client.ItemRendererMixin");

    private static volatile ItemWriter itemWriter;

    /** The writer the audited {@code renderQuadList} runs, or null when it was not audited or failed. */
    public static ItemWriter itemWriter() {
        return itemWriter;
    }

    /** @return null when {@code ItemRenderer.renderQuadList} was audited and usable, otherwise why not */
    public static String itemBlocker() {
        String result = RESULTS.get(ITEM_RENDERER);
        if (result == null) return "ItemRenderer was not audited";
        return result.isEmpty() ? null : result;
    }

    private static final Map<String, String> RESULTS = new ConcurrentHashMap<>();

    private GpuEntityAudit() {
    }

    /**
     * One skipped or hooked method: accepted names (official and SRG), its descriptor and, for a hooked method,
     * how many distinct VRO handlers it must call (a HEAD/RETURN pair is two: one half alone is unsafe).
     */
    record Target(String description, List<String> names, String descriptor, int hooks) {
        Target(String description, List<String> names, String descriptor) {
            this(description, names, descriptor, 1);
        }

        boolean matches(MethodNode method) {
            return names.contains(method.name) && method.desc.equals(descriptor);
        }
    }

    static final Target MODEL_PART_RENDER = new Target("ModelPart.render", List.of("render", "m_104306_"),
            "(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V");
    static final Target MODEL_PART_COMPILE = new Target("ModelPart.compile", List.of("compile", "m_104290_"),
            "(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V");
    static final Target CUBE_COMPILE = new Target("ModelPart$Cube.compile", List.of("compile", "m_171332_"),
            "(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V");
    static final Target SPRITE_VERTEX = new Target("SpriteCoordinateExpander.vertex", List.of("vertex", "m_5954_"),
            "(FFFFFFFFFIIFFF)V");
    static final Target SPRITE_UV = new Target("SpriteCoordinateExpander.uv", List.of("uv", "m_7421_"),
            "(FF)Lcom/mojang/blaze3d/vertex/VertexConsumer;");
    static final Target BUILDER_POP = new Target("BufferBuilder.popNextBuffer", List.of("popNextBuffer", "m_85728_"),
            "()Lcom/mojang/datafixers/util/Pair;", 2);
    static final Target BUILDER_END = new Target("BufferBuilder.end", List.of("end", "m_85721_"), "()V");
    static final Target BUILDER_DISCARD = new Target("BufferBuilder.discard", List.of("discard", "m_85730_"), "()V");
    static final Target BUILDER_BEGIN = new Target("BufferBuilder.begin", List.of("begin", "m_166779_"),
            "(Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;Lcom/mojang/blaze3d/vertex/VertexFormat;)V");
    static final Target BUILDER_SORT = new Target("BufferBuilder.setQuadSortOrigin",
            List.of("setQuadSortOrigin", "m_166771_"), "(FFF)V");
    static final Target UPLOADER_END = new Target("BufferUploader.end", List.of("end", "m_85761_"),
            "(Lcom/mojang/blaze3d/vertex/BufferBuilder;)V", 2);
    static final Target UPLOADER_DRAW = new Target("BufferUploader._end", List.of("_end", "m_166838_"),
            "(Ljava/nio/ByteBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;Lcom/mojang/blaze3d/vertex/VertexFormat;"
                    + "ILcom/mojang/blaze3d/vertex/VertexFormat$IndexType;IZ)V");

    /** GeckoLib 3 cube and quad bodies, skipped for cubes the GPU writes (their pose steps still run). */
    static final Target GEO_RENDER_CUBE = new Target("IGeoRenderer.renderCube", List.of("renderCube"),
            "(Lsoftware/bernie/geckolib3/geo/render/built/GeoCube;Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V");
    static final Target GEO_QUAD = new Target("IGeoRenderer.createVerticesOfQuad", List.of("createVerticesOfQuad"),
            "(Lsoftware/bernie/geckolib3/geo/render/built/GeoQuad;Lcom/mojang/math/Matrix4f;Lcom/mojang/math/Vector3f;"
                    + "Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V");

    /** Ars Nouveau's shaded GeckoLib 3 copy: the same two bodies under its own package. */
    static final Target ARS_GEO_RENDER_CUBE = new Target("Ars IGeoRenderer.renderCube", List.of("renderCube"),
            "(Lsoftware/bernie/ars_nouveau/geckolib3/geo/render/built/GeoCube;Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V");
    static final Target ARS_GEO_QUAD = new Target("Ars IGeoRenderer.createVerticesOfQuad", List.of("createVerticesOfQuad"),
            "(Lsoftware/bernie/ars_nouveau/geckolib3/geo/render/built/GeoQuad;Lcom/mojang/math/Matrix4f;Lcom/mojang/math/Vector3f;"
                    + "Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V");
    /** Citadel's private per-part vertex body, skipped for parts the GPU writes. */
    static final Target CITADEL_DO_RENDER = new Target("AdvancedModelBox.doRender", List.of("doRender"),
            "(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V");

    static final Target ITEM_QUAD_LIST = new Target("ItemRenderer.renderQuadList", List.of("renderQuadList", "m_115162_"),
            "(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Ljava/util/List;"
                    + "Lnet/minecraft/world/item/ItemStack;II)V");

    /** Called from the mixin plugin's postApply for VRO's GPU mixins. */
    public static void audit(String targetInternalName, ClassNode node, String vroMixin) {
        String result = switch (targetInternalName) {
            case MODEL_PART -> skippedBodiesUntouched(node, List.of(MODEL_PART_RENDER, MODEL_PART_COMPILE));
            case MODEL_PART_CUBE -> skippedBodiesUntouched(node, List.of(CUBE_COMPILE));
            case BUFFER_BUILDER -> hooksPresent(node, vroMixin,
                    List.of(BUILDER_POP, BUILDER_END, BUILDER_DISCARD, BUILDER_SORT, BUILDER_BEGIN));
            case BUFFER_UPLOADER -> hooksPresent(node, vroMixin, List.of(UPLOADER_END, UPLOADER_DRAW));
            case SPRITE_EXPANDER -> skippedBodiesUntouched(node, List.of(SPRITE_VERTEX, SPRITE_UV));
            case GEO_RENDERER -> skippedBodiesUntouched(node, List.of(GEO_RENDER_CUBE, GEO_QUAD));
            case ARS_GEO_RENDERER -> skippedBodiesUntouched(node, List.of(ARS_GEO_RENDER_CUBE, ARS_GEO_QUAD));
            case CITADEL_BOX -> skippedBodiesUntouched(node, List.of(CITADEL_DO_RENDER));
            case ITEM_RENDERER -> itemWriter(node, vroMixin);
            case MODEL_BLOCK_RENDERER -> blockModelWriter(node, vroMixin);
            case GEO_BLOCK_RENDERER -> {
                MethodNode method = find(node, GEO_BLOCK_RENDER_CUBE);
                yield method == null ? "GeoBlockRenderer.renderCube is missing"
                        : vroMixin.equals(mergedFrom(method)) ? null
                        : "GeoBlockRenderer.renderCube is not VRO's (" + mergedFrom(method) + ")";
            }
            default -> "unexpected audit target " + targetInternalName;
        };
        RESULTS.put(targetInternalName, result == null ? "" : result);
    }

    /**
     * The sprite wrapper path is separate: a problem there only keeps block entities on the CPU.
     *
     * @return null when {@code SpriteCoordinateExpander} was audited and untouched
     */
    public static String spriteBlocker() {
        String result = RESULTS.get(SPRITE_EXPANDER);
        if (result == null) return "SpriteCoordinateExpander was not audited";
        return result.isEmpty() ? null : result;
    }

    /**
     * GeckoLib cubes are separate too: a problem there only keeps GeckoLib models on the CPU.
     *
     * @return null when GeckoLib's {@code IGeoRenderer} was audited and untouched
     */
    public static boolean arsGeckoAudited() {
        return RESULTS.containsKey(ARS_GEO_RENDERER);
    }

    /** @return null when Ars Nouveau's shaded IGeoRenderer was audited and untouched */
    public static String arsGeckoBlocker() {
        String result = RESULTS.get(ARS_GEO_RENDERER);
        if (result == null) return "Ars Nouveau IGeoRenderer was not audited";
        return result.isEmpty() ? null : result;
    }

    public static boolean citadelAudited() {
        return RESULTS.containsKey(CITADEL_BOX);
    }

    /** @return null when Citadel's AdvancedModelBox was audited and its doRender is untouched */
    public static String citadelBlocker() {
        String result = RESULTS.get(CITADEL_BOX);
        if (result == null) return "AdvancedModelBox was not audited";
        return result.isEmpty() ? null : result;
    }

    /** @return null when GeoBlockRenderer carries VRO's renderCube and IGeoRenderer passed its audit */
    public static String geckoBlockBlocker() {
        String result = RESULTS.get(GEO_BLOCK_RENDERER);
        if (result == null) return "GeoBlockRenderer was not audited";
        return result.isEmpty() ? geckoBlocker() : result;
    }

    public static boolean geckoAudited() {
        return RESULTS.containsKey(GEO_RENDERER);
    }

    public static String geckoBlocker() {
        String result = RESULTS.get(GEO_RENDERER);
        if (result == null) return "IGeoRenderer was not audited";
        return result.isEmpty() ? null : result;
    }

    /** @return null when all four classes were audited and passed, otherwise the first problem */
    public static String blocker() {
        for (String target : List.of(MODEL_PART, MODEL_PART_CUBE, BUFFER_BUILDER, BUFFER_UPLOADER)) {
            String result = RESULTS.get(target);
            if (result == null) return target.substring(target.lastIndexOf('/') + 1) + " was not audited (its VRO mixin did not apply)";
            if (!result.isEmpty()) return result;
        }
        return null;
    }

    static String skippedBodiesUntouched(ClassNode node, List<Target> targets) {
        for (Target target : targets) {
            MethodNode method = find(node, target);
            if (method == null) return target.description() + " is missing";
            String owner = mergedFrom(method);
            if (owner != null && !allowedOverwrite(owner)) {
                return target.description() + " is overwritten by " + owner;
            }
            boolean vroSeen = false;
            for (AbstractInsnNode insn : method.instructions) {
                if (!(insn instanceof MethodInsnNode call) || !call.owner.equals(node.name)) continue;
                MethodNode callee = find(node, call.name, call.desc);
                String handler = callee == null ? null : mergedFrom(callee);
                if (handler == null || allowedOverwrite(handler)) continue;
                if (handler.startsWith(VRO_PREFIX)) {
                    vroSeen = true;
                } else if (target == MODEL_PART_RENDER && PRECEDING_RENDER_HOOKS.contains(handler)) {
                    if (vroSeen) return target.description() + " runs " + handler + " after VRO's hook";
                } else if (target == MODEL_PART_COMPILE && CONDITIONAL_COMPILE_OBSERVERS.contains(handler)) {
                    if (node.name.equals(MODEL_PART)) xaeroTraceHook = true;
                } else {
                    return target.description() + " is modified by " + handler;
                }
            }
        }
        return null;
    }

    /**
     * {@code renderQuadList} must carry VRO's hook, may be Embeddium's overwrite (EMBEDDIUM writer) or vanilla's
     * body (FORGE writer), and may only carry the reviewed foreign hooks before VRO's. Reports the writer to
     * {@link GpuItems}; a problem keeps items on the CPU only.
     */
    static String itemWriter(ClassNode node, String vroMixin) {
        MethodNode method = find(node, ITEM_QUAD_LIST);
        String problem = null;
        ItemWriter writer = ItemWriter.FORGE;
        if (method == null) {
            problem = ITEM_QUAD_LIST.description() + " is missing";
        } else {
            String owner = mergedFrom(method);
            if (owner != null) {
                if (ITEM_WRITER_OVERWRITES.stream().anyMatch(owner::startsWith)) writer = ItemWriter.EMBEDDIUM;
                else problem = ITEM_QUAD_LIST.description() + " is overwritten by " + owner;
            }
            boolean vroSeen = false;
            for (AbstractInsnNode insn : method.instructions) {
                if (problem != null) break;
                if (!(insn instanceof MethodInsnNode call) || !call.owner.equals(node.name)) continue;
                MethodNode callee = find(node, call.name, call.desc);
                String handler = callee == null ? null : mergedFrom(callee);
                if (handler == null || ITEM_WRITER_OVERWRITES.stream().anyMatch(handler::startsWith)) continue;
                if (handler.equals(vroMixin)) {
                    vroSeen = true;
                } else if (handler.startsWith(VRO_PREFIX)) {
                    continue;
                } else if (PRECEDING_ITEM_HOOKS.contains(handler)) {
                    if (vroSeen) problem = ITEM_QUAD_LIST.description() + " runs " + handler + " after VRO's hook";
                } else {
                    problem = ITEM_QUAD_LIST.description() + " is modified by " + handler;
                }
            }
            if (problem == null && !vroSeen) problem = ITEM_QUAD_LIST.description() + " does not contain VRO's GPU item hook";
        }
        // Recorded here, not in GpuItems: this runs while ItemRenderer is being transformed, and loading a class
        // that references ItemRenderer now would re-enter the transformer.
        itemWriter = problem == null ? writer : null;
        return problem;
    }

    /** Same rules as {@link #itemWriter} for {@code ModelBlockRenderer.renderModel}; no foreign hooks are allowed. */
    static String blockModelWriter(ClassNode node, String vroMixin) {
        MethodNode method = find(node, BLOCK_RENDER_MODEL);
        String problem = null;
        ItemWriter writer = ItemWriter.FORGE;
        if (method == null) {
            problem = BLOCK_RENDER_MODEL.description() + " is missing";
        } else {
            String owner = mergedFrom(method);
            if (owner != null) {
                if (BLOCK_MODEL_OVERWRITES.stream().anyMatch(owner::startsWith)) writer = ItemWriter.EMBEDDIUM;
                else problem = BLOCK_RENDER_MODEL.description() + " is overwritten by " + owner;
            }
            boolean vroSeen = false;
            for (AbstractInsnNode insn : method.instructions) {
                if (problem != null) break;
                if (!(insn instanceof MethodInsnNode call) || !call.owner.equals(node.name)) continue;
                MethodNode callee = find(node, call.name, call.desc);
                String handler = callee == null ? null : mergedFrom(callee);
                if (handler == null || BLOCK_MODEL_OVERWRITES.stream().anyMatch(handler::startsWith)) continue;
                if (handler.equals(vroMixin)) vroSeen = true;
                else if (!handler.startsWith(VRO_PREFIX)) problem = BLOCK_RENDER_MODEL.description() + " is modified by " + handler;
            }
            if (problem == null && !vroSeen) problem = BLOCK_RENDER_MODEL.description() + " does not contain VRO's GPU hook";
        }
        blockModelWriter = problem == null ? writer : null;
        return problem;
    }

    static String hooksPresent(ClassNode node, String vroMixin, List<Target> targets) {
        for (Target target : targets) {
            MethodNode method = find(node, target);
            if (method == null) return target.description() + " is missing";
            String owner = mergedFrom(method);
            if (owner != null) return target.description() + " is overwritten by " + owner;
            Set<String> handlers = new HashSet<>();
            for (AbstractInsnNode insn : method.instructions) {
                if (!(insn instanceof MethodInsnNode call) || !call.owner.equals(node.name)) continue;
                MethodNode callee = find(node, call.name, call.desc);
                if (callee != null && vroMixin.equals(mergedFrom(callee))) handlers.add(callee.name + callee.desc);
            }
            if (handlers.isEmpty()) return target.description() + " does not contain VRO's GPU entity hook";
            if (handlers.size() < target.hooks()) {
                return target.description() + " keeps " + handlers.size() + " of VRO's " + target.hooks()
                        + " GPU entity hooks";
            }
        }
        return null;
    }

    private static boolean allowedOverwrite(String mixin) {
        return RENDERER_COMPILE_OVERWRITES.stream().anyMatch(mixin::startsWith);
    }

    private static MethodNode find(ClassNode node, Target target) {
        for (MethodNode method : node.methods) {
            if (target.matches(method)) return method;
        }
        return null;
    }

    private static MethodNode find(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) return method;
        }
        return null;
    }

    /** The mixin class a method was merged from, or null for the target's own methods. */
    static String mergedFrom(MethodNode method) {
        String owner = mergedFrom(method.visibleAnnotations);
        return owner != null ? owner : mergedFrom(method.invisibleAnnotations);
    }

    private static String mergedFrom(List<AnnotationNode> annotations) {
        if (annotations == null) return null;
        for (AnnotationNode annotation : annotations) {
            if (!MIXIN_MERGED.equals(annotation.desc) || annotation.values == null) continue;
            for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                if ("mixin".equals(annotation.values.get(i))) return String.valueOf(annotation.values.get(i + 1));
            }
        }
        return null;
    }
}
