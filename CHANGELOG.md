# Changelog

All notable changes to The Vault Render Optimization are recorded here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.5.2] - 2026-10-04

### Added

- Diagnostics for Oculus' batched entity rendering: when a parked buffer segment is filled on the CPU
  instead of going to the GPU, VRO logs why once per distinct reason (the GPU path state, both vertex
  counts, both vertex formats, the batch kind, the builder and both byte sizes), and `/vro gpuentity stats`
  shows the latest reason. Investigates the 0.5.0 Oculus crash fixed in 0.5.1.

### Fixed

- GPU entity models: buffer pops and uploads on threads other than the render thread no longer consume or
  fill the render thread's pending GPU hand-off; such pops are filled on the CPU.
- GPU block models and GPU items: cached meshes are checked against the model's quads on every use, not
  once per frame, so a model that returns different quads for different blocks or stacks in one frame is
  no longer drawn with another one's geometry. A model or tint callback that renders another item from
  inside VRO's capture now leaves that nested render on the CPU.
- The startup GPU audit now requires both halves of each paired hook (`BufferBuilder.popNextBuffer`,
  `BufferUploader.end`); a mod that removes one half turns the GPU path off instead of leaving it half hooked.
- The Oculus segment window resets every frame, so a lost hook can no longer park unrelated buffers.
- Batches whose instance data exceeds the GPU's storage block limit stay on the CPU.
- Native memory: a failed staging-buffer growth can no longer free the same memory twice, verify mode no
  longer leaks on a failed allocation, and a model arena that the driver cannot grow keeps its old contents.
- Texture atlas stitching (ModernFix backport): a very large atlas falls back to vanilla instead of
  overflowing and hanging the loading screen.
- The GPU self-tests ignore GL errors left over from earlier rendering, which could turn the GPU path off
  for the session with a misleading "self-test" reason.
- A model part or upload that throws is left on the CPU instead of crashing the frame.
- ImmediatelyFast port: a pooled buffer is never freed while still building (for example after the PC sleeps
  mid-frame), and a parked GPU batch is dropped before its builder's memory is released.
- The particle provider cache is cleared on resource reload.
- Opening the settings screen with an incompatible Cloth Config shows a message instead of crashing.
- The update reminder is posted on the client thread when a config file edit triggers it.
- Sophisticated Storage: a barrel whose render state cannot be read is updated as without VRO.

### Changed

- Code tidy-up with no change in behaviour: shared mod-id, config-key and command-text constants, commands and
  diagnostics grouped into their own packages, the GPU rendering runtime and the client config definition split
  into smaller classes, and two package dependency cycles removed. A golden test now pins every config key,
  comment and default.
- The jar is reproducible (no build timestamp, fixed file order and times) and its mod metadata lists the issue
  tracker, credits and a logo.
- `/vro particles` no longer names a version in its status line ("[VRO] Particle optimizations ...").
- A failing Vault event listener is logged through the mod log instead of printed to standard error.

## [0.5.1] - 2026-10-03

### Fixed

- Fixed a crash (`IndexOutOfBoundsException` in `HoleBatch.fillItem`) with Oculus' batched entity
  rendering. When Oculus drew a parked buffer segment with a different draw state than the one it was
  reserved in, VRO filled the reserved vertices into Oculus' shorter upload slice using offsets from the
  original buffer. It now fills the original buffer it reserved into, and every CPU fill checks its bounds:
  a hole that does not fit is skipped and counted (`/vro gpuentity stats`, "writes skipped out of range")
  instead of crashing the game.

## [0.5.0] - 2026-10-03

### Performance

- GPU entity models (`gpu_entity_models`, on by default; validated in game on public Embeddium,
  public Oculus and custom forks with 0 mismatching vertices and +8-11% FPS in a crowded mob scene).
  Entity model parts reserve their vertices in vanilla's entity buffer, and a compute shader writes
  them into the vertex buffer vanilla has just uploaded, right before vanilla's own draw.
  - The shader reproduces vanilla's float arithmetic bit for bit and must pass a startup self-test.
  - Reserved vertices are written on the CPU with vanilla's exact bytes whenever they go anywhere
    else, for example sorting.
  - It turns itself off with an Oculus shader pack, on unsupported drivers and in Compare Mode. It
    also stays off when another mod changes `ModelPart` rendering. Skin Layers 3D, wildbackport and
    Xaero's Minimap are supported.
  - Covers mobs, armor stands and unenchanted armor, and block entities drawn through an atlas
    sprite (chests, beds, shulker boxes, signs).
  - Also covers GeckoLib 3 entities: Vault Hunters' knights, Death and Acid mobs, Naga, Scarabs,
    tanks, bosses and pets. With 192 such mobs in view: +50% FPS (90 to 136) and 0 mismatching
    vertices. GeckoLib's own flat-cube normal handling is reproduced exactly; a renderer that
    overrides GeckoLib's cube code, or another mod changing it, keeps those models on the CPU.
  - Billboard particles too (`gpu_particles`, default on, shaders off): each particle reserves its
    four vertices and the compute shader writes them with VRO's exact billboard arithmetic (own
    startup self-test; 39.6 million live-verified vertices, 0 mismatches). No particle is skipped.
    Under heavy Nova particles: particle render -6%, allocation -25%; frame rate within noise.
  - When installed, also covers Ars Nouveau's own GeckoLib copy (familiars, Wilden, Weald walkers)
    and Citadel models (Alex's Mobs). Both are exact against the mods' own rendering and apply only
    when those mods are present. Checked in Wolds Vaults with 621 different mobs in view: 0
    mismatching vertices, and CPU vertex building for entity models almost gone.
  - Only the vertices the CPU wrote are uploaded; the GPU fills the rest in place.
  - Works with public Oculus' batched entity rendering (Oculus installed, shaders off).
  - Translucent parts such as players stay on vanilla's path.
  - Pauses itself where buffers are drawn outside vanilla's upload.
  - Adapted from Accelerated Rendering (MIT). See `docs/GPU_ENTITY_MODELS.md`.
- `/vro gpuentity verify on|off` reads back every GPU dispatch and compares the whole uploaded buffer
  with the exact expected bytes (testing only).
- `/vro particles new on|off` switches the 0.5.0 particle set live, for A/B comparisons: collision
  cache, tick compaction, shared random and provider cache.
- `/vro feature <collision|compaction|random|provider|frustum> on|off` switches each of those
  optimizations live on its own; `/vro feature frustum verify on|off` checks every frustum answer
  against vanilla's; `/vro alloc start|report|resources` measures render-thread allocation per frame
  and shows the live Create contraption meshes and Oculus program-cache pipelines.
- With Fastload installed, visibility checks for particles, entities and block entities skip Fastload's
  per-check event lookup while that event is idle (all gameplay after world load), with identical
  results (`fastload_frustum_bypass`, default on; `/vro feature fastload on|off`). Under heavy Nova
  particles: particle render time -17%, FPS +11% (213 to 237), render-thread allocation -38%. It only
  applies when Fastload's own hook is the only change to that frustum method.
- Experimental, off by default: GPU entity models and particles can stay on with an Oculus shader
  pack (`gpu_entity_models_with_shaders`; `/vro feature gpushaders on|off`). A second compute program
  writes Oculus' extended entity vertices exactly as Oculus computes them (0 mismatches over 63.1
  million live-verified vertices with SolasVH). On a GPU-bound shader setup it cost about 4-8% FPS,
  so it is off by default; it is meant for players limited by a slower CPU. `gpu_particles_with_shaders`
  (`/vro feature gpushaderparticles on|off`, default off) keeps only particles on the GPU. See
  `docs/GPU_ENTITY_MODELS.md`.
- Block items on the GPU (`gpu_items`, default on; `/vro feature gpuitems on|off`). Dropped, framed and
  held solid/cutout block items are written by the compute path, exactly as Embeddium's or Forge's item
  writer would (the mixin audit picks the installed one). This also works under shader packs when
  `gpu_entity_models_with_shaders` is on. Flat items (gems, loot, tools) and translucent blocks work
  too, although their buffers are sorted. Only each quad's two sort positions are written on the CPU
  before vanilla sorts and writes the indices unchanged. With 240 flat stacks added to the scene:
  - shaders off: +17% FPS (132 to 154);
  - SolasVH: +4%;
  - 0 mismatches over 106 million GPU-written vertices and 104 million oracle vertices.

  Glinting and custom-rendered items stay on the CPU, and tiny batches (HUD icons, a held item) are
  filled on the CPU. Asgard, 480 block-item stacks:
  - shaders off: +7% FPS (217 to 232), 19% less render-thread allocation;
  - SolasVH: FPS-neutral;
  - 0 mismatches over 32.6 million GPU-written vertices, and over 42.7 million vertices checked against
    the installed writer.

- Cheaper visibility test for particles, entities and block entities (`allocation_free_frustum`,
  default on): each screen edge is checked against the one box corner that decides it instead of
  up to eight, with no temporary vectors. Every answer is bit-for-bit vanilla's (checked on
  600,000 random boxes and 429,385 live ones); the saving is small.
- Each particle type's provider is resolved once instead of through a registry key and hash
  lookup on every spawn (`particle_provider_cache`, default on). Invalidated whenever a provider is
  registered; the same provider vanilla would find is always returned.
- Particles draw from a per-thread generator with `java.util.Random`'s exact algorithm instead of
  each allocating its own (`particle_shared_random`, default on). That saves a CAS, a `nanoTime`
  call and an `AtomicLong` per particle, plus a CAS per random draw. Same distribution. The same
  generator replaces vanilla's six `Math.random()` calls in the velocity constructor and the
  per-spawn `Random`/`Math.random()` use in Vault's Nova cloud, explosion and Frost/Poison Nova
  particles (Vault hooks skip themselves if Vault changes those classes).
  Particle packets (Frost Nova sends 400 per cast) spread each particle with six Gaussian draws;
  those use the same generator's unsynchronized copy of `Random`'s polar method.
- Particles that die in a tick are removed in one ordered pass instead of one queue shift each
  (`particle_tick_compaction`, default on). Tick order, death timing and particle-group limits
  match vanilla exactly; with thousands of Nova particles expiring per tick this was quadratic.
- Particle collision reads each block once per particle tick instead of once per particle, using
  the exact cells, shapes and collision response vanilla uses (`particle_collision_cache`,
  default on; `/vro particles collision verify on` compares every result with vanilla in game).
  Aimed at Vault Hunters Nova bursts, whose particles collide every tick just above the floor.
  Replaces the opt-in empty-section proof. No particle is skipped or culled.

### Fixed

- Adaptive chunk budget no longer holds back important rebuilds and sorts (the player's own
  block edits). They could wait up to about 250 ms behind background pacing.
- If the old Oculus Flywheel Compat mod (`irisflw`) is still installed, VRO switches it off and
  uses its own Create shader support; the irisflw jar can be removed. Both patch the same
  Flywheel/Oculus methods, so they never run together: if irisflw cannot be switched off safely,
  VRO turns off its own support instead.
- Create shader compatibility checks the installed Oculus jar before applying its mixins. A
  missing class, member or injection target disables the feature with a log message instead
  of failing a required injection. Public Oculus 1.6.4 and the 1.6.5/1.6.7/1.6.8 dh-compat
  builds pass. The startup decision is made once instead of once per mixin config.
- Renderer transfers match validated Embeddium/Rubidium versions exactly and verify the
  bytes of each renderer class that held the fork's own copy. The old `0.3.18`/`0.5.6`
  prefix check accepted pre-removal Embeddium fork builds (including one carrying a stock
  version string) that still contain the transferred code, so both copies ran. Those now
  leave the renderer's own copy in charge.
- The post-removal Embeddium fork `0.3.18-git.ced34c84+mc1.18.2` is supported for chunk
  deferral, adaptive budget and index-only sorting as well (byte-identical to the 0.3.19
  fork for every hashed class). `ChunkBuilder$WrappedTask` and `$WorkerRunnable` are now
  hashed too.
- The custom-Embeddium tests now run whenever the custom jar is found or passed with
  `-Pcustom_embeddium_jar`; `-Prequire_custom_renderer_tests=true` fails instead of skipping.
- Create sectioned contraption meshes on the non-Flywheel path now free their native vertex
  copies when a contraption is invalidated, removed, disabled for sectioning or reset.
  Contraptions that Create drops as dead (without an invalidate call) are swept as well.
- Create shader compatibility no longer keeps references to destroyed Oculus pipelines and
  their program sets. Oculus already closes the programs themselves, so nothing is freed twice.
- Create shader compatibility: shader packs that declare `mc_Entity` as `vec2` or `vec3`
  alongside a `vec4 at_tangent` no longer fail to compile under the extended vertex format
  (the `mc_Entity` swizzle used the tangent's width).
- Accept the exact custom Embeddium `0.3.19-git.7b0cf676+mc1.18.2` build (and only its
  inspected `ChunkBuilder` hash) for chunk deferral, adaptive budget, index-only sorting
  and renderer transfers. This build was previously blocked by the version and hash gates.
  Prior Embeddium/Rubidium baselines are unchanged, and other git builds stay blocked.

### Changed

- `create_rendering.auto_enable_flywheel_instancing` is now session-only. It turns on Flywheel's
  instancing renderer for this session when the pack has it set to off. Flywheel's own config file
  is never changed; turning this off (or turning on Compare Mode) returns to the pack's setting
  immediately. Previously VRO wrote `INSTANCING` into Flywheel's config once and never undid it.

- Release particle light-cache world references on level changes, including dormant worker caches.
- Sample diagnostic particle censuses every 250 ms and include census work in render CPU timings.
- Reuse bounded chunk-budget observation tables and request arrays without changing terrain-loading bypasses.
- Compute Create culling bounds from exact affine extrema instead of eight transformed corners.
- Cache limited-barrel count glyph vertices and consecutive atlas runs; keep per-slot color, light and pose dynamic.
- Cache Vault float-uniform locations across calls, invalidating on shader load/destroy; reuse Flywheel normal matrices
  and replace long-lived temporary-stack uniform buffers with owned storage.

- The build no longer requires the local Prism instances. Every compile-only jar has a
  `-P<name>_jar` override and `-Pvro_deps_dir` supplies a folder of jars; all missing jars are
  reported together. `gradlew printDependencyJars` shows the resolved jars. A fresh clone no
  longer fails with `NoSuchElementException` before the override is read.

### Removed

- VRO's optional dynamic-light engine, its `/vro lights` commands, the `[dynamic_lights]`
  config section and `vro_dynamic_lights` resource definitions. It was default off, never
  validated, and outside VRO's render-speed scope; packs use a standalone dynamic-lights mod.
  Existing config files lose the old section on the next load (Forge corrects the file).

### Added

- Settings screen, opened with a key that is unbound by default (Controls, under "The Vault Render
  Optimization"). Every VRO setting and the built-in ImmediatelyFast options are grouped into
  player-facing tabs, each with a one-line summary that expands into a full explanation on click,
  and a `(restart)` badge where a change needs a restart or world rejoin. Saving applies live settings at once and lists
  the ones that need a restart. **Default** resets everything to the shipped defaults;
  **Experimental** turns on every default-off setting outside Diagnostics. Both ask first. Uses
  Cloth Config (6.5.102 or newer) as an optional dependency: without it VRO runs as before and
  the key says where the settings still live. See `docs/CONFIGURATION.md`. Not yet tested in game.
- VRO's screens are laid out at a fixed 960 x 540 virtual size, so they look the same at every
  resolution and GUI scale; the player's GUI scale is restored when they close.
- **Report a bug** button in the settings screen's Diagnostics tab. It first shows a preview of
  the exact GitHub issue text (versions, rendering stack, non-default settings, Compare Mode and
  GPU path status) and of the newest crash report, with user-folder paths, the player name and
  UUID removed. Nothing is uploaded and nothing happens until the player clicks: **Copy crash
  report & open GitHub** copies the scrubbed crash report to the clipboard and opens a pre-filled
  issue with the exception line and a place to paste it; **Open GitHub without crash report**
  opens the issue without it. Not yet tested in game.
- Built-in ImmediatelyFast: a relocated copy of ImmediatelyFast Reforged 1.18.2 (1.1.10,
  LGPL-3.0-or-later) for immediate-mode and HUD batching, fast text lookup, font atlas resizing,
  map atlases and buffer-upload reuse. On by default
  (`config/vault_render_optimization-immediatelyfast.json`); skipped entirely when the standalone
  mod is installed; with Oculus it runs only if the Oculus members it needs are present. See
  `docs/IMMEDIATELYFAST.md`. Not yet tested in game.
- `/vro particles stress nova|frost <casts per second> <seconds> [radius]`: a client-only benchmark
  driver that replays Nova or Frost Nova casts beside the player through the same client code a real
  cast runs. Nothing is sent to the server and no sound plays.
- Farsight chunk bound (`chunk_updates.farsight_chunk_bound`, default on, `/vro chunks farsight`):
  with Farsight installed, forgets client chunks the server has already unloaded that lie beyond
  `max(server view distance, render distance) + 1` once a second, releasing chunk, light and
  Embeddium/Rubidium render-section memory that Farsight otherwise keeps until the level changes.
  A chunk the server still counts as sent is never dropped, since the server would not resend it
  and a fast-moving player would find an empty hole until relogging (VH Accelerator's fix
  9909763, carried over). Ported from VH Accelerator; the
  jar's `META-INF/vro-features/farsight-chunk-bound` marker makes VH Accelerator leave it to VRO.
  Not affected by Compare Mode. Not yet tested in game.
- `/vro chunks sorting status` reports how much translucent sort data Embeddium keeps on the
  heap and what a centroid-based store would need. Measurement only, computed when the command
  runs; see `docs/INDEX_ONLY_SORTING.md` for the proposed replacement and its prerequisites.
- Opt-in experimental `/vro particles collision on|off`: bypass collision construction only inside a proven
  vanilla-air section covering the entire swept search and its neighbor halo. Yields to Particle Core/Flerovium.
- Opt-in experimental `/vro chunks sort_geometry on|off`: reuse decoded triangle centers for validated Embeddium
  index-only sorting. Weak generation keys, 16 MiB global payload bound, native stable triangle order, and existing
  stale-result rejection remain in force. Unknown/oversized data falls back to the original sorter.
- Opt-in plain HUD text geometry reuse (`/vro experiments hud on|off|status`): exact live
  draw-input keys, bounded replay, font/viewport invalidation and unchanged frame-rate drawing.
  Formatted/animated text, HUD icons/bars and gear data remain on their original paths.
- Opt-in CPU-native vertex buffer trimming (`/vro experiments memory on|off|status`):
  30-second demand history and aggregate pressure, applied only by the owning worker at
  its next safe build start. Existing per-buffer ceiling and deterministic destroy remain.

The first development batch passed automated checks. The subsequent HUD/snapshot/trimming
batch is compiler-checked only; its new regression cases and all in-game comparisons are
deferred at the user's request. No measured FPS gains are claimed.

## [0.4.2] - 2026-09-07

### Added

- Added default-on, hot-configurable Sophisticated Storage rendering paths for
  the validated Remastered 1.18.2 release pair. VRO skips front-face item,
  quantity, fill, and upgrade block-entity rendering when the display faces
  away from the camera or its face is immediately covered; caches bounded
  quantity glyphs; emits fill bars without legacy per-vertex vector
  allocations; and filters count/fill-only client packets that would otherwise
  request a chunk-model rebuild. Tier rendering is intentionally unchanged.
- Added `/vro storage` feature controls and opt-in counters for rendered and
  culled faces, quantity-cache hits/misses, fill bars, allowed model rebuilds,
  and skipped count-only rebuilds.

### Fixed

- Corrected limited-barrel fill texture coordinates and preserved the expected
  small multi-slot fill column before release.

## [0.4.1] - 2026-09-05

### Changed

- Moved the unreleased Mana Stealer visual replacement, player drain stream,
  preview commands, config, assets, mixins, and tests into Arcane Beam so VRO
  remains focused on rendering performance and stability.

### Added

- Added dynamic deferred chunk build/upload budgets for bytecode-validated
  Embeddium 0.3.18/0.3.19. Learns per-machine costs during play, limits upload
  batches, applies queue backpressure and reserves aged update-class admissions.
  Includes `/vro chunks budget on|off|status`, Compare Mode fallback and bounded
  diagnostics. Predictive budgets may be exceeded by indivisible uploads;
  visual updates can be delayed under load. Revised experiment defaults on
  under a fresh `chunk_updates.adaptive_budget_v2` key after user testing exposed
  cached-terrain loading delays. Initial builds and their ready results now use
  native scheduling/uploads; queue pressure and waiting work also force native
  fallback, with a recovery cooldown. Explicit v2 off settings remain respected.
  Diagnostics expose pending requests, queued/active workers and
  pacing/fallback reasons. The revised loading guard was validated in the
  VaultCrafters client before release preparation.

- Added independently switchable index-only terrain transparency sorting for
  bytecode-validated Embeddium 0.3.18/0.3.19. Retains unchanged vertices,
  shares immutable heap snapshots, and batches drawing-order uploads. Stale
  sorting results cannot replace rebuilt/unloaded geometry. Includes hot
  `/vro chunks sorting on|off|status` controls, Compare Mode support and counters.
  Vanilla and Rubidium 0.5.6 are unchanged by this feature.

- Added default-on VRO-controlled asynchronous chunk updates for vanilla Forge
  and validated Embeddium/Rubidium renderers. Uses each renderer's native
  deferred path without editing its settings or replacing its task lifecycle.
  `/vro chunks defer on|off` controls the feature immediately; `/vro chunks
  status` reports selection and observed behavior. Visible block changes may
  arrive later under load. Off/Compare Mode preserves native preferences.

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
- Added eight independently gated Embeddium/Rubidium renderer corrections:
  adjacent-position face hiding, null-buffer vertex sinks, direct
  CodeChickenLib renderer lookup, bounded vertex-buffer retention, capped
  asynchronous arena growth, cached smooth lighting for non-luminous fluids,
  chunk-layer shader-color reset, and equivalent chunk-rebuild coalescing.
- Added renderer-family ownership diagnostics and restart-bound
  `[embeddium_transfers]` controls. Supported stock Embeddium and Rubidium
  layouts are selected explicitly; unknown or ambiguous stacks fail closed.

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
- Correctly recognizes Embeddium's same-JAR Rubidium compatibility identity as
  one renderer while continuing to block genuinely separate renderer mods.
- Preserves the distinct stock Embeddium and Rubidium method/field layouts for
  face hiding, fluid lighting, arena growth, and active rebuild tasks.

### Fixed

- Fixed Create/Flywheel block-model format handling during Oculus pipeline
  transitions, including the DH-ready reload before world login. Model readers
  now follow each buffer's actual format; instanced model ticks wait while the
  extended format remains enabled without a pipeline. Removed an unreachable
  format-forcing fallback and released both native copies owned by extended
  readers. Offline regression coverage added; in-game confirmation pending.
- Bound The Vault's native grayscale shader only while its startup values are
  uploaded, then restored the previously active OpenGL program. This removes
  the two `No active program` errors without changing normal shader ownership.
- Fixed the adjacent-position face-hiding call used by custom blocks.
- Prevented optimized vertex sinks from dereferencing a missing backing buffer
  during loading transitions.
- Bounded retained native vertex buffers and speculative renderer-arena
  headroom so long sessions cannot retain or compound pathological peaks.
- Preserved smooth lighting for non-luminous fluids, reset leaked shader color
  after chunk layers, and suppressed duplicate non-sort chunk rebuilds without
  losing stronger updates.

### Verification

- Ported the focused traversal, duplicate-allocation, cube-cache, and
  profile-texture cache tests and added ownership/config bootstrap coverage.
- Completed the renderer-transfer unit and structural suite, five-pack build
  matrix, stock Embeddium live pressure tests, accepted long-session evidence,
  and post-removal single-owner control recorded in
  `docs/EMBEDDIUM_OWNERSHIP_TRANSFER.md`.

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

[Unreleased]: https://github.com/HoYin1600p/The-Vault-Render-Optimization/compare/v0.4.2...HEAD
[0.4.2]: https://github.com/HoYin1600p/The-Vault-Render-Optimization/compare/v0.4.1...v0.4.2
[0.4.1]: https://github.com/HoYin1600p/The-Vault-Render-Optimization/compare/v0.4.0...v0.4.1
[0.4.0]: https://github.com/HoYin1600p/The-Vault-Render-Optimization/compare/v0.3.5...v0.4.0
[0.3.5]: https://github.com/HoYin1600p/The-Vault-Render-Optimization/compare/v0.3.4...v0.3.5
[0.3.4]: https://github.com/HoYin1600p/The-Vault-Render-Optimization/compare/v0.3.3...v0.3.4
[0.3.3]: https://github.com/HoYin1600p/The-Vault-Render-Optimization/compare/v0.3.2...v0.3.3
[0.3.2]: https://github.com/HoYin1600p/The-Vault-Render-Optimization/compare/v0.3.0...v0.3.2
[0.3.0]: https://github.com/HoYin1600p/The-Vault-Render-Optimization/releases/tag/v0.3.0
