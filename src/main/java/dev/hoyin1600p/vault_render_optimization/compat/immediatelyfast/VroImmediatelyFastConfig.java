/*
 * Based on ImmediatelyFast Reforged's ImmediatelyFastConfig (LGPL-3.0-or-later),
 * Copyright (C) 2023 RK_01/RaphiMC and contributors. Modified by HoYin1600p for VRO, 2026:
 * adds the "enabled" switch; the remaining option names and defaults are unchanged.
 */
package dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast;

/** Read once at startup (mixin selection needs it before Forge configs load). */
@SuppressWarnings("unused")
public class VroImmediatelyFastConfig {
    private final String INFO = "VRO's built-in ImmediatelyFast. It is skipped entirely when the standalone "
            + "ImmediatelyFast mod is installed. Changes apply after a restart.";
    public boolean enabled = true;

    private final String REGULAR_INFO = "----- Regular config values below -----";
    public boolean font_atlas_resizing = true;
    public boolean map_atlas_generation = true;
    public boolean hud_batching = true;
    public boolean fast_text_lookup = true;
    public boolean fast_buffer_upload = true;

    private final String COSMETIC_INFO = "----- Cosmetic only config values below (Does not optimize anything) -----";
    public boolean dont_add_info_into_debug_hud = false;

    private final String EXPERIMENTAL_INFO = "----- Experimental config values below (Rendering glitches may occur) -----";
    public boolean experimental_item_hud_batching = false;

    private final String DEBUG_INFO = "----- Debug only config values below (Do not touch) -----";
    public boolean debug_only_and_not_recommended_disable_universal_batching = false;
    public boolean debug_only_and_not_recommended_disable_mod_conflict_handling = false;
}
