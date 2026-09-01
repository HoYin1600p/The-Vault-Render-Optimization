package dev.hoyin1600p.vault_render_optimization.renderertransfer;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class RendererTransferOwnershipRegistry {
    private static volatile RendererFamily rendererFamily = RendererFamily.NONE;
    private static volatile Map<RendererTransferFeature, RendererTransferDecision> decisions =
            unavailable("mixin selection has not completed");

    private RendererTransferOwnershipRegistry() {
    }

    public static synchronized void initialize(
            boolean physicalClient,
            boolean compareMode,
            Predicate<RendererTransferFeature> configured,
            RendererFamily family,
            String version,
            Function<RendererTransferFeature, String> compatibilityProbe
    ) {
        rendererFamily = family;
        EnumMap<RendererTransferFeature, RendererTransferDecision> resolved =
                new EnumMap<>(RendererTransferFeature.class);
        for (RendererTransferFeature feature : RendererTransferFeature.values()) {
            resolved.put(feature, RendererTransferOwnershipResolver.resolve(
                    feature, physicalClient, compareMode, configured.test(feature), family, version,
                    compatibilityProbe.apply(feature)
            ));
        }
        decisions = Map.copyOf(resolved);
    }

    public static boolean applies(RendererTransferFeature feature, String mixinClassName) {
        String simpleName = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        return decisions.get(feature).status() == RendererTransferStatus.APPLIED
                && feature.mixinMatchesFamily(simpleName, rendererFamily);
    }

    public static RendererTransferDecision decision(RendererTransferFeature feature) {
        return decisions.get(feature);
    }

    public static List<String> reportLines() {
        return Arrays.stream(RendererTransferFeature.values())
                .map(feature -> {
                    RendererTransferDecision decision = decisions.get(feature);
                    return feature.id() + "=" + decision.status() + " - " + decision.reason();
                }).toList();
    }

    public static String summary() {
        return decisions.values().stream()
                .collect(Collectors.groupingBy(
                        RendererTransferDecision::status,
                        () -> new EnumMap<>(RendererTransferStatus.class),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    private static Map<RendererTransferFeature, RendererTransferDecision> unavailable(String reason) {
        EnumMap<RendererTransferFeature, RendererTransferDecision> unavailable =
                new EnumMap<>(RendererTransferFeature.class);
        for (RendererTransferFeature feature : RendererTransferFeature.values()) {
            unavailable.put(feature, new RendererTransferDecision(feature, RendererTransferStatus.BLOCKED, reason));
        }
        return Map.copyOf(unavailable);
    }
}
