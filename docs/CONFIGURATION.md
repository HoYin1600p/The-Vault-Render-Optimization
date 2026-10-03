# Configuration and commands

VRO stores client settings in:

```text
config/vault_render_optimization-client.toml
```

All release fast paths are enabled by default. Forge reloads changes made by
VRO's command immediately. For manual file edits, stop Minecraft first.

## Settings screen

VRO has an in-game settings screen. Open it with the **Open VRO settings** key,
which is unbound by default: set it in Options > Controls, under
**The Vault Render Optimization**. There is no Mods-list or pause-menu button.

VRO's screens (the settings screen, its dialogs and the bug-report preview)
look the same at every resolution and GUI scale setting. While one of them is
open, VRO lays it out at a fixed virtual size of 960 x 540 GUI units, scaling
it to fill the window (for example 2x at 1080p, about 2.67x at 1440p, 4x at
4K; windows smaller than 960 x 540 pixels use 1x). Your own GUI scale is
restored as soon as you leave VRO's screens.

The screen needs [Cloth Config](https://www.curseforge.com/minecraft/mc-mods/cloth-config)
(Forge, 6.5.102 or newer). Cloth is an optional dependency. Without it VRO
loads and runs normally, and pressing the key prints a chat message saying that
the settings can still be changed in the `.toml` file and with `/vro` commands.

Settings are grouped into tabs by what they affect, not by `.toml` section:
GPU rendering, Chunks & terrain, Entities & particles, Interface & HUD, Mod
compatibility, ImmediatelyFast, Updates and Diagnostics. The ImmediatelyFast
tab edits `config/vault_render_optimization-immediatelyfast.json`. Its
cosmetic and debug-only options are under Diagnostics. The option
`debug_only_and_not_recommended_disable_mod_conflict_handling` is not shown,
because VRO never reads it.

Each setting is a toggle, a number field or slider (within the `.toml` range),
or a selector. A grey line under it summarises it; click that line to show or
hide the full explanation underneath. A `(restart)` badge marks settings that take effect only after a
restart: the ModernFix backports, the Embeddium/Rubidium transfer switches,
every ImmediatelyFast option and Compare Mode's effect on those startup
features. The two buffer-arena sizes are badged too; they apply after
rejoining a world.

**Save** writes the values through Forge's config (and the ImmediatelyFast
JSON), applies live settings at once the same way a config reload and the
`/vro` commands do, and then lists any changed setting that needs a restart.

Two extra buttons sit beside Cancel and Save. Both ask for confirmation first,
discard unsaved edits on the screen, and save and apply immediately:

- **Default** resets every setting, including Diagnostics and the
  ImmediatelyFast options, to the shipped defaults.
- **Experimental** turns on every setting that ships off, except those in
  Diagnostics and horizontal section culling (which shortens the sideways draw
  distance rather than being an experiment). The confirmation lists the
  settings it will change. Currently these are GPU models with shaders, GPU
  particles with shaders, the sort geometry cache, HUD text reuse, adaptive
  vertex buffer trimming and ImmediatelyFast's hotbar item batching.

### Report a bug

The Diagnostics tab starts with a **Report a bug** button. It opens a preview
screen. VRO uploads nothing: it only copies text to your clipboard and opens
a page in your browser, and only after you click.

The preview shows the exact text of a new GitHub issue for
`HoYin1600p/The-Vault-Render-Optimization`:

- versions of VRO, Minecraft, Forge, Java and the operating system;
- the rendering stack: GPU and driver, Embeddium or Rubidium, Oculus and its
  shader pack, Flywheel, and whether ImmediatelyFast is the standalone mod or
  VRO's built-in copy;
- VRO's state: Compare Mode, every setting that differs from its default, and
  the GPU path status including its self-test results.

If `crash-reports/` holds a crash report, the newest one is shown below the
issue text exactly as it would be copied. Before anything is shown, paths
inside user folders (`C:\Users\<name>\`, `/home/<name>/`, `/Users/<name>/`)
become `<user>`, and your Minecraft name and UUID become `<player>` and
`<uuid>`.

- **Copy crash report & open GitHub** copies that crash report to the
  clipboard and opens the pre-filled issue, which includes the exception line
  and a marked place to paste the report.
- **Open GitHub without crash report** opens the issue without the crash
  section. Without a crash report this button is simply **Open GitHub**.
- **Cancel** goes back.

The link is kept under about 7,500 characters. When the report is longer, GPU
status lines are dropped first, then non-default settings. Describe the problem
and submit the issue yourself on GitHub.

## Chunk-update frame pacing

| Key | Default | Purpose |
| --- | --- | --- |
| `chunk_updates.defer_updates` | `true` | Use native asynchronous chunk scheduling to avoid waiting for important chunk work on the render thread |

No renderer mod is required. Vanilla Forge 1.18.2, Embeddium
`0.3.18+mc1.18.2` / the validated `0.3.19+mc1.18.2` fork, and Rubidium `0.5.6`
use separate guarded paths. Unsupported/ambiguous renderers are left alone.

`/vro chunks defer on|off` saves the VRO setting and applies to subsequent
scheduling decisions without restarting. `/vro chunks status` reports the
backend, whether its scheduling hook has run, and the last observed native
deferral preference. Existing queued tasks finish through their native path.

Off and Compare Mode **yield to the user's original settings**, not force a
blocking renderer. If Embeddium's own "Always Defer Chunk Updates" is on,
updates remain deferred when VRO is off. For an isolated VRO comparison, keep
the native setting off in both test conditions. VRO never writes that setting.

This can delay visible placement/removal or transparency updates, particularly
under sustained load. It does not discard dirty updates, change server blocks,
lower render distance, or cap native upload time. See
[implementation and testing notes](CHUNK_UPDATE_DEFERRAL.md).

## Index-only terrain transparency sorting

`chunk_updates.index_only_sorting=true` enables VRO's index-only sorting path
on validated Embeddium `0.3.18+mc1.18.2` / `0.3.19+mc1.18.2`. Startup checks
the inspected renderer bytecode; changed/unknown implementations are blocked.
Vanilla and Rubidium 0.5.6 do not expose this path and are left unchanged.

- `/vro chunks sorting on|off` saves the toggle without a restart.
- `/vro chunks sorting status` reports applied/yielded/blocked status, scheduled
  and applied jobs, stale/fallback counts, and avoided vertex copy/upload bytes.
- Off/Compare Mode restores native creation of new sort jobs. Existing VRO
  jobs still finish and release their resources. The renderer's own translucent
  sorting option is respected and is never changed by VRO.

No new queue, worker, sorting algorithm, GPU format or visibility reduction is
introduced. See [implementation and validation](INDEX_ONLY_SORTING.md).

## Update notices

| Key | Default | Purpose |
| --- | --- | --- |
| `updates.check_for_updates` | `true` | Checks VRO's raw GitHub update manifest without blocking the client |
| `updates.update_types` | `CRITICAL` | Shows only `[CRITICAL]` notices; set `ALL` to include normal notices |

The filter applies to both the coordinated main-menu row and in-world chat
reminders. Changing it reuses an already fetched result. Invalid or missing
filter values fall back to `CRITICAL`. Disabling checks cancels the active
request and hides all notices; enabling them starts a fresh request
immediately.

Critical reminders are eligible every five qualifying client launches and
normal reminders every ten. A launch counts at most once per JVM, only after a
successful update result and a playable world frame. Rejoins, dimension
changes, and server transfers in the same JVM do not advance the counter or
show another reminder. Reminder state is stored in
`config/vault_render_optimization-update-notice-state.json`.

The manifest request uses HTTPS, has bounded connection/request timeouts and
response size, and fails closed on network, HTTP, JSON, or local-state errors.
The download target is always VRO's fixed CurseForge project page; remote JSON
cannot replace it. VRO reports updates but never downloads or installs them.

## Compare Mode

| Key | Default | Purpose |
| --- | --- | --- |
| `benchmark.compare_mode` | `false` | Disables all VRO performance optimizations for comparison |

Compare Mode does not disable client crash guards, unloaded-world cleanup, or
Vault/Xaero map-key compatibility. Those behaviors are intentionally kept out
of performance comparisons.

Changing Compare Mode clears Vault gear and tool caches so the next frame does
not reuse data created under the previous state. When Create is installed, it
also reloads Create's world renderers so a sectioned or monolithic contraption
mesh is rebuilt for the newly selected condition.

The ModernFix-derived startup backports below read Compare Mode before mixin
selection and stay disabled for that entire JVM launch. Restart after changing
Compare Mode when those paths are part of a comparison.

## ModernFix render backports

These restart-bound options default on. VRO applies one only when its exact
implementation is not still present in VH Accelerator, ModernFix does not own
the corresponding active option, and the compatibility gate passes. An
unverifiable ModernFix state fails closed.

| Key | Default | Purpose / compatibility |
| --- | --- | --- |
| `modernfix_backports.chunkMeshing` | `true` | Reuses a section iterator and duplicate block-state lookup; unavailable with Fluidlogged. |
| `modernfix_backports.bufferBuilderLeakFix` | `true` | Prevents duplicate RenderBuffers allocation; unavailable with Isometric Renders or Cracker's Wither Storm Mod. |
| `modernfix_backports.compactEntityModels` | `true` | Shares immutable entity-model cubes and clears the cache on model reload. |
| `modernfix_backports.profileTextureHashCache` | `true` | Bounds repeated profile-texture URL/hash work to 2,048 entries with 60-second after-access expiry. |
| `modernfix_backports.modelSelectorPredicateCache` | `true` | Caches multipart selector predicates by block state definition. |
| `modernfix_backports.modelVariantTraversal` | `true` | Reduces allocations while preserving missing-texture reporting. |
| `modernfix_backports.modelTransformationHashCache` | `true` | Caches immutable model transformation hashes. |
| `modernfix_backports.objModelCacheConcurrency` | `true` | Uses concurrent Forge OBJ material and model caches. |
| `modernfix_backports.fasterTextureStitching` | `true` | Uses guarded STB packing for atlases with at least 100 sprites; small/oversized candidates retain vanilla behavior. |
| `modernfix_backports.modelDataManagerConcurrencyFix` | `true` | Corrects Forge model-data refresh concurrency; unavailable with legacy Rubidium unless Embeddium is present. |
| `modernfix_backports.ctmMetadataCacheConcurrencyFix` | `true` | Corrects nullable CTM metadata caching only for validated CTM `1.18.2-1.1.5+5`. |

Use `/vro backports` to see the immutable owner and reason selected for each
feature in the current launch. See
[`MODERNFIX_RENDER_BACKPORTS.md`](MODERNFIX_RENDER_BACKPORTS.md) for exact
source provenance and ownership rules.

## Render fast paths

| Key | Default | Purpose |
| --- | --- | --- |
| `render_fast_paths.particle_light_cache` | `true` | Reuses unchanged particle light during one client tick |
| `render_fast_paths.particle_shared_light_cache` | `true` | Shares a bounded light result among particles in the same block and tick |
| `render_fast_paths.particle_billboard_fast_path` | `true` | Builds ordinary particle corners from the camera left/up basis |
| `render_fast_paths.particle_billboard_owner` | `AUTO` | Chooses `AUTO`, `RENDERER`, or `VRO` ownership for ordinary billboards |
| `render_fast_paths.particle_diagnostics` | `false` | Collects queue classes, render/tick timing, writer, and light-cache counters |
| `render_fast_paths.skip_empty_particle_render` | `true` | Avoids particle renderer setup when all queues are empty |
| `render_fast_paths.skip_empty_toast_render` | `true` | Avoids toast renderer work when no toast is active |
| `render_fast_paths.skip_empty_debug_render` | `true` | Avoids debug renderer work when supported overlays are inactive |

`AUTO` selects VRO's geometry and uses Rubidium/Embeddium's packed writer when
available. `RENDERER` yields the complete render call to Rubidium/Embeddium;
without a compatible renderer it safely falls back to VRO. `VRO` explicitly
selects VRO. Flerovium remains the external owner whenever it is installed.

The billboard, owner, shared-light, and diagnostics settings are hot through
`/vro particles`. Particle subclasses that replace the normal render or light
method retain their own behavior. VRO does not reduce particle counts, cull
on-screen particles, or move particle work to another thread. Diagnostics are
off by default because class-level queue accounting is intended for profiling,
not everyday play.

## Sophisticated Storage

| Key | Default | Purpose |
| --- | --- | --- |
| `sophisticated_storage.front_display_culling` | `true` | Skips item, quantity, fill, and upgrade displays when their barrel face points away from the camera or is immediately covered |
| `sophisticated_storage.quantity_text_cache` | `true` | Reuses bounded formatted quantity glyphs and measured widths |
| `sophisticated_storage.fill_level_fast_path` | `true` | Emits the existing fill-bar geometry without temporary per-vertex vectors |
| `sophisticated_storage.count_only_render_update_filter` | `true` | Avoids terrain rebuilds when synchronized changes affect quantity/fill displays but not the barrel model |
| `sophisticated_storage.diagnostics` | `false` | Collects front-face, quantity, fill, and render-update counters |

These settings are hot through `/vro storage`. They apply only to the exact
validated Sophisticated Storage `1.18.2-0.9.8.915` and Sophisticated Core
`1.18.2-0.6.4.604` pair. Unknown internal layouts fail closed. Compare Mode
disables all four performance paths. Tier rendering is not modified.

## Client tick fast paths

| Key | Default | Purpose |
| --- | --- | --- |
| `client_tick_fast_paths.skip_inactive_tutorial` | `true` | Skips the completed tutorial's empty tick when no timed tutorial toast exists |

## Renderer lookup caches

| Key | Default | Purpose |
| --- | --- | --- |
| `renderer_lookup_caches.entity_renderer_cache` | `true` | Caches non-player entity renderers by entity type |
| `renderer_lookup_caches.block_entity_renderer_cache` | `true` | Caches block-entity renderers by block-entity type |

Both caches are cleared and rebuilt after resource reloads. Player renderer
selection remains on Minecraft's established path.

## Section-distance culling

| Key | Default | Purpose |
| --- | --- | --- |
| `section_distance_culling.vertical_enabled` | `true` | Skips terrain sections beyond the vertical camera range |
| `section_distance_culling.vertical_distance` | `12` | Vertical range in 16-block sections |
| `section_distance_culling.horizontal_enabled` | `false` | Enables circular horizontal terrain culling |
| `section_distance_culling.horizontal_distance` | `24` | Horizontal radius in 16-block sections |

The limits use each section's nearest edge, are symmetric around the camera,
and do not rotate with view direction. They affect terrain drawing only. Chunk
loading, generation, simulation, server distance, and Distant Horizons storage
are unchanged. VRO supplies separate vanilla and Embeddium/Rubidium paths and
yields both when Better Fps - Render Distance is installed.

## Commands

| Command | Result |
| --- | --- |
| `/vro` | Shows Compare Mode state |
| `/vro compare` | Shows Compare Mode state |
| `/vro compare status` | Shows Compare Mode state |
| `/vro compare on` | Saves and immediately disables VRO performance paths |
| `/vro compare off` | Saves and immediately enables configured performance paths |
| `/vro backports` | Reports the launch-time owner and reason for every ModernFix-derived render backport |
| `/vro storage` | Shows Sophisticated Storage availability and all four hot feature states |
| `/vro storage face on\|off` | Controls front-display backface/covered-face culling |
| `/vro storage counts on\|off` | Controls the bounded quantity-label cache |
| `/vro storage fill on\|off` | Controls allocation-light fill bars |
| `/vro storage updates on\|off` | Controls count/fill-only chunk-rebuild filtering |
| `/vro storage diagnostics on\|off\|reset` | Controls and resets Sophisticated Storage profiling counters |
| `/vro updates` | Shows whether checks are enabled and the selected update types |
| `/vro updates status` | Shows the same update-notice state explicitly |
| `/vro updates on` | Enables checks, saves the setting, and starts a fresh request |
| `/vro updates off` | Disables checks, saves the setting, and hides all notices |
| `/vro updates critical` | Saves the critical-only filter and applies it immediately |
| `/vro updates all` | Saves the all-update filter and applies it immediately |
| `/vro culling` | Shows section-culling state and distances |
| `/vro culling vertical on` | Enables vertical terrain culling immediately |
| `/vro culling vertical off` | Disables vertical terrain culling immediately |
| `/vro culling vertical <1-64>` | Saves the vertical distance immediately |
| `/vro culling horizontal on` | Enables horizontal terrain culling immediately |
| `/vro culling horizontal off` | Disables horizontal terrain culling immediately |
| `/vro culling horizontal <1-64>` | Saves the horizontal distance immediately |
| `/vro create` | Shows Create/Flywheel and loaded-contraption diagnostics |
| `/vro create status` | Shows the same Create diagnostics explicitly |

These are client commands in multiplayer. They require no server permission
and work even when the remote server does not have VRO.

## Create rendering

| Key | Default | Purpose |
| --- | --- | --- |
| `create_rendering.skip_empty_contraption_buffer_flush` | `true` | Skips Create's shared-buffer flush when a contraption has no special block entities to submit |
| `create_rendering.contraption_block_entity_culling` | `true` | Frustum-culls non-instanced special block entities inside contraptions |
| `create_rendering.contraption_actor_culling` | `true` | Frustum-culls movement actors inside contraptions |
| `create_rendering.sectioned_contraption_meshes` | `true` | Splits large contraption geometry into local 16-block sections for frustum culling |
| `create_rendering.sectioned_mesh_block_threshold` | `512` | Minimum rendered-block count for sectioned contraption meshes |
| `create_rendering.smart_machinery_render_bounds` | `true` | Uses tighter directional bounds for supported Create machinery |
| `create_rendering.auto_enable_flywheel_instancing` | `true` | Turns on Flywheel's instancing renderer for this session when the pack has it set to `OFF`. Flywheel's own config file is never changed; turning this off returns to the pack's setting immediately. Unsupported hardware and integration failures retain the fallback renderer |

Sectioned meshes preserve Create's original block models, textures, lighting,
render layers, and shader program. The feature is not LOD: it does not reduce
detail, substitute generic blocks, or hide geometry based on distance. A large
contraption may take slightly longer to build its render data once because it
creates multiple cached mesh sections; the intended gain is lower recurring
draw work when only part of the structure is on screen.

The smart-bounds path covers belts, mechanical arms, deployers, portable
storage interfaces, and mechanical rollers in Create 0.5.1.i. All Create paths
are optional and are omitted automatically when Create is absent.

## Farsight chunk bound

`chunk_updates.farsight_chunk_bound` defaults to `true` and only acts when
Farsight (`farsight_view`) is installed. Farsight cancels the client's chunk
forget packet, so every chunk seen while travelling stays in memory (chunk data,
light data and Embeddium/Rubidium render sections) until the level changes. Once
a second, VRO forgets chunks that the server has already unloaded and that are
farther than `max(server view distance, render distance) + 1` chunks from the
player, running the same drop, light release and renderer notification the
cancelled packet would have. Chunks within render distance stay, so Farsight's
extended view is unchanged. A chunk the server still counts as sent is never
dropped: the server would not resend it, and the area would stay empty until a
relog.

This is a leak fix, not an optimization, so Compare Mode does not affect it. The
VRO jar contains `META-INF/vro-features/farsight-chunk-bound`; VH Accelerator
sees that marker and leaves the bound to VRO, so turning this off leaves Farsight
unbounded. `/vro chunks farsight on|off|status` changes or reports it without a
restart; status shows the server radius, current bound, tracked chunks and how
many were forgotten in this level.

## Adaptive deferred chunk budgets

`chunk_updates.adaptive_budget_v2` defaults to `true` (experimental). Existing
explicit v2 off settings are preserved. The old `adaptive_budget` key is no
longer read. No opt-in is required on supported renderers. It requires validated
Embeddium 0.3.18/0.3.19 and effective deferred updates. `/vro chunks budget
on|off|status` controls it without restart; it is independent of index-only
sorting. Each renderer learns its own costs, with no saved calibration from
another PC. Compare Mode/off uses native scheduling and drains remaining native
results; switching off during a backlog can cause a catch-up hitch. Initial terrain,
loading workers and ready initial geometry use native throughput. Backlog pressure
or 250ms of continuous pending-class activity/observed result waiting also yields
to native behavior, followed by a 500ms recovery cooldown. Status distinguishes
`PACING` from `NATIVE FALLBACK` and includes pending requests and worker counts. See
[design, soft limits and test requirements](ADAPTIVE_CHUNK_BUDGET.md).

## Optional-mod ownership

Coexistence is decided during client startup. When an overlapping standalone
mod is detected, VRO leaves that feature to the standalone mod regardless of
the VRO config value. Restart after adding or removing an overlap mod.
# Embeddium/Rubidium transfers

The `[embeddium_transfers]` group contains independent restart-bound controls
for the eight VRO-owned renderer-fork corrections. VRO enables them only on
validated Embeddium `0.3.18` or Rubidium `0.5.6` class layouts. An unknown or
ambiguous renderer stack is blocked rather than guessed. Compare Mode disables
performance-sensitive transfers but retains the adjacent-position occlusion,
null-buffer, and shader-color correctness guards.

`embeddium_transfers.vertexBufferMaxRetainedMib` defaults to `16`. It bounds
the native capacity retained by each reusable chunk-build vertex buffer; a
larger one-off allocation is trimmed at the next build start and `destroy()`
continues to free it deterministically.

`embeddium_transfers.asyncArenaGrowthDivisor` defaults to `6`; a smaller value
reserves more VRAM headroom and reduces repeated arena compactions. The
increment is derived once from each arena's initial capacity, matching the
source fork's fixed-increment behavior instead of compounding with every
resize.
`asyncArenaMaxHeadroomMib` defaults to `64` and caps only speculative headroom,
never bytes required by the current upload. This control is independent of
vertex-buffer retention.

## Particle collision cache

`render_fast_paths.particle_collision_cache` defaults to `true`. Particle movement
collides with exactly the blocks and shapes vanilla would use, and vanilla's own
collision response is applied, but each block position is read once per particle
tick instead of once per particle. Every particle still collides; nothing is
skipped or culled. The cache lives only for one `ParticleEngine.tick`, when no
block can change. It replaces the earlier opt-in empty-section proof
(`particle_empty_section_collision`, no longer read) and yields to Particle Core
and Flerovium.

`/vro particles collision on|off` toggles it. `/vro particles collision verify on`
also runs vanilla collision for every particle, uses vanilla's result, and counts
mismatches in `/vro particles status` (the first ten are logged). Verification
costs more than either path, so it is not saved and is off after a restart.

## Exact per-particle and frame fast paths

All default to `true`, are disabled by Compare Mode, and keep vanilla's results;
none of them removes, skips or hides a particle.

| Key | What it avoids |
| --- | --- |
| `render_fast_paths.particle_tick_compaction` | One array shift per dead particle; dead particles are removed in one ordered pass |
| `render_fast_paths.particle_shared_random` | A new `Random` per particle and atomic draws (also constructor `Math.random()`, packet Gaussian spread and Vault Nova constructors) |
| `render_fast_paths.particle_provider_cache` | A registry key and hash lookup per particle spawn |
| `render_fast_paths.allocation_free_frustum` | 48 vector allocations per particle/entity/block-entity frustum test |

Particle tick compaction, the shared generator, the provider cache and the
collision cache yield to Particle Core, Flerovium and AsyncParticles. See
[Particle optimizations](PARTICLE_OPTIMIZATIONS.md) for the Nova-storm details.

## Astra development experiments

These additions are unreleased and default off pending in-game validation:

- `chunk_updates.sort_geometry_cache`: `/vro chunks sort_geometry on|off`.
  Requires VRO index-only sorting on the bytecode-validated Embeddium path.
  Reuses immutable triangle centers, capped at 16 MiB globally and 1 MiB per
  entry (256 entries maximum). `/vro chunks sorting status` reports cache reuse.

Compare Mode disables both. Queued work finishes safely. Neither option changes
the native initial/cached-terrain loading bypass, visible particle count, or
render distance. Existing `sophisticated_storage.quantity_text_cache` now also
retains bounded count glyph geometry with fresh color, pose and lighting; resource
reloads clear it. Particle census diagnostics now sample at 250 ms intervals.

### GPU entity models

- `render_fast_paths.gpu_entity_models` (default `true`): `/vro gpuentity on|off|status|stats|selftest`.
  Entity model part vertices are written by a compute shader into the vertex buffer vanilla has
  just uploaded, before vanilla's own draw. The arithmetic is bit-exact with vanilla and checked by
  a startup self-test. It turns itself off with an Oculus shader pack, on unsupported drivers, in
  Compare Mode, or when another mod changes `ModelPart` rendering. See `docs/GPU_ENTITY_MODELS.md`.

### Deferred-validation runtime batch

- `render_fast_paths.hud_text_geometry` (default `false`):
  `/vro experiments hud on|off|status`. Reuses exact final glyph vertices for
  unchanged, printable ASCII string draws inside the Forge HUD. Draws still happen
  every frame. Text, position, full pose, color/alpha, shadow, background, display
  mode and light are part of the key. Font identity/default-font selection,
  font-set reload/close, resource reload and window/GUI-scale changes invalidate
  reuse. At most 128 entries and 2 MiB of vertex payload, plus bounded metadata.
  Styled sequences, formatting codes (including obfuscation), bidirectional text,
  custom Font/FontSet/buffer-source subclasses and nonstandard vertex attributes
  fall back. Yields to detected Modern UI, ImmediatelyFast, Exordium and Smooth Font.
  No icon/bar geometry or gear/HUD state is cached. This is not a reduced-refresh HUD.
  Counters report accepted-path hits/misses and unsupported-output fallbacks;
  they do not count all text drawn by other paths or prove an FPS improvement.
- `embeddium_transfers.vertexBufferAdaptiveTrimming` (default `false`):
  `/vro experiments memory on|off|status`. Requires VRO's existing VRO-EMB-02
  ownership. Retains a 30-second demand peak, rounds retained demand upward, and
  reallocates only for savings of at least half the buffer. Aggregate pressure
  permits earlier trimming after three consecutive low-demand starts. A worker
  returning after 30 seconds idle can trim to its initial capacity. No other
  thread frees its buffer and no live result/upload storage is touched.
  `vertexBufferAggregateRetainedMib` (default `128`) is the advisory threshold for
  aggregate capacity above initial allocations, **not** a hard native-memory or
  VRAM cap. Trimming waits for each owner's next `start()`; workers that remain
  idle retain allocations until reuse or native `destroy()`. Required growth is
  never capped. Existing per-builder `vertexBufferMaxRetainedMib` still applies.

Both new switches honor Compare Mode and are hot; they do not require a new
client session when the corresponding mixins were installed at startup. Changing
the VRO-EMB-02 ownership/startup setting still requires a restart.

The diagnostic `indexed sections` count is affected buckets, not old source cells.
Snapshot publication costs allocations on source changes and requires measurement
in moving-light-heavy scenes; it is not a blanket performance guarantee.

All tests for this batch remain pending; compilation is not runtime validation.
Gear-cache invalidation and TTLs are intentionally unchanged.
