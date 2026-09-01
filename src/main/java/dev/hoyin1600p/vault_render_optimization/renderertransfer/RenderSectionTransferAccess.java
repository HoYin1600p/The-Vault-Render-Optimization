package dev.hoyin1600p.vault_render_optimization.renderertransfer;

public interface RenderSectionTransferAccess {
    boolean vro$hasEquivalentPendingRebuild(boolean important);

    boolean vro$hasActiveBuildTask();
}
