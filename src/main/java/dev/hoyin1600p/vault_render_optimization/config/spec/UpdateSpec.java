package dev.hoyin1600p.vault_render_optimization.config.spec;

import dev.hoyin1600p.vault_render_optimization.config.model.UpdateNoticeFilter;
import dev.hoyin1600p.vault_render_optimization.util.ConfigSections;
import net.minecraftforge.common.ForgeConfigSpec;

/** Defines the update-check settings. Called once, in order, by {@code ClientOptimizationConfig}. */
public final class UpdateSpec {
    public final ForgeConfigSpec.BooleanValue updateChecks;
    public final ForgeConfigSpec.EnumValue<UpdateNoticeFilter> updateNoticeFilter;

    public UpdateSpec(ForgeConfigSpec.Builder builder) {
        builder.push(ConfigSections.UPDATES);
        this.updateChecks = builder
                .comment(
                        "Check VRO's raw GitHub update manifest asynchronously.",
                        "The /vro updates command changes and saves this setting."
                )
                .define("check_for_updates", true);
        this.updateNoticeFilter = builder
                .comment(
                        "Choose which update types VRO may show: CRITICAL or ALL.",
                        "CRITICAL is the safe default; ALL also permits normal update notices."
                )
                .defineEnum("update_types", UpdateNoticeFilter.CRITICAL);
        builder.pop();
    }
}
