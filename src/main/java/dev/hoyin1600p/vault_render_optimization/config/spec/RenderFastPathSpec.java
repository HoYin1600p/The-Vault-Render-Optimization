package dev.hoyin1600p.vault_render_optimization.config.spec;

import dev.hoyin1600p.vault_render_optimization.config.model.ParticleBillboardOwner;
import dev.hoyin1600p.vault_render_optimization.util.ConfigKeys;
import net.minecraftforge.common.ForgeConfigSpec;

/** Defines the HUD, particle and frustum fast paths (defined inside the render_fast_paths section). Called once, in order, by {@code ClientOptimizationConfig}. */
public final class RenderFastPathSpec {
    public final ForgeConfigSpec.BooleanValue hudTextGeometry;
    public final ForgeConfigSpec.BooleanValue particleCollisionCache;
    public final ForgeConfigSpec.BooleanValue particleLightCache;
    public final ForgeConfigSpec.BooleanValue particleSharedLightCache;
    public final ForgeConfigSpec.BooleanValue particleBillboardFastPath;
    public final ForgeConfigSpec.EnumValue<ParticleBillboardOwner> particleBillboardOwner;
    public final ForgeConfigSpec.BooleanValue particleDiagnostics;
    public final ForgeConfigSpec.BooleanValue emptyParticleRenderSkip;
    public final ForgeConfigSpec.BooleanValue allocationFreeFrustum;
    public final ForgeConfigSpec.BooleanValue fastloadFrustumBypass;
    public final ForgeConfigSpec.BooleanValue particleTickCompaction;
    public final ForgeConfigSpec.BooleanValue particleSharedRandom;
    public final ForgeConfigSpec.BooleanValue particleProviderCache;

    public RenderFastPathSpec(ForgeConfigSpec.Builder builder) {
        this.hudTextGeometry = builder.comment(
                "Experimental: reuse unchanged plain HUD text glyph geometry while drawing every frame.",
                "Formatted/animated/bidirectional/custom font paths fall back; no gear data or HUD state is cached.",
                "Defaults off pending visual validation. Compare Mode disables reuse."
        ).define(ConfigKeys.HUD_TEXT_GEOMETRY, false);
        this.particleCollisionCache = builder.comment(
                "Exact particle block collision with a per-tick cache: visits the same cells and shapes as",
                "vanilla and uses vanilla's collision response, but reads each block once per particle tick",
                "instead of once per particle. Every particle still collides; nothing is skipped.",
                "Yields to Particle Core/Flerovium. /vro particles collision on|off|verify toggles at runtime."
        ).define(ConfigKeys.PARTICLE_COLLISION_CACHE, true);
        this.particleLightCache = builder
                .comment("Cache unchanged particle light lookups for one client tick.")
                .define("particle_light_cache", true);
        this.particleSharedLightCache = builder
                .comment(
                        "Share light results between particles occupying the same block during one client tick.",
                        "This remains bounded and is cleared when the client level or game tick changes."
                )
                .define("particle_shared_light_cache", true);
        this.particleBillboardFastPath = builder
                .comment(
                        "Build ordinary particle billboards from the camera's left/up basis instead of rotating four corners.",
                        "Particles with custom render methods are unchanged. This option can be changed while the game is running."
                )
                .define("particle_billboard_fast_path", true);
        this.particleBillboardOwner = builder
                .comment(
                        "Select who renders ordinary particle billboards: AUTO, RENDERER, or VRO.",
                        "AUTO selects VRO's geometry and uses an installed renderer's packed writer.",
                        "RENDERER yields to Rubidium/Embeddium when available; VRO forces VRO's compatible path."
                )
                .defineEnum("particle_billboard_owner", ParticleBillboardOwner.AUTO);
        this.particleDiagnostics = builder
                .comment(
                        "Collect particle queue, render/tick timing, writer, and light-cache counters.",
                        "Disabled by default because class-level diagnostics add measurement overhead."
                )
                .define("particle_diagnostics", false);
        this.emptyParticleRenderSkip = builder
                .comment("Skip particle renderer setup when every particle queue is empty.")
                .define("skip_empty_particle_render", true);
        this.allocationFreeFrustum = builder.comment(
                "Test each frustum plane against the box corner farthest along its normal instead of allocating eight",
                "vectors per plane. Same float operations in the same order, so the answer is vanilla's exactly."
        ).define(ConfigKeys.ALLOCATION_FREE_FRUSTUM, true);
        this.fastloadFrustumBypass = builder.comment(
                "With Fastload installed: while its frustum event has no listeners (all gameplay after world load),",
                "visibility checks skip its synchronized per-check event lookup. Same answers; only applies when",
                "Fastload's hook is the only change to that frustum method."
        ).define(ConfigKeys.FASTLOAD_FRUSTUM_BYPASS, true);
        this.particleTickCompaction = builder.comment(
                "Remove particles that died this tick in one ordered pass instead of one queue shift each.",
                "Every particle still ticks in vanilla order; only the removal of dead ones is batched."
        ).define(ConfigKeys.PARTICLE_TICK_COMPACTION, true);
        this.particleSharedRandom = builder.comment(
                "Give particles a per-thread generator with java.util.Random's exact algorithm instead of a new",
                "Random each (a CAS, nanoTime and AtomicLong per particle, and a CAS per draw). Same distribution."
        ).define(ConfigKeys.PARTICLE_SHARED_RANDOM, true);
        this.particleProviderCache = builder.comment(
                "Resolve each particle type's provider once instead of a registry key plus hash lookup per spawn.",
                "Invalidated whenever a provider is registered. Returns exactly the provider vanilla would."
        ).define(ConfigKeys.PARTICLE_PROVIDER_CACHE, true);
    }
}
