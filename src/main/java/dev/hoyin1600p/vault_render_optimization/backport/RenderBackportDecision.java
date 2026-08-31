package dev.hoyin1600p.vault_render_optimization.backport;

public record RenderBackportDecision(
        RenderBackportFeature feature,
        RenderBackportOwner owner,
        String reason
) {
}
