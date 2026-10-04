package dev.hoyin1600p.vault_render_optimization.config.spec;

import dev.hoyin1600p.vault_render_optimization.util.ConfigSections;
import net.minecraftforge.common.ForgeConfigSpec;

/** Defines the Sophisticated Storage settings. Called once, in order, by {@code ClientOptimizationConfig}. */
public final class StorageSpec {
    public final ForgeConfigSpec.BooleanValue sophisticatedStorageFaceCulling;
    public final ForgeConfigSpec.BooleanValue sophisticatedStorageCountCache;
    public final ForgeConfigSpec.BooleanValue sophisticatedStorageFillFastPath;
    public final ForgeConfigSpec.BooleanValue sophisticatedStorageRenderUpdateFilter;
    public final ForgeConfigSpec.BooleanValue sophisticatedStorageDiagnostics;

    public StorageSpec(ForgeConfigSpec.Builder builder) {
        builder.push(ConfigSections.SOPHISTICATED_STORAGE);
        this.sophisticatedStorageFaceCulling = builder
                .comment(
                        "Skip barrel front-display rendering when its display plane faces away from the camera",
                        "or is completely covered by the immediately adjacent block. Visible displays are unchanged."
                )
                .define("front_display_culling", true);
        this.sophisticatedStorageCountCache = builder
                .comment(
                        "Cache limited-barrel count formatting and glyph layout until the displayed value changes.",
                        "The cache is bounded and cleared by resource reloads."
                )
                .define("quantity_text_cache", true);
        this.sophisticatedStorageFillFastPath = builder
                .comment(
                        "Render limited-barrel fill indicators without per-vertex temporary objects",
                        "while preserving the original fill values, texture, lighting, and transparency."
                )
                .define("fill_level_fast_path", true);
        this.sophisticatedStorageRenderUpdateFilter = builder
                .comment(
                        "Avoid rebuilding barrel chunk geometry for client updates that only change quantities",
                        "or fill levels. Model-affecting display changes still rebuild immediately."
                )
                .define("count_only_render_update_filter", true);
        this.sophisticatedStorageDiagnostics = builder
                .comment(
                        "Collect Sophisticated Storage render and cache counters for /vro storage.",
                        "Disabled by default to keep the normal render path as small as possible."
                )
                .define("diagnostics", false);
        builder.pop();
    }
}
