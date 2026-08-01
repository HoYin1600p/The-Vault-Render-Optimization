# Third-Party Notices

## No Client Side Entity Collision Checks

The client collision optimization in this project is adapted from CorgiTaco's
`No-Client-Side-Entity-Collision-Checks` / `Entity Collision FPS Fix` project.

- Source archive: https://github.com/CorgiTaco-Archive/No-Client-Side-Entity-Collision-Checks
- License: CC0 1.0 Universal

The implementation is disabled automatically when the original
`entitycollisionfpsfix` mod is installed, so the two mods can coexist during
migration without applying duplicate mixins.

## Generic Render Fast-Path Design References

The particle-light cache and conservative empty-work fast paths were
independently implemented for Minecraft Forge 1.18.2 after studying optimization
mechanisms in these projects. No source files were copied.

### Particle Core

- Project: Particle Core by fzzyhmstrs
- Source: https://github.com/fzzyhmstrs/pc
- Inspected revision: `1151fe6aca4e1c3b62459de3e3a99ec32af2ac99`
- License observed at inspection: MIT
- Design influence: cache repeated particle light lookups while position and
  world time are unchanged.

VRO does not port Particle Core's particle suppression, distance limits,
asynchronous ticking, movement cache, or renderer implementation. Forge 1.18.2
already supplies particle frustum culling, so that feature was not duplicated.

### BadOptimizations

- Project: BadOptimizations by Thosea
- Source: https://github.com/imthosea/BadOptimizations
- Inspected revision: `5de4a3ad4299909178d8995dc0bc80626be48d44`
- License observed at inspection: MIT
- Design influence: avoid empty renderer/tick setup and cache stable renderer
  lookups.

VRO's implementations target the Minecraft 1.18.2 Forge classes directly and
use their own configuration, invalidation, and compatibility logic. Lightmap
caching, sky-color caching, and other shader-sensitive features were
deliberately excluded.
