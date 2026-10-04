package dev.hoyin1600p.vault_render_optimization.config.spec;

import dev.hoyin1600p.vault_render_optimization.util.ConfigSections;
import net.minecraftforge.common.ForgeConfigSpec;

/** Defines the chunk update settings. Called once, in order, by {@code ClientOptimizationConfig}. */
public final class ChunkSpec {
    public final ForgeConfigSpec.BooleanValue deferChunkUpdates;
    public final ForgeConfigSpec.BooleanValue indexOnlySorting;
    public final ForgeConfigSpec.BooleanValue sortGeometryCache;
    public final ForgeConfigSpec.BooleanValue adaptiveChunkBudget;
    public final ForgeConfigSpec.BooleanValue farsightChunkBound;

    public ChunkSpec(ForgeConfigSpec.Builder builder) {
        builder.push(ConfigSections.CHUNK_UPDATES);
        this.deferChunkUpdates = builder.comment(
                "Use the renderer's existing asynchronous chunk-update path to reduce render-thread stalls.",
                "Default-on for vanilla Forge and validated Embeddium/Rubidium versions; no renderer mod is required.",
                "Visible block changes can be delayed under load. No render distance or world data is changed.",
                "Does not edit vanilla/renderer settings. Off and Compare Mode yield to those settings.",
                "The /vro chunks defer on|off command applies immediately to new scheduling decisions."
        ).define("defer_updates", true);
        this.indexOnlySorting = builder.comment(
                "Reuse unchanged terrain vertices during translucent sorting on validated Embeddium builds.",
                "Only drawing-order indices are uploaded. Vanilla and Rubidium 0.5.6 are unchanged.",
                "Off/Compare Mode affects new jobs; already queued index-only jobs finish safely.",
                "Use /vro chunks sorting on|off|status without a restart."
        ).define("index_only_sorting", true);
        this.sortGeometryCache = builder.comment(
                "Experimental: cache immutable triangle centers for validated Embeddium index-only sorting.",
                "Bounded to 16 MiB globally; preserves native stable distance order and generation checks.",
                "Defaults off pending in-game water/glass and shader comparisons."
        ).define("sort_geometry_cache", false);
        this.adaptiveChunkBudget = builder.comment(
                "Experimental v2: dynamically pace updates to already-built terrain using measured costs.",
                "Default-on on supported renderers; an explicit adaptive_budget_v2=false remains respected.",
                "Initial terrain and delayed/overloaded queues use native scheduling/draining automatically.",
                "Requires validated Embeddium and effective asynchronous chunk updates; vanilla/Rubidium unchanged.",
                "Uses conservative startup budgets, bounded feedback and queue backpressure; not an FPS guarantee.",
                "Off/Compare Mode restores native draining and scheduling; /vro chunks budget on|off|status is hot."
        ).define("adaptive_budget_v2", true);
        this.farsightChunkBound = builder.comment(
                "Only with Farsight installed: forget client chunks farther than max(server view distance,",
                "render distance) + 1 chunks, once per second, exactly as the forget packet Farsight cancels would",
                "(chunk drop, light release, Embeddium/Rubidium render sections). Chunks within render distance stay.",
                "This is a leak fix: Compare Mode does not affect it, and VH Accelerator leaves this job to VRO,",
                "so turning it off means nothing bounds Farsight's retained chunks until the level changes."
        ).define("farsight_chunk_bound", true);
        builder.pop();
    }
}
