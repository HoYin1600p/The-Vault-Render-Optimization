package dev.hoyin1600p.vault_render_optimization.backport;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class RenderBackportOwnershipRegistry {
    private static volatile Map<RenderBackportFeature, RenderBackportDecision> decisions =
            unavailableSnapshot("mixin selection has not completed");

    private RenderBackportOwnershipRegistry() {
    }

    public static synchronized void initialize(
            boolean physicalClient,
            boolean compareMode,
            Predicate<RenderBackportFeature> configuredProbe,
            Predicate<RenderBackportFeature> vhAcceleratorProbe,
            Function<RenderBackportFeature, ModernFixOwnership> modernFixProbe,
            Function<RenderBackportFeature, String> compatibilityProbe
    ) {
        EnumMap<RenderBackportFeature, RenderBackportDecision> resolved =
                new EnumMap<>(RenderBackportFeature.class);
        for (RenderBackportFeature feature : RenderBackportFeature.values()) {
            resolved.put(
                    feature,
                    RenderBackportOwnershipResolver.resolve(
                            feature,
                            physicalClient,
                            compareMode,
                            configuredProbe.test(feature),
                            vhAcceleratorProbe.test(feature),
                            modernFixProbe.apply(feature),
                            compatibilityProbe.apply(feature)
                    )
            );
        }
        decisions = Map.copyOf(resolved);
    }

    public static RenderBackportDecision decision(RenderBackportFeature feature) {
        return decisions.get(feature);
    }

    public static boolean vroOwns(RenderBackportFeature feature) {
        return decision(feature).owner() == RenderBackportOwner.VRO;
    }

    public static List<String> reportLines() {
        return Arrays.stream(RenderBackportFeature.values())
                .map(feature -> {
                    RenderBackportDecision decision = decision(feature);
                    return feature.id() + "=" + decision.owner().name() + " - " + decision.reason();
                })
                .toList();
    }

    public static String summary() {
        return decisions.values().stream()
                .collect(Collectors.groupingBy(
                        RenderBackportDecision::owner,
                        () -> new EnumMap<>(RenderBackportOwner.class),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(entry -> entry.getKey().name() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    private static Map<RenderBackportFeature, RenderBackportDecision> unavailableSnapshot(String reason) {
        EnumMap<RenderBackportFeature, RenderBackportDecision> unavailable =
                new EnumMap<>(RenderBackportFeature.class);
        for (RenderBackportFeature feature : RenderBackportFeature.values()) {
            unavailable.put(
                    feature,
                    new RenderBackportDecision(feature, RenderBackportOwner.UNAVAILABLE, reason)
            );
        }
        return Map.copyOf(unavailable);
    }
}
