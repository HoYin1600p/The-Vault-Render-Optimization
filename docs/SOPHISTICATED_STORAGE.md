# Sophisticated Storage rendering

VRO optimizes front-display work for the exact Sophisticated Storage
`1.18.2-0.9.8.915` and Sophisticated Core `1.18.2-0.6.4.604` pair used by the
Remastered pack. Other versions fail closed because these hooks target private
renderer and synchronization layouts.

Tier badges remain untouched. They are part of the baked barrel model and need
a separate design and compatibility pass.

## Default-on paths

### Front-display visibility

VRO skips the block-entity-rendered item, quantity, fill, and upgrade layers
when the display face is behind the camera plane or an immediately adjacent
block fully covers it. The camera-inside-block case remains visible. This is a
narrow face test, not a ray-trace occlusion system; EntityCulling can continue
to own general block-entity occlusion.

Hidden tier and lock previews are outside this hook and retain upstream
behavior.

### Quantity labels

Limited-barrel quantity strings, styled glyph sequences, and measured widths
are cached by exact count and the original five/six-character layout. The
primitive-keyed least-recently-used cache is limited to 4,096 entries and is
cleared on resource reload. Slot color, placement, scale, lighting, and
abbreviation behavior are unchanged.

On Astra-Dev, each label also retains local glyph vertices and consecutive font
atlas runs, capped at 128 vertices per label. Color, light and pose are supplied
fresh on every draw through the original buffer consumer. Unknown glyph attributes
or oversized output fall back to the normal font renderer. Font replacement and
resource reload clear the cache. This avoids repeated glyph layout/emission setup;
it does not claim to eliminate already-batched GPU draws. The tier display is unchanged.

### Fill bars

VRO preserves Sophisticated Storage's texture, UVs, slot layout, transparency,
lighting, and fill proportions while emitting transformed vertices directly.
This removes four temporary vector allocations for every visible bar in every
frame.

### Display-only update filtering

The validated 1.18.2 synchronization path can request a terrain rebuild for a
barrel even when only its block-entity-rendered quantity or fill changed. VRO
defers that decision until wood and material fields have loaded, then compares
an exact immutable snapshot of every model-bearing field. A first update or a
real model change still notifies Minecraft. Stack size is excluded; item/NBT,
display arrangement, inaccessible slots, materials, colors, wood, packing,
lock/tier state, block state, and dynamic-render ownership remain triggers.

The comparison does not use a hash, so a collision cannot hide a real update.

## Controls and diagnostics

All performance paths default on and apply immediately:

- `/vro storage face on|off`
- `/vro storage counts on|off`
- `/vro storage fill on|off`
- `/vro storage updates on|off`

`/vro storage` reports their current state. Compare Mode disables all four
without changing saved values.

Diagnostics default off. `/vro storage diagnostics on` collects front faces
rendered, backfaces/covered faces skipped, count labels, cache hits/misses,
fill bars, required model rebuilds, and display-only rebuilds skipped. Use
`/vro storage diagnostics reset` before a comparison and turn diagnostics off
for clean FPS measurement.

## Validation checklist

Automated validation covers the exact version pair, fail-closed version gates,
camera-plane math, quantity cache keys, deferred post-load notification point,
model-bearing snapshot contents, count exclusion, and allocation-light fill
source structure.

Manual in-game validation should compare enabled versus Compare Mode while:

1. viewing one-, two-, three-, and four-slot limited barrels from the front;
2. changing quantities and watching labels/fill bars update continuously;
3. changing displayed item, material, color, lock visibility, and tier
   visibility, confirming every real model change appears immediately;
4. viewing ordinary and limited barrels from behind and from the side;
5. covering/uncovering the display face with full and partial blocks;
6. holding Sophisticated Storage display tools so hidden previews still work;
7. inspecting upgrade icons and dynamic item models with shaders on and off;
8. running a hopper/controller-driven quantity churn near a barrel wall and
   comparing `/vro storage diagnostics` plus frame-time/allocation profiles.

## Provenance

The installed renderer behavior was inspected at Sophisticated Storage
revision `891b0d9e29350ec78c7b8567285036d2ecdb303c`. Later revision
`93656abf01687429c4a5f4407e8f8ff10f5fc29e` informed the display-only update
separation. Sophisticated Storage is GPL-3.0-only. VRO's adaptations and new
policy remain distributed as part of the AGPL-3.0-or-later project under the
GPLv3 section 13 compatibility path. Full notices are in `CREDITS.md` and
`THIRD_PARTY_NOTICES.md`.
