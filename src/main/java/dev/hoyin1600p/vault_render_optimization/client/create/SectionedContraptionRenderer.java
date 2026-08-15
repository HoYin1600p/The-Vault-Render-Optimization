package dev.hoyin1600p.vault_render_optimization.client.create;

import com.jozufozu.flywheel.backend.model.ArrayModelRenderer;
import com.jozufozu.flywheel.core.model.Model;
import com.jozufozu.flywheel.core.model.ShadeSeparatedBufferedData;
import com.jozufozu.flywheel.core.model.WorldModelBuilder;
import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.contraptions.render.ContraptionProgram;
import com.simibubi.create.content.contraptions.render.ContraptionRenderInfo;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class SectionedContraptionRenderer {
    private static final int SECTION_SIZE = 16;
    private static final Map<Contraption, FallbackData> FALLBACK_MESHES = new IdentityHashMap<>();

    private SectionedContraptionRenderer() {
    }

    public static boolean shouldSection(Contraption contraption) {
        return ClientOptimizationConfig.optimizationsEnabled()
                && ClientOptimizationConfig.createSectionedContraptionMeshes
                && contraption.getRenderedBlocks().size() >= ClientOptimizationConfig.createSectionedMeshThreshold;
    }

    public static boolean renderFallback(ContraptionRenderInfo renderInfo,
                                         RenderType layer,
                                         VertexConsumer consumer) {
        Contraption contraption = renderInfo.contraption;
        if (!shouldSection(contraption)) {
            return false;
        }
        if (!renderInfo.isVisible()) {
            return true;
        }

        FallbackData data = FALLBACK_MESHES.computeIfAbsent(
                contraption,
                ignored -> new FallbackData(contraption, renderInfo.renderWorld)
        );
        data.render(layer, renderInfo.getMatrices(), consumer);
        return true;
    }

    public static void invalidateFallback(Contraption contraption) {
        FALLBACK_MESHES.remove(contraption);
    }

    public static void clearFallbackMeshes() {
        FALLBACK_MESHES.clear();
    }

    public static FlywheelData buildFlywheel(Contraption contraption, VirtualRenderWorld renderWorld) {
        return new FlywheelData(contraption, renderWorld);
    }

    private static List<Group> groupBlocks(Collection<StructureTemplate.StructureBlockInfo> blocks) {
        Map<SectionKey, GroupBuilder> builders = new HashMap<>();
        for (StructureTemplate.StructureBlockInfo block : blocks) {
            BlockPos pos = block.pos;
            SectionKey key = new SectionKey(
                    Math.floorDiv(pos.getX(), SECTION_SIZE),
                    Math.floorDiv(pos.getY(), SECTION_SIZE),
                    Math.floorDiv(pos.getZ(), SECTION_SIZE)
            );
            builders.computeIfAbsent(key, ignored -> new GroupBuilder()).add(block);
        }

        List<Group> groups = new ArrayList<>(builders.size());
        for (Map.Entry<SectionKey, GroupBuilder> entry : builders.entrySet()) {
            groups.add(entry.getValue().build(entry.getKey()));
        }
        return groups;
    }

    private static boolean sectionVisible(AABB bounds, ContraptionMatrices matrices) {
        boolean visible = CreateRenderContext.isVisible(bounds, matrices.getLight());
        CreateRenderContext.recordSection(!visible);
        return visible;
    }

    public static final class FlywheelData {
        private final Map<RenderType, List<FlywheelSection>> layers = new HashMap<>();

        private FlywheelData(Contraption contraption, VirtualRenderWorld renderWorld) {
            List<Group> groups = groupBlocks(contraption.getRenderedBlocks());
            for (RenderType layer : RenderType.chunkBufferLayers()) {
                List<FlywheelSection> sections = new ArrayList<>();
                for (Group group : groups) {
                    Model model = new WorldModelBuilder(layer)
                            .withRenderWorld(renderWorld)
                            .withModelData(contraption.modelData)
                            .withBlocks(group.blocks())
                            .toModel("vro_contraption_" + contraption.entity.getId() + "_" + group.key() + "_" + layer);
                    if (model.empty()) {
                        model.delete();
                        continue;
                    }
                    sections.add(new FlywheelSection(group.bounds(), model, new ArrayModelRenderer(model)));
                }
                if (!sections.isEmpty()) {
                    layers.put(layer, sections);
                }
            }
        }

        public void render(RenderType layer, ContraptionMatrices matrices, ContraptionProgram shader) {
            List<FlywheelSection> sections = layers.get(layer);
            if (sections == null || sections.isEmpty()) {
                return;
            }
            for (FlywheelSection section : sections) {
                if (sectionVisible(section.bounds(), matrices)) {
                    section.renderer().draw();
                }
            }
        }

        public void delete() {
            for (List<FlywheelSection> sections : layers.values()) {
                for (FlywheelSection section : sections) {
                    section.renderer().delete();
                    section.model().delete();
                }
            }
            layers.clear();
        }
    }

    private static final class FallbackData {
        private final Contraption contraption;
        private final VirtualRenderWorld renderWorld;
        private final List<Group> groups;
        private final Map<RenderType, List<FallbackSection>> layers = new HashMap<>();

        private FallbackData(Contraption contraption, VirtualRenderWorld renderWorld) {
            this.contraption = contraption;
            this.renderWorld = renderWorld;
            this.groups = groupBlocks(contraption.getRenderedBlocks());
        }

        private void render(RenderType layer, ContraptionMatrices matrices, VertexConsumer consumer) {
            List<FallbackSection> sections = layers.computeIfAbsent(layer, this::buildLayer);
            for (FallbackSection section : sections) {
                if (!sectionVisible(section.bounds(), matrices)) {
                    continue;
                }
                section.buffer()
                        .transform(matrices.getModel())
                        .light(matrices.getWorld())
                        .hybridLight()
                        .renderInto(matrices.getViewProjection(), consumer);
            }
        }

        private List<FallbackSection> buildLayer(RenderType layer) {
            List<FallbackSection> sections = new ArrayList<>();
            for (Group group : groups) {
                ShadeSeparatedBufferedData raw = new WorldModelBuilder(layer)
                        .withRenderWorld(renderWorld)
                        .withModelData(contraption.modelData)
                        .withBlocks(group.blocks())
                        .build();
                SuperByteBuffer buffer = new SuperByteBuffer(raw);
                raw.release();
                if (!buffer.isEmpty()) {
                    sections.add(new FallbackSection(group.bounds(), buffer));
                }
            }
            return sections;
        }
    }

    private static final class GroupBuilder {
        private final List<StructureTemplate.StructureBlockInfo> blocks = new ArrayList<>();
        private int minX = Integer.MAX_VALUE;
        private int minY = Integer.MAX_VALUE;
        private int minZ = Integer.MAX_VALUE;
        private int maxX = Integer.MIN_VALUE;
        private int maxY = Integer.MIN_VALUE;
        private int maxZ = Integer.MIN_VALUE;

        private void add(StructureTemplate.StructureBlockInfo block) {
            blocks.add(block);
            BlockPos pos = block.pos;
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX() + 1);
            maxY = Math.max(maxY, pos.getY() + 1);
            maxZ = Math.max(maxZ, pos.getZ() + 1);
        }

        private Group build(SectionKey key) {
            return new Group(key, List.copyOf(blocks), new AABB(minX, minY, minZ, maxX, maxY, maxZ));
        }
    }

    private record SectionKey(int x, int y, int z) {
    }

    private record Group(SectionKey key, List<StructureTemplate.StructureBlockInfo> blocks, AABB bounds) {
    }

    private record FlywheelSection(AABB bounds, Model model, ArrayModelRenderer renderer) {
    }

    private record FallbackSection(AABB bounds, SuperByteBuffer buffer) {
    }
}
