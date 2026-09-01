# Configuration and commands

VRO stores client settings in:

```text
config/vault_render_optimization-client.toml
```

The removable Mana Stealer visual prototype deliberately uses a separate file:

```text
config/vault_render_optimization-mana-stealer-client.toml
```

All release fast paths are enabled by default. Forge reloads changes made by
VRO's command immediately. For manual file edits, stop Minecraft first.

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

The Mana Stealer visual prototype is runtime-owned rather than startup-owned.
Compare Mode immediately restores Vault's original dust and ground sigil; its
already-live composite orbs finish without replenishment.

## Mana Stealer visual prototype

| Key | Default | Purpose |
| --- | ---: | --- |
| `mana_stealer_visual_prototype.enabled` | `true` | Replaces the legacy dust with composite inward-moving orbs |
| `mana_stealer_visual_prototype.replace_ground_sigil` | `true` | Hides the legacy line sigil while the replacement is active |
| `mana_stealer_visual_prototype.all_population` | `80` | Live object target on All particles |
| `mana_stealer_visual_prototype.decreased_population` | `52` | Live object target on Decreased particles |
| `mana_stealer_visual_prototype.minimal_population` | `20` | Live object target on Minimal particles |
| `mana_stealer_visual_prototype.max_spawns_per_tick` | `8` | Maximum population deficit filled per tick |
| `mana_stealer_visual_prototype.minimum_speed` | `0.1625` | Minimum inward speed in blocks/tick |
| `mana_stealer_visual_prototype.maximum_speed` | `0.26` | Maximum inward speed in blocks/tick |
| `mana_stealer_visual_prototype.outer_diameter` | `0.3` | Reference outer diameter in blocks; rendered from 250% to 12.5% |
| `mana_stealer_visual_prototype.inner_diameter_ratio` | `0.46` | Inner navy diameter divided by outer diameter |
| `mana_stealer_visual_prototype.drain_stream_enabled` | `true` | Shows a batched blue-orb stream from affected players to the trap |
| `mana_stealer_visual_prototype.drain_stream_density` | `6.5` | All-particle visible stream orbs per block |
| `mana_stealer_visual_prototype.drain_stream_minimum_orbs` | `6` | Minimum visible stream population |
| `mana_stealer_visual_prototype.drain_stream_maximum_orbs` | `48` | Per-player/per-trap All-particle stream cap |
| `mana_stealer_visual_prototype.drain_stream_speed` | `0.32` | Stream travel speed in blocks/tick |
| `mana_stealer_visual_prototype.drain_stream_orb_diameter` | `0.22` | Stream-orb reference diameter in blocks |
| `mana_stealer_visual_prototype.drain_stream_spread` | `0.18` | Maximum radial stream spread in blocks |

This development module changes one Vault trap's visuals but not its gameplay,
particle range, configured radius, duration, mana drain, server targeting, or
sound. See [Mana Stealer visual prototype](MANA_STEALER_VISUAL_PROTOTYPE.md)
for the exact removal boundary and required in-game checks.

Use `/vro mana_stealer preview <ticks> [x y z]` to render a client-only
radius-6 visual/audio simulation at the targeted block or an exact coordinate.
Use `/vro mana_stealer preview stop` to stop replenishing it. The command never
creates a trap entity or applies mana drain.
Use `/vro mana_stealer stream on|off` to hot-toggle the player-to-trap stream.
It appears only inside the synchronized spherical radius and is a client visual,
not confirmation that mana was removed on a specific server tick.

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
| `/vro lights` | Shows configuration, active state, source counts, rebuilds, and loaded definitions |
| `/vro lights on` | Enables VRO dynamic lights immediately |
| `/vro lights off` | Disables VRO dynamic lights and clears retained source state |
| `/vro lights entities on\|off` | Controls all entity-based light sources |
| `/vro lights block_entities on\|off` | Controls resource-defined block entity sources |
| `/vro lights shaders on\|off` | Controls operation while Oculus shaders are active |
| `/vro lights interval <1-20>` | Saves the independent per-source update interval |
| `/vro create` | Shows Create/Flywheel and loaded-contraption diagnostics |
| `/vro create status` | Shows the same Create diagnostics explicitly |

These are client commands in multiplayer. They require no server permission
and work even when the remote server does not have VRO.

## Dynamic lights

| Key | Default | Purpose |
| --- | --- | --- |
| `dynamic_lights.enabled` | `false` | Enables VRO's client-only dynamic-light engine |
| `dynamic_lights.entities` | `true` | Allows held/dropped items, burning entities, TNT, and supported entities/projectiles to emit light |
| `dynamic_lights.block_entities` | `true` | Allows resource-defined block entity types to emit light |
| `dynamic_lights.enable_with_shaders` | `false` | Keeps VRO lights active while an Oculus shader pack is active |
| `dynamic_lights.update_interval_ticks` | `1` | Per-source update interval from 1 to 20 client ticks |

The master switch is intentionally off by default. Dynamic light is visual
only and does not change server light levels, mob spawning, crops, or chunk
storage. Compare Mode pauses it. Dynamic Lights Reforged owns the feature when
that mod is installed, regardless of these settings.

See [Dynamic lights](DYNAMIC_LIGHTS.md) for source definitions and diagnostics.

## Create rendering

| Key | Default | Purpose |
| --- | --- | --- |
| `create_rendering.skip_empty_contraption_buffer_flush` | `true` | Skips Create's shared-buffer flush when a contraption has no special block entities to submit |
| `create_rendering.contraption_block_entity_culling` | `true` | Frustum-culls non-instanced special block entities inside contraptions |
| `create_rendering.contraption_actor_culling` | `true` | Frustum-culls movement actors inside contraptions |
| `create_rendering.sectioned_contraption_meshes` | `true` | Splits large contraption geometry into local 16-block sections for frustum culling |
| `create_rendering.sectioned_mesh_block_threshold` | `512` | Minimum rendered-block count for sectioned contraption meshes |
| `create_rendering.smart_machinery_render_bounds` | `true` | Uses tighter directional bounds for supported Create machinery |
| `create_rendering.auto_enable_flywheel_instancing` | `true` | Restores Flywheel's upstream-default instancing backend when a pack configures it as `OFF`; unsupported hardware and integration failures retain the fallback renderer |

Sectioned meshes preserve Create's original block models, textures, lighting,
render layers, and shader program. The feature is not LOD: it does not reduce
detail, substitute generic blocks, or hide geometry based on distance. A large
contraption may take slightly longer to build its render data once because it
creates multiple cached mesh sections; the intended gain is lower recurring
draw work when only part of the structure is on screen.

The smart-bounds path covers belts, mechanical arms, deployers, portable
storage interfaces, and mechanical rollers in Create 0.5.1.i. All Create paths
are optional and are omitted automatically when Create is absent.

## Optional-mod ownership

Coexistence is decided during client startup. When an overlapping standalone
mod is detected, VRO leaves that feature to the standalone mod regardless of
the VRO config value. Restart after adding or removing an overlap mod.
