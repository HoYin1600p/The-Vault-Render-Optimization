package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

/**
 * Which vanilla-format item quad writer produced (and therefore must be reproduced for) an item's vertices:
 * Forge's {@code IForgeVertexConsumer.putBulkData}, or Embeddium's overwrite of
 * {@code ItemRenderer.renderQuadList}. They differ only in the normal input floats and the baked-light rule.
 */
public enum ItemWriter {
    FORGE,
    EMBEDDIUM
}
