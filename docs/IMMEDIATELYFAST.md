# Built-in ImmediatelyFast

VRO includes a relocated copy of ImmediatelyFast Reforged 1.18.2 (1.1.10, LGPL-3.0-or-later),
the same code the Wolds Vaults packs already ship as a separate mod. It reduces draw calls and
per-frame work for immediate-mode rendering:

- universal batching of immediate-mode draws by render type;
- HUD batching (text, fills, item icons and overlays drawn in fewer batches);
- fast text lookup and font atlas resizing;
- map atlas generation (many maps share one texture);
- buffer-upload reuse for vertex buffers.

## When it is active

It is on by default and applies only when all of these hold:

1. `enabled` is `true` in `config/vault_render_optimization-immediatelyfast.json`.
2. The standalone `immediatelyfast` mod is **not** installed. If it is, the standalone mod owns
   these optimizations and VRO's copy applies no mixins at all.
3. With Oculus installed, the Oculus members its shader compatibility uses are present (checked
   from the Oculus jar's bytes before any mixin applies). Public Oculus 1.6.4 and the dh-compat
   builds pass. If they are missing, the built-in copy stays off instead of risking wrong vertex
   formats; the original exited the game in that case.

Whenever ImmediatelyFast (standalone or built in) owns HUD batching or text lookup, VRO's own
overlapping text path (HUD text geometry reuse) steps aside.

## Configuration

The JSON file keeps ImmediatelyFast's own option names and defaults: `font_atlas_resizing`,
`map_atlas_generation`, `hud_batching`, `fast_text_lookup`, `fast_buffer_upload`,
`dont_add_info_into_debug_hud`, `experimental_item_hud_batching`, and the two debug-only options.
It is read once at startup, so changes need a restart. The startup log reports
`Built-in ImmediatelyFast: ACTIVE` or `OFF` with the reason.

## Testing

Compare frame time in a HUD- and loot-heavy scene with `enabled` true and false (restart between).
In a pack that already ships the standalone mod, remove the standalone jar to test VRO's copy.
