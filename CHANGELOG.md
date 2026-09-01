# Changelog

All notable changes to The Vault Render Optimization are recorded here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Added eleven ModernFix-derived client render and graphics backports: chunk
  meshing traversal, duplicate BufferBuilder allocation prevention,
  reload-safe entity-model cube compaction, bounded profile-texture hashing,
  multipart selector caching, model-variant traversal, transformation hash
  caching, Forge OBJ cache concurrency, guarded STB atlas stitching, Forge
  model-data concurrency, and CTM metadata-cache concurrency.
- Added restart-bound per-feature configuration and `/vro backports`
  diagnostics with immutable owner/reason reporting.
- Added a repository-owned provenance ledger and embedded the complete
  ModernFix LGPL-3.0-or-later license in the runnable jar.
- Added Flerovium-derived camera-basis particle billboard geometry with a
  portable vanilla writer and a packed Rubidium/Embeddium writer. Ordinary
  visible particles retain their four vertices, UVs, colors, light, and roll.
- Added a bounded per-tick light cache shared by particles in the same block.
  This sits behind the existing subclass-safe per-particle light cache.
- Added hot `/vro particles` billboard, renderer-ownership, shared-light, and
  diagnostics controls. Diagnostics report queue classes, particle render/tick
  timings, writer ownership, cache hits, actual light lookups, and empty work.
- Added focused particle geometry and ownership tests, exact Flerovium source
  provenance, and the complete LGPL-3.0 license in the runnable jar.

### Compatibility

- Added exact feature-level ownership arbitration among VRO, the temporary
  current VH Accelerator overlap, and genuinely active ModernFix options.
  Unknown ModernFix state fails closed, and VRO does not force ownership of
  the texture stitcher.
- Preserved the Fluidlogged chunk-meshing exclusion; Isometric Renders and
  Cracker's Wither Storm Mod BufferBuilder exclusions; legacy Rubidium versus
  Embeddium model-data gate; and the exact CTM `1.18.2-1.1.5+5` layout gate.
- Particle billboard ownership can hot-yield to Rubidium/Embeddium. VRO yields
  automatically to Flerovium, leaves custom particle render overrides intact,
  and does not cull visible particles or tick them asynchronously.

### Verification

- Ported the focused traversal, duplicate-allocation, cube-cache, and
  profile-texture cache tests and added ownership/config bootstrap coverage.

## [0.4.0] - 2026-08-28

### Added

- Added a client-only update-notification system sourced from the canonical
  Forge Update Notifier integration package and relocated into VRO's own Java
  package.
- Added bounded asynchronous checks against VRO's raw GitHub update manifest.
  Connection and request timeouts, HTTP status handling, a 262,144-character
  response limit, strict JSON parsing, and fail-closed error handling keep the
  render thread independent from network availability.
- Added a deterministic main-menu update row coordinated with other
  HoYin1600p mods through Forge metadata. Participating rows are sorted by
  display name and mod ID so they do not overlap.
- Added clickable in-world update reminders whose download target is fixed to
  VRO's HTTPS CurseForge project page. Remote manifest content can provide a
  short message but cannot replace the download link.
- Added `update.json` at the repository root using Forge's update-manifest
  schema, plus `updateJSONURL`, `displayURL`, and notifier coordination
  properties in `mods.toml`.
- Added persistent reminder state at
  `config/vault_render_optimization-update-notice-state.json`, written
  atomically after a ten-client-tick delay.
- Added `/vro updates`, `/vro updates status`, `/vro updates on`,
  `/vro updates off`, `/vro updates critical`, and `/vro updates all` as
  immediate client-only controls that require no server permission.
- Added 31 automated tests across manifest fetching and parsing, semantic
  version ordering, severity filtering, state corruption and persistence,
  reminder cadence, once-per-JVM session behavior, network failures, and the
  repository's real VRO manifest.

### Configuration

- Added `updates.check_for_updates`, enabled by default. Disabling it cancels
  the active request and hides VRO's menu and chat notices; enabling it starts
  a fresh request without a restart.
- Added `updates.update_types` with `CRITICAL` and `ALL` values. It defaults to
  `CRITICAL`, and missing or invalid values fail closed to that setting.
- Changing the update-type filter applies immediately to the already fetched
  result without issuing another network request.

### Reminder behavior

- Critical notices are eligible every fifth qualifying client JVM launch;
  normal notices are eligible every tenth launch and require the explicit
  `ALL` filter.
- A launch is eligible only after a successful manifest result confirms an
  update and the client reaches a playable world frame.
- Each JVM can advance the persisted counter at most once and deliver at most
  one chat reminder. Rejoins, dimension changes, and server transfers in the
  same JVM cannot advance or repeat it.

### Licensing and release safety

- Added exact Forge Update Notifier source provenance, its MIT license, and a
  packaged license copy in the runnable jar.
- Added the ignored append-only identity-scan log location and documented the
  required baseline/incremental scan workflow for future public releases.

## [0.3.5] - 2026-08-16

### Added

- Shader packs can now supply dedicated `gbuffers_flw` and `shadow_flw`
  programs for Flywheel 0.6 scene and shadow rendering. VRO reports the active
  source through `/vro create status` and retries its existing generated path
  when a dedicated program is absent, invalid, or fails compilation.

## [0.3.4] - 2026-08-16

### Added

- Added `/vro create status` diagnostics for Flywheel backend state, loaded
  contraption size, special renderers, actors, and per-frame culling counters.
- Added optional Flywheel shader-instancing compatibility for public Oculus
  1.6.4, Rubidium 0.5.6, Create 0.5.1.i, and Flywheel 0.6.11. The feature has
  saved in-game controls, Compare Mode integration, strict version gates, an
  early startup recovery switch, and automatic fallback after shader-program
  compilation failure.
- Added guarded automatic restoration of Flywheel's upstream-default
  `INSTANCING` backend when a modpack ships with Flywheel configured as `OFF`.
  Unsupported GPUs and shader integration failures retain the fallback renderer,
  and users can disable the behavior in VRO's client configuration.

### Changed

- Large Create contraptions can now use cached 16-block render sections so
  off-screen portions are frustum-culled without changing models or detail.
- Create special block entities and movement actors are conservatively culled
  inside contraptions, empty special-renderer buffer flushes are skipped, and
  supported machinery uses tighter directional render bounds.
- Changing Compare Mode now reloads Create's world renderers so each condition
  owns freshly built contraption meshes.

- Vault Loot Beams tooltip data is now populated only when a dropped item is
  actually evaluated for rendering instead of for every item entering the
  client world.
- iSpawner display-item selection no longer creates a stream and temporary
  list every render, and ordinary spawner displays may use normal frustum
  culling without reducing their configured render distance.

### Fixed

- Prevented Minecraft's shared `ItemStack.EMPTY` singleton from retaining the
  last empty dropped-item entity assigned during synchronized data updates.
- Cleared retained Vault Loot Beams tooltip entries when a client world
  unloads.

- Prevented a delayed obsolete Powah cable unload from removing a newer cable
  at the same position and aborting the client chunk-and-light packet that was
  replacing the surrounding chunk.
- Prevented Xaero's World Map from turning an invalidated queued cache write
  into a fatal client crash. Cache preparation validation and writing now share
  the region lock, while stale work uses Xaero's normal buffer cleanup path.

## [0.3.3] - 2026-08-03

### Fixed

- Prevented the optional dynamic-light block-entity observer from scanning
  Minecraft's live ticker list while the feature is disabled, and use a stable
  snapshot when enabled to avoid render-thread concurrent-modification crashes.

## [0.3.2] - 2026-08-03

`0.3.1` was an internal test candidate and was not published.

### Added

- Added an optional client-side dynamic-light engine with separate entity,
  block-entity, shader, and update-interval controls. It is disabled by
  default and includes in-game diagnostics.

### Changed

### Fixed

- Isolated Vault elixir-orb number text from Minecraft's shared render buffer
  and restored particle render state afterward, preventing later particles from
  rendering black when elixir numbers are enabled.
- Corrected access to Powah's world-keyed cable cache so the unload cleanup
  remains active without causing an `IllegalAccessError` during client exit.

### Performance

- Compacted simple baked-model face lists and shared the all-empty side map to
  reduce retained model memory.
- Canonicalized identical block-state `faceSturdy` arrays to reduce duplicate
  cache storage alongside FerriteCore 4.2.2.
- Added independently configurable vertical and horizontal terrain-section
  distance culling for vanilla and Embeddium/Rubidium renderers. Vertical
  culling defaults on at 12 sections; horizontal culling defaults off.

### Compatibility

- Section-distance culling yields to Better Fps - Render Distance when that mod
  is installed and does not change chunk loading or Distant Horizons storage.
- VRO dynamic lights yield completely to Dynamic Lights Reforged when it is
  installed. Shader-pack participation is separately configurable and defaults
  off.

## [0.3.0] - 2026-08-02

### Added

- Initial public release for Minecraft 1.18.2 and Forge 40.3.11+.
- Vault gear durability, armor state, texture, armor model, identified tool
  model, and learned ability-list caches.
- Cached priority listener snapshots for selected high-frequency Vault client
  rendering events.
- Reuse of Vault's existing damage-number formatter.
- Client-only entity collision fast paths for crowded mob areas.
- Particle-light, empty particle renderer, empty toast, inactive tutorial,
  empty debug renderer, entity renderer, and block-entity renderer fast paths.
- Exact-world cleanup for Create Addition and Powah state on world unload.
- Client crash guards for stale Vault Integrations altar conduits and Powah
  cable replacement.
- Vault and Xaero world-map key compatibility.
- Persistent client-side `/vro compare` controls for enabled/disabled testing.
- One jar compiled against official, Remastered, Wolds, and custom Vault API
  baselines without bundling those mods.

### Performance

- A controlled 40-trial campaign across four Vault Hunters clients measured an
  unweighted average improvement of 5.35% in average FPS, 30.66% in 1% lows,
  35.07% in 0.1% lows, 13.94% in p99 frame time, and 4.49% in average client CPU
  time.
- Frames longer than 16.7 ms decreased in every tested client. Frame-time
  consistency is the primary supported performance claim.

### Compatibility

- Equivalent features automatically yield to Entity Collision FPS Fix,
  BadOptimizations, Particle Core, and Flerovium when installed.
- Optional Vault Hunters, Vault Integrations, Powah, and Create Addition paths
  activate only when their target mod is present.
- VRO is client-side and does not need to be installed on the remote server.

### Licensing

- Documented the learned-ability cache adapted from Unobtanium and client
  collision mixins adapted from Entity Collision FPS Fix.
- Released the complete project under AGPL-3.0-or-later, with exact source
  revisions and third-party notices included.

[Unreleased]: https://github.com/HoYin1600p/The-Vault-Render-Optimization/compare/v0.4.0...HEAD
[0.4.0]: https://github.com/HoYin1600p/The-Vault-Render-Optimization/compare/v0.3.5...v0.4.0
[0.3.5]: https://github.com/HoYin1600p/The-Vault-Render-Optimization/compare/v0.3.4...v0.3.5
[0.3.4]: https://github.com/HoYin1600p/The-Vault-Render-Optimization/compare/v0.3.3...v0.3.4
[0.3.3]: https://github.com/HoYin1600p/The-Vault-Render-Optimization/compare/v0.3.2...v0.3.3
[0.3.2]: https://github.com/HoYin1600p/The-Vault-Render-Optimization/compare/v0.3.0...v0.3.2
[0.3.0]: https://github.com/HoYin1600p/The-Vault-Render-Optimization/releases/tag/v0.3.0
