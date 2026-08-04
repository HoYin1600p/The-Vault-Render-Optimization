# Dynamic lights

VRO contains an optional client-only dynamic-light engine. It is disabled
by default. Enable it with `/vro lights on` or
`dynamic_lights.enabled = true` in the client configuration.

## Supported sources

- held or equipped luminous items on living entities;
- dropped luminous items and items displayed in item frames;
- burning entities, primed TNT, hostile fire projectiles, fireworks, blazes,
  magma cubes, spectral arrows, glow item frames, and swelling creepers;
- luminous `BlockItem` stacks using their default block state's light value;
- resource-defined item and block entity types.

Dynamic light affects rendering only. It does not alter the world's saved light
data, server simulation, spawning, crop growth, or Distant Horizons storage.

## Engine behavior

Each source is stored in one 16-block spatial cell. A light query checks only
the source's cell and its 26 neighbors, which fully covers the fixed 7.75-block
visual radius. Moving or changing sources independently observe the configured
update interval. Terrain sections affected by several sources are deduplicated
and rebuilt once at the end of the client tick.

Sources that stop ticking are removed after two client ticks. All source,
spatial-cell, and pending-rebuild state is cleared during disconnects and level
changes.

## Shader behavior

Dynamic lights pause while an Oculus shader pack is active by default. This
avoids applying two unrelated dynamic-light systems to the same lightmap. Use
`/vro lights shaders on` only when the selected shader does not provide suitable
held lighting and has been tested with VRO's lightmap path.

## Resource definitions

Resource packs and mods can add JSON files beneath:

```text
assets/<namespace>/vro_dynamic_lights/*.json
```

Each file contains an `entries` array. Item entries support an optional
`water_sensitive` field. Block entity definitions are fixed type-level light
values.

```json
{
  "entries": [
    {
      "item": "example:crystal_torch",
      "luminance": 13,
      "water_sensitive": true
    },
    {
      "block_entity": "example:glowing_machine",
      "luminance": 9
    }
  ]
}
```

Luminance is clamped to `0-15`. Missing optional mods are ignored during
resource loading. Reloading resource packs replaces the definition maps rather
than accumulating old entries.

## Diagnostics

`/vro lights` reports configured and effective state, source and spatial-cell
counts, the last tick's source updates and section rebuilds, and loaded item and
block entity definition totals. "Configured ON, active OFF" means Compare Mode,
an active shader policy, or Dynamic Lights Reforged currently owns the path.

VRO applies no dynamic-light mixins when `dynamiclightsreforged` is installed.
Adding or removing that standalone mod requires a client restart.
