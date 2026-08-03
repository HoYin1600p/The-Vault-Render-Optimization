package dev.hoyin1600p.vault_render_optimization.client.memory;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModelFaceCompactor {
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Map<Direction, List<BakedQuad>> EMPTY_CULLED_FACES = createEmptyCulledFaces();

    private ModelFaceCompactor() {
    }

    public static List<BakedQuad> compactUnculled(List<BakedQuad> quads) {
        return List.copyOf(quads);
    }

    public static Map<Direction, List<BakedQuad>> compactCulled(
            Map<Direction, List<BakedQuad>> source) {
        EnumMap<Direction, List<BakedQuad>> compacted = new EnumMap<>(Direction.class);
        boolean allEmpty = true;

        for (Direction direction : DIRECTIONS) {
            List<BakedQuad> quads = List.copyOf(source.getOrDefault(direction, List.of()));
            compacted.put(direction, quads);
            allEmpty &= quads.isEmpty();
        }

        return allEmpty ? EMPTY_CULLED_FACES : compacted;
    }

    private static Map<Direction, List<BakedQuad>> createEmptyCulledFaces() {
        EnumMap<Direction, List<BakedQuad>> empty = new EnumMap<>(Direction.class);
        for (Direction direction : DIRECTIONS) {
            empty.put(direction, List.of());
        }
        return Collections.unmodifiableMap(empty);
    }
}
