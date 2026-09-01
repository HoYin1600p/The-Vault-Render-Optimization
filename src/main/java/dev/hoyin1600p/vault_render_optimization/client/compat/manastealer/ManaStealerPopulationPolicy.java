package dev.hoyin1600p.vault_render_optimization.client.compat.manastealer;

public final class ManaStealerPopulationPolicy {
    public static final int ALL = 0;
    public static final int DECREASED = 1;
    public static final int MINIMAL = 2;

    private ManaStealerPopulationPolicy() {
    }

    public static int target(int quality, int all, int decreased, int minimal) {
        return switch (quality) {
            case DECREASED -> Math.max(0, decreased);
            case MINIMAL -> Math.max(0, minimal);
            default -> Math.max(0, all);
        };
    }
}
