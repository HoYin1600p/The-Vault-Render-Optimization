package dev.hoyin1600p.vault_render_optimization.config.spec;

import dev.hoyin1600p.vault_render_optimization.renderertransfer.RendererTransferFeature;
import dev.hoyin1600p.vault_render_optimization.util.ConfigKeys;
import dev.hoyin1600p.vault_render_optimization.util.ConfigSections;
import java.util.EnumMap;
import net.minecraftforge.common.ForgeConfigSpec;

/** Defines the renderer transfer toggles, vertex buffer limits and async arena growth. Called once, in order, by {@code ClientOptimizationConfig}. */
public final class RendererSpec {
    public final ForgeConfigSpec.IntValue vertexBufferMaxRetainedMib;
    public final ForgeConfigSpec.BooleanValue vertexBufferAdaptiveTrimming;
    public final ForgeConfigSpec.IntValue vertexBufferAggregateRetainedMib;
    public final ForgeConfigSpec.IntValue asyncArenaGrowthDivisor;
    public final ForgeConfigSpec.IntValue asyncArenaMaxHeadroomMib;

    public RendererSpec(ForgeConfigSpec.Builder builder, EnumMap<RendererTransferFeature, ForgeConfigSpec.BooleanValue> options) {
        builder.push(ConfigSections.EMBEDDIUM_TRANSFERS);
        for (RendererTransferFeature feature : RendererTransferFeature.values()) {
            options.put(
                    feature,
                    builder.comment(
                            "Enable " + feature.id() + ": " + feature.displayName() + ".",
                            "This startup option requires a client restart.",
                            feature.activeInCompareMode()
                                    ? "This correctness guard remains active in Compare Mode."
                                    : "Compare Mode yields this performance or compatibility-sensitive path."
                    ).define(feature.configKey(), true)
            );
        }
        this.vertexBufferMaxRetainedMib = builder.comment(
                "Maximum native capacity retained by one renderer vertex buffer between builds.",
                "Larger one-off buffers are trimmed at the next start; destroy still frees them deterministically."
        ).defineInRange("vertexBufferMaxRetainedMib", 16, 1, 256);
        this.vertexBufferAdaptiveTrimming = builder.comment(
                "Experimental: trim unused CPU-native vertex capacity at safe worker start points.",
                "Keeps a 30-second demand window; idle workers trim only when used again, never remotely.",
                "Existing per-buffer ceiling and native destroy remain unchanged. Off pending validation."
        ).define("vertexBufferAdaptiveTrimming", false);
        this.vertexBufferAggregateRetainedMib = builder.comment(
                "Aggregate excess-capacity pressure threshold, not a hard process/native/VRAM limit.",
                "Above this, active workers release old peaks at their next safe start; required writes are never capped."
        ).defineInRange("vertexBufferAggregateRetainedMib", 128, 16, 2048);
        this.asyncArenaGrowthDivisor = builder.comment(
                "Reserve roughly current arena capacity divided by this value during a resize.",
                "Smaller values trade more speculative VRAM for fewer resize/compaction events."
        ).defineInRange(ConfigKeys.ASYNC_ARENA_GROWTH_DIVISOR, 6, 2, 64);
        this.asyncArenaMaxHeadroomMib = builder.comment(
                "Maximum speculative VRAM headroom added by one arena growth.",
                "Memory required by the actual upload is never capped by this value."
        ).defineInRange(ConfigKeys.ASYNC_ARENA_MAX_HEADROOM_MIB, 64, 1, 512);
        builder.pop();
    }
}
