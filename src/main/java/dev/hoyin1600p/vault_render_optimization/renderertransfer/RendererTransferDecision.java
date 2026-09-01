package dev.hoyin1600p.vault_render_optimization.renderertransfer;

public record RendererTransferDecision(
        RendererTransferFeature feature,
        RendererTransferStatus status,
        String reason
) {
}
