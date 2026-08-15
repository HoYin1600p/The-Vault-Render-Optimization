# Changelog

All notable changes to The Vault Render Optimization are recorded here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Added `/vro create status` diagnostics for Flywheel backend state, loaded
  contraption size, special renderers, actors, and per-frame culling counters.

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

[Unreleased]: https://github.com/HoYin1600p/The-Vault-Render-Optimization/compare/v0.3.3...HEAD
[0.3.3]: https://github.com/HoYin1600p/The-Vault-Render-Optimization/compare/v0.3.2...v0.3.3
[0.3.2]: https://github.com/HoYin1600p/The-Vault-Render-Optimization/compare/v0.3.0...v0.3.2
[0.3.0]: https://github.com/HoYin1600p/The-Vault-Render-Optimization/releases/tag/v0.3.0
