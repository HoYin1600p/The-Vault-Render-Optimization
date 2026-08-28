# The Vault Render Optimization

**Client-side performance and stability improvements for Vault Hunters, large
Create contraptions, crowded bases, particles, terrain, and long play
sessions.**

The Vault Render Optimization (VRO) is a Minecraft 1.18.2 Forge mod that cuts
repeated client work while preserving normal models, textures, animations,
effects, loot, and server gameplay. It works with official and custom Vault
Hunters packs, and its generic optimizations remain available when Vault
Hunters is not installed.

VRO is client-side. The remote server does not need it.

VRO can check its raw GitHub update manifest asynchronously and show an update
row on the main menu plus occasional clickable CurseForge reminders in chat.
Checks are enabled by default, while displayed update types default to critical
only. The feature has bounded network behavior, fails closed, and never
downloads or installs updates.

## Vault Hunters improvements

- Caches repeated Vault gear, armor, tool-model, ability HUD, durability, and
  damage-number work.
- Reduces repeated client render-event processing used by Vault lighting,
  biome colors, dimension effects, and end-of-level rendering.
- Defers unused Vault Loot Beams tooltip work and clears retained data when a
  world unloads.
- Reduces iSpawner display-item work without changing its animation or
  configured viewing distance.
- Resolves the Vault map and Xaero's World Map `M`-key conflict.
- Isolates Vault elixir number rendering so it cannot corrupt later particle
  colors.

## Create and shader improvements

- Divides large Create contraptions into render sections so off-screen pieces
  can be skipped without lowering model detail.
- Skips off-screen contraption actors and special block entities when it is
  safe to do so.
- Keeps Flywheel GPU instancing available with supported Oculus shader stacks,
  avoiding the severe fallback-renderer slowdown seen around large moving
  contraptions.
- Works with supported Rubidium and Embeddium configurations.
- Allows shader packs to provide dedicated Flywheel scene and shadow programs.
  When they are unavailable or fail to compile, VRO automatically retries its
  generated compatibility path and then falls back safely.
- Restores Flywheel's normal instancing default when a pack ships it disabled,
  while retaining hardware and shader failure safeguards.

Use `/vro create status` to see the active Flywheel backend, shader path, and
contraption-culling activity. Shader compatibility can be changed immediately
with `/vro create shader_compat on|off|status`.

## General performance and memory

- Reduces client collision work around dense mob farms and rapid kill systems.
- Reuses safe particle-light and renderer lookups and skips empty particle,
  toast, tutorial, debug, and renderer setup work.
- Compacts baked-model and block-state data beyond the reductions already
  present in FerriteCore 4.2.2.
- Adds separate vertical and horizontal terrain-section culling. Vertical
  culling is enabled by default; horizontal culling is optional.
- Cleans up retained Create Addition, Powah, Vault Loot Beams, and empty-item
  references during long sessions and world changes.

## Stability fixes

VRO includes client-side guards for several known stale-state crashes,
including Powah cable replacement, Vault Integrations altar conduits, and
Xaero's World Map cache writes. These fixes do not change server behavior.

An optional spatial dynamic-light engine supports held and dropped items,
luminous entities, and resource-defined block entities. It is disabled by
default and can be controlled in game with `/vro lights`.

## Update notices

VRO checks its repository-owned update manifest in the background and never
downloads or installs files. When an allowed release is available, it can show
a small coordinated main-menu row and an occasional in-world reminder linking
to this official CurseForge page.

Update checks are enabled by default, while displayed update types default to
critical-only. Use `/vro updates all` to include normal release notices or
`/vro updates off` to disable VRO's menu and chat notices immediately. Short
timeouts, response limits, and fail-closed parsing keep network or manifest
problems nonfatal. Rejoining, changing dimensions, or transferring servers
does not repeat a reminder during the same Minecraft launch.

## Compatibility

- **Minecraft:** 1.18.2
- **Forge:** 40.3.11 or newer in the Forge 40.x line
- **Environment:** Client only
- **Vault Hunters:** Official Third Edition, Remastered, Wolds Vaults, and
  selected custom 1.18.2 baselines
- **Create shader path:** Create 0.5.1.i, Flywheel 0.6.11, Oculus 1.6.x, and
  supported Rubidium or Embeddium releases

Optional integrations load only when their target mod is present. VRO yields
overlapping work when Entity Collision FPS Fix, BadOptimizations, Particle
Core, Flerovium, Better Fps - Render Distance, or Dynamic Lights Reforged is
installed.

## Installation

1. Stop Minecraft.
2. Remove or disable older VRO jars.
3. Download the latest release and place its VRO jar in the instance's `mods`
   folder.
4. Keep only one active VRO jar.

No server installation, world migration, cache deletion, or settings reset is
required.

## In-game controls

| Command | Purpose |
| --- | --- |
| `/vro` | Show the current Compare Mode state. |
| `/vro compare on|off|status` | Compare VRO optimizations without restarting Minecraft. |
| `/vro updates on|off|status|critical|all` | Control update checks and choose critical-only or all notices. |
| `/vro culling` | View or change vertical and horizontal terrain culling. |
| `/vro lights` | View or change the optional dynamic-light engine. |
| `/vro create status` | Show Create, Flywheel, shader-path, and culling status. |
| `/vro create shader_compat on|off|status` | Control Flywheel compatibility with Oculus shaders. |

Crash guards, world cleanup, and map-key compatibility remain active in
Compare Mode because they are correctness fixes rather than performance
features.

## Source, support, and credits

- [Source code](https://github.com/HoYin1600p/The-Vault-Render-Optimization)
- [Issue tracker](https://github.com/HoYin1600p/The-Vault-Render-Optimization/issues)
- [Installation and compatibility](https://github.com/HoYin1600p/The-Vault-Render-Optimization/blob/main/docs/INSTALLATION.md)
- [Create shader compatibility](https://github.com/HoYin1600p/The-Vault-Render-Optimization/blob/main/docs/FLYWHEEL_SHADER_COMPAT.md)
- [Complete changelog](https://github.com/HoYin1600p/The-Vault-Render-Optimization/blob/main/CHANGELOG.md)
- [Credits](https://github.com/HoYin1600p/The-Vault-Render-Optimization/blob/main/CREDITS.md)
- [Third-party notices](https://github.com/HoYin1600p/The-Vault-Render-Optimization/blob/main/THIRD_PARTY_NOTICES.md)

VRO was developed by [HoYin1600p](https://github.com/HoYin1600p) and is
licensed under GNU AGPL v3.0 or later. Complete adapted-source attribution and
license notices are included in the public repository and release jar.

No third-party mod jar, Vault Hunters source, shader pack, or decompiled class
is bundled. Minecraft is a trademark of Microsoft. Vault Hunters belongs to
its respective authors. This independent project is not affiliated with
Mojang, Microsoft, Forge, Iskallia, or the credited projects.
