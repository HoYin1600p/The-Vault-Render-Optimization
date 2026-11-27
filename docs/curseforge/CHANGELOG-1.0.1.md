# The Vault Render Optimization 1.0.1

Vault Hunters 22.0.x compatibility, important GPU/barrel/collision fixes, and
new default-on terrain, HUD, audio, and render optimizations that help both
default setups and Oculus shader packs.

## Added

- **Angle-based terrain section occlusion** (`angle_section_occlusion`, default
  on, hot-configurable): culls unreachable opposing-face chunk sections in
  Embeddium `0.3.18`/`0.3.19` with no visual difference. Automatically skips
  spectator mode, Compare Mode, and Oculus shadow passes, and yields to
  Flerovium.
- **Built-in ImmediatelyFast upgrades** (when standalone ImmediatelyFast is not
  installed): preserves `GL_STATIC_DRAW` vertex buffers instead of forcing
  `GL_DYNAMIC_DRAW`, and batches `ItemBorders` / `ModdedItemBorders` slot-border
  fills into a single HUD fill pass.
- **Sound distance and static-channel culling** (`sound_distance_culling`,
  `sound_channel_culling`, default on, hot-configurable): skips inaudible
  out-of-range non-tickable sounds (checking OpenAL listener, camera, and player
  position plus a 4-block safety margin so teleport/elevator sounds always play)
  and skips non-streaming sounds when OpenAL static channels are already full;
  yields to Flerovium.
- **Sky-angle caching** (`sky_angle_caching`, default on, hot-configurable):
  avoids duplicate sky-angle queries per sky render pass; yields to
  BadOptimizations.
- **Internal hot-path cleanups:** skips Sophisticated Storage barrel fill-bar
  rendering when the front display is culled, skips empty Create contraption
  render layers, and removes synchronization from hot Flywheel shader-state
  reads.

## Changed

- **Vault Hunters 22.0.0 / 3.22.0 / 22.0.1 compatibility:** VRO now detects and
  steps aside for the seven fixes Vault ships itself in builds `7209`, `7210`,
  and `7237` (event listener cache, ability cache, tool model cache, armour
  texture cache, armour layer factory, spawner item skip, and empty-`ItemStack`
  entity-reference cleanup) while keeping all fixes active on older Vault
  builds.
- `/vro particles stress nova` now supports Vault 22.x's new Nova particle
  classes alongside legacy `NovaParticleMessage`.

## Fixed

- Animated item textures (and animated block models in block entities) now keep
  animating with GPU items enabled under Embeddium/Rubidium's "Animate Only
  Visible Textures".
- Sophisticated Storage barrel displays no longer disappear inside Create
  contraptions or other virtual worlds due to mismatched camera coordinates.
- Players keep normal vanilla mob pushback while non-player mobs continue
  skipping redundant client-side entity push checks.
- Pre-release fixes verified before release: sound distance culling checks the
  OpenAL listener, camera, and player position (with a 4-block margin and
  skipping tickable sounds) so Elevator and teleport sounds always play, and a
  planned zero-effect FOV shortcut was removed before release to avoid
  conflicting with JustZoom when `FOV Effects` is set to `0%`.

VRO remains client-only: stop Minecraft, replace the old VRO jar with
`vault_render_optimization.1.0.1.jar`, and keep only one version in your `mods`
folder.

Full technical release details and version diff:
https://github.com/HoYin1600p/The-Vault-Render-Optimization/releases/tag/v1.0.1
