# The Vault Render Optimization

[![Minecraft](https://img.shields.io/badge/Minecraft-1.18.2-62b47a)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-40.3.11%2B-e04e39)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.18.2.html)
[![Environment](https://img.shields.io/badge/Environment-Client-4b8bbe)](#requirements-and-support)
[![License](https://img.shields.io/badge/License-AGPL--3.0--or--later-blue.svg)](LICENSE)
[![Release](https://img.shields.io/badge/Release-0.4.1--candidate-7b68ee)](docs/releases/0.4.1.md)

The Vault Render Optimization (VRO) is a client-side Minecraft Forge 1.18.2
mod that reduces repeated rendering and client simulation work in Vault
Hunters. It targets frame-time consistency in busy bases, Vault HUD and gear
rendering, particle-heavy scenes, and long sessions with repeated world or
dimension changes.

VRO does not remove visible effects, lower animation rates, change loot, or
modify server gameplay. The current development branch has one clearly
documented exception for local testing: the removable Mana Stealer prototype
replaces that trap's legacy dust and optional line sigil with a new visual of
bounded composite orbs. The remote server does not need the mod.

## Highlights

- Dynamically paces deferred chunk builds/uploads on validated Embeddium using
  each machine's measured costs, with queue backpressure and hot controls.
  In-game evaluation pending; see [adaptive budgets](docs/ADAPTIVE_CHUNK_BUDGET.md).

- Avoids copying and re-uploading unchanged terrain vertices for translucent
  sorting on validated Embeddium builds. Independently switchable; see
  [index-only sorting](docs/INDEX_ONLY_SORTING.md).

- Reduces blocking chunk-update stalls through default-on native asynchronous
  scheduling, with or without a supported renderer mod. No other mod's settings
  need changing. Visible block updates can lag under load; see
  [chunk-update controls](docs/CHUNK_UPDATE_DEFERRAL.md).
- Caches expensive Vault gear, armor, tool-model, ability HUD, and event
  lookups that would otherwise repeat during rendering.
- Avoids client-only entity collision work in crowded mob-processing areas.
- Builds ordinary particle billboards from the camera basis, reuses packed
  Rubidium/Embeddium output when available, shares safe same-tick light
  results, and skips renderer setup when there is nothing to draw.
- Includes an isolated development prototype that replaces the Mana Stealer
  trap's flat dust presentation with bounded inward-moving composite orbs for
  local visual and performance testing.
- Reduces retained block-state and baked-model memory beyond FerriteCore 4.2.2.
- Adds eleven ModernFix-derived render/model improvements with per-feature
  ownership, compatibility gates, and safe coexistence with ModernFix and the
  temporary VH Accelerator overlap.
- Carries eight guarded Embeddium/Rubidium renderer corrections for chunk
  rebuild scheduling, bounded native buffers, arena growth, custom face
  hiding, fluid lighting, shader color, vertex sinks, and CodeChickenLib.
- Skips terrain sections beyond a configurable vertical render range without
  changing chunk loading or Distant Horizons storage.
- Releases stale Create Addition and Powah world references after unloads.
- Frustum-culls large Create contraptions in 16-block mesh sections and avoids
  off-screen special renderer work without reducing model detail.
- Keeps Flywheel's GPU instancing path available for large Create contraptions
  when supported public Oculus shaders are active.
- Uses shader-pack-provided `gbuffers_flw` and `shadow_flw` programs when
  available, with automatic fallback to VRO's generated compatibility path.
- Defers unused Vault Loot Beams tooltip work and clears its per-entity cache
  when a world unloads.
- Removes iSpawner's per-frame stream/list allocations and allows ordinary
  off-screen spawner displays to be frustum culled.
- Prevents Minecraft's shared empty item stack from retaining a dropped-item
  entity.
- Repairs two known client-only stale-state crashes without changing the
  server.
- Resolves the Vault map and Xaero's World Map `M`-key conflict.
- Checks VRO's GitHub manifest asynchronously and shows coordinated menu and
  in-world update notices with a fixed CurseForge download link.
- Provides immediate in-game Compare Mode for repeatable enabled/disabled
  benchmarks.
- Includes opt-in spatially indexed dynamic lighting for held and dropped
  items, luminous entities, and resource-defined block entities.
- Automatically yields overlapping work to Entity Collision FPS Fix,
  BadOptimizations, Particle Core, Flerovium, Better Fps - Render Distance,
  and Dynamic Lights Reforged when present.

## What VRO improves

### Vault gear and HUD rendering

VRO keeps the results of expensive Vault gear reads close to the item being
rendered and refreshes them when the gear data changes or the one-second safety
window expires. This covers:

- armor durability used by HUD and inventory overlays;
- whether identified Vault armor can take damage;
- armor texture and model selection;
- identified Vault tool model selection;
- the learned ability list used by Vault HUD elements;
- damage-number formatting without allocating a new formatter per number.

Unidentified tools retain Vault's animated preview behavior. Gear caches use
weak item references and are cleared when Compare Mode changes.

### Vault render event dispatch

Vault's high-frequency client event dispatcher normally rebuilds and copies
its priority listener tree every time certain render events fire. VRO retains
an immutable ordered snapshot for biome colors, dimension effects, ambient
light, and end-of-level rendering, and invalidates it whenever listeners are
registered or released. Other Vault events remain untouched.

This work is especially relevant to chunk rebuild tint and lighting calls.

### Crowded entity areas

The server is authoritative for movement, pushing, and suffocation. VRO skips
the duplicate client-side wall checks and living-entity push calculation. This
reduces CPU work around dense mob farms, spawners, and rapid kill systems.

The feature disables itself when Entity Collision FPS Fix is installed.

### Generic render fast paths

VRO also applies conservative optimizations outside Vault-specific code:

- reuse a particle's block-light value while it remains in the same block and
  client tick;
- share bounded block-light results among particles occupying the same block
  during that tick;
- build ordinary particle corners directly from the camera's left/up basis,
  avoiding four quaternion rotations while retaining roll and visible output;
- skip particle renderer setup when every retained queue is empty;
- skip toast rendering when no toast is queued, visible, or transitioning;
- skip the completed tutorial's empty tick when no timed tutorial toast exists;
- skip debug rendering when no supported debug overlay is active;
- cache non-player entity and block-entity renderer lookups, rebuilding them
  after resource reloads.
- select iSpawner display items without temporary streams or lists while
  preserving its original rotation interval, copied stack, and view distance;
- defer Vault Loot Beams tooltip creation until an item is actually queried.

These paths are independently configurable and do not intentionally change
visible output. Shader-sensitive lightmap and sky-color caching were rejected.

### ModernFix render and model backports

VRO carries selected later ModernFix improvements adapted to Forge 1.18.2:
allocation-light chunk meshing; duplicate BufferBuilder protection;
reload-safe entity-model cube compaction; bounded profile-texture hashing;
multipart selector, model-variant, transformation, and OBJ cache improvements;
guarded STB texture-atlas stitching; and concurrency corrections for Forge
model data and validated CTM metadata.

Each feature is restart-bound and independently configurable. VRO yields when
ModernFix reports the exact implementation active. During the migration from
VH Accelerator, VRO also yields only when VHA still contains that exact class,
preventing duplicate mixins without permanently coupling ownership to VHA's
mod ID. Compatibility exclusions preserve Fluidlogged, Isometric Renders,
Cracker's Wither Storm Mod, legacy Rubidium, Embeddium, and validated CTM
behavior. Use `/vro backports` for the selected owner and reason.

Exact source commits, upstream copyrights, license terms, and behavioral
adaptations are documented in
[ModernFix render-backport provenance](docs/MODERNFIX_RENDER_BACKPORTS.md).

### Embeddium and Rubidium renderer corrections

VRO owns eight independently gated corrections transferred from the supported
Embeddium stability fork: adjacent-position face hiding, null-buffer vertex
sinks, direct CodeChickenLib renderer lookup, bounded vertex-buffer retention,
preemptive but capped arena growth, cached smooth lighting for non-luminous
fluids, chunk-layer shader-color reset, and equivalent chunk-rebuild
coalescing.

The corrections target stock Embeddium `0.3.18+mc1.18.2` and Rubidium `0.5.6`.
Unknown or ambiguous renderer layouts fail closed. Compare Mode yields the
performance-sensitive paths while retaining narrow correctness fixes. The
source implementations were removed from the private Embeddium fork after VRO
completed its single-owner validation, so a supported stack applies each path
only once. See the
[ownership-transfer ledger](docs/EMBEDDIUM_OWNERSHIP_TRANSFER.md) for the exact
features, gates, tests, and accepted compatibility evidence.

### Memory and terrain distance

VRO compacts simple baked-model face lists and shares identical block-state
`faceSturdy` arrays. These additions target data that FerriteCore 4.2.2 does
not already compact.

Vertical terrain-section culling is enabled by default at 12 sections above
and below the camera. Horizontal culling is independently configurable and
disabled by default. Both paths affect terrain drawing only, support vanilla
and Embeddium/Rubidium renderers, and yield to Better Fps - Render Distance.

### Optional dynamic lights

VRO includes an optional dynamic-light engine that is disabled by default.
When enabled, luminous entities and items light nearby terrain without changing
server light data. Sources are indexed by 16-block cells, carry independent
update schedules, and submit one deduplicated set of terrain rebuilds per tick.

Vanilla luminous block items work automatically. Additional item and block
entity definitions can be supplied by resource packs under
`assets/<namespace>/vro_dynamic_lights/*.json`. Entity sources and block entity
sources can be controlled separately. Dynamic lights pause while Oculus shaders
are active unless shader participation is explicitly enabled.

VRO leaves this entire subsystem inactive when Dynamic Lights Reforged is
installed. See [Dynamic lights](docs/DYNAMIC_LIGHTS.md) for the resource format,
commands, diagnostics, and behavior boundaries.

### Long-session cleanup and crash recovery

On world unload, VRO removes the exact unloaded world from Create Addition's
energy-network map and Powah's cable-network map. This prevents old client or
integrated-server levels from remaining reachable through repeated dimension
changes and reconnects.

The same lifecycle pass clears Vault Loot Beams tooltip entries. VRO also
prevents Minecraft's global empty `ItemStack` singleton from retaining an
`ItemEntity` reference after synchronized dropped-item updates.

VRO also repairs two deterministic stale client states:

- Vault Integrations altar conduits receive their missing placement position
  before the client tick continues;
- Powah replaces an obsolete cable entry at the same position and continues
  its normal adjacent-network refresh. Delayed unloads remove a cable only
  when that exact cable is still current, preserving newer replacements and
  allowing the surrounding chunk update to finish;
- Xaero's World Map validates and writes prepared cache regions under the same
  region lock. A queued save invalidated by newer map work is cleaned up and
  skipped instead of crashing the client.

These guards are client-only and remain active in Compare Mode.

### Create contraption rendering

Create 0.5.1.i normally submits a glued contraption as one large mesh. If any
part of that contraption is visible, the complete mesh remains eligible to
draw. VRO divides contraptions containing at least 512 rendered blocks into
local 16-block sections and frustum-tests those sections independently. It
uses Create's existing models, textures, lighting, render layers, and Flywheel
shader path; no blocks are simplified or replaced with lower-detail models.

VRO also frustum-tests non-instanced special block entities and movement actors
inside a contraption, skips an empty shared-buffer flush, and narrows oversized
render bounds for supported stationary Create machinery. Compare Mode reloads
Create's world renderers so an enabled/disabled comparison does not reuse a
mesh built for the other condition.

Use `/vro create status` to report Flywheel's backend, loaded contraption and
block counts, and the previous frame's section, actor, and block-entity culling
counters. Flywheel instancing remains Create's preferred backend; VRO does not
replace it. VRO restores Flywheel's upstream-default `INSTANCING` backend when
a pack explicitly configures it as `OFF`. Unsupported hardware and shader
integration failures retain Flywheel's safe fallback renderer. Set
`create_rendering.auto_enable_flywheel_instancing=false` to preserve a manually
selected `OFF` backend.

With Oculus 1.6.x and Flywheel 0.6.11, VRO can retain that instancing backend
while shaders are active. Use `/vro create shader_compat on|off|status` to
control or inspect the path. Unsupported or incomplete render stacks remain a
clean no-op. Recovery and acceptance testing are documented in
[Create shader instancing compatibility](docs/FLYWHEEL_SHADER_COMPAT.md).

### World-map key compatibility

The Vault's map key binding is active only while a client Vault is active. This
allows Xaero's World Map to receive `M` in the overworld while preserving the
Vault map inside Vault dimensions.

### Update notices

VRO can check its repository-owned Forge update manifest without blocking
startup or rendering. An allowed update appears as a coordinated row on the
main menu and may produce an occasional in-world reminder whose clickable link
is fixed to VRO's official CurseForge page. VRO never downloads or installs an
update.

Checks are enabled by default, while the displayed update types default to
`CRITICAL`. Select `ALL` explicitly to include ordinary release notices. The
request uses short timeouts, rejects oversized responses, and fails closed on
network, HTTP, or manifest errors. Each JVM can advance the reminder cadence
once and deliver one chat reminder, so reconnects, dimensions, and server
transfers cannot repeat it during the same client launch.

## Measured performance

A deterministic 40-trial campaign tested five enabled and five disabled runs
in each of four Vault Hunters clients. Every paired test used the same client,
world, route, render distance, time, weather, focus, shader state, and warm-up.

Across the four clients, the unweighted mean improvements were:

| Metric | VRO improvement |
| --- | ---: |
| Average FPS | 5.35% |
| 1% low FPS | 30.66% |
| 0.1% low FPS | 35.07% |
| p99 frame time | 13.94% |
| Average client CPU time | 4.49% |

VRO reduced frames longer than 16.7 ms in every tested client. Average-FPS
results varied by pack, so the strongest supported claim is better frame-time
consistency rather than a guaranteed percentage on every machine.

See [Performance validation](docs/PERFORMANCE_VALIDATION.md) for the complete
pack-level summary and test controls.

## Requirements and support

| Component | Supported baseline |
| --- | --- |
| Minecraft | `1.18.2` |
| Forge | `40.3.11+` in the Forge 40.x line |
| Java bytecode | Java 17 |
| Environment | Client |
| Vault Hunters official | `3.21.6.6884` |
| Vault Hunters Remastered | `20.0.3-remastered` |
| Wolds Vaults | Pack `0.32.2` / Vault `3.21.5.6573` |
| Custom compatibility target | Vault `3.21.62` |

Vault Hunters is an optional integration rather than a hard loading
dependency. Generic optimizations remain available when Vault is absent.
Supported versions are tested compatibility baselines, not permission to mix
different pack files together.

## Installation

1. Stop Minecraft.
2. Remove or disable every older VRO jar.
3. Place `vault_render_optimization.0.4.1.jar` in the instance's `mods`
   directory.
4. Keep only one active VRO jar.
5. Remove Entity Collision FPS Fix only if you want VRO to own that same
   feature. Keeping it installed is safe because VRO yields automatically.

No server installation is required. See [Installation](docs/INSTALLATION.md)
for upgrades, removal, optional-mod coexistence, and issue isolation.

## Commands

| Command | Result |
| --- | --- |
| `/vro` | Reports the current comparison state. |
| `/vro compare on` | Saves and immediately disables VRO performance optimizations. |
| `/vro compare off` | Saves and immediately enables configured VRO optimizations. |
| `/vro compare status` | Reports whether Compare Mode is active. |
| `/vro updates` | Reports whether update checks are enabled and which update types may be shown. |
| `/vro updates on\|off` | Enables or disables update checks immediately and saves the setting. |
| `/vro updates critical\|all` | Shows only critical updates, or opts into all update notices. |
| `/vro particles` | Reports particle ownership, hot options, and diagnostics. |
| `/vro chunks status` | Reports the selected chunk-update backend and last observed native preference. |
| `/vro chunks defer on\|off` | Controls native asynchronous chunk scheduling without changing another mod's settings. |
| `/vro chunks sorting on\|off\|status` | Controls index-only transparency updates and reports avoided vertex copies/uploads. |
| `/vro chunks budget on\|off\|status` | Controls per-machine adaptive deferred budgets and reports upload cost, queue pressure and waiting time. |
| `/vro particles billboards on\|off` | Hot-enables or disables VRO's camera-basis billboard geometry. |
| `/vro particles owner auto\|renderer\|vro` | Hot-selects VRO or renderer ownership; `renderer` yields to Rubidium/Embeddium when present. |
| `/vro particles shared_light on\|off` | Hot-controls the bounded same-tick shared light cache. |
| `/vro particles diagnostics on\|off\|reset` | Controls queue, timing, writer, and light-cache measurement. |
| `/vro mana_stealer` | Reports the isolated Mana Stealer visual prototype state and quality targets. |
| `/vro mana_stealer on\|off` | Hot-enables or disables the prototype particle replacement. |
| `/vro mana_stealer preview <ticks> [x y z]` | Simulates the complete client-only visual/audio effect at the targeted block or an explicit center. |
| `/vro mana_stealer preview stop` | Stops preview replenishment; already-live orbs finish naturally. |
| `/vro mana_stealer sigil on\|off` | Retains or replaces Vault's legacy ground sigil. |
| `/vro mana_stealer stream on\|off` | Hot-controls the affected-player blue-orb stream to the trap. |
| `/vro culling` | Reports vertical and horizontal terrain-culling settings. |
| `/vro culling vertical on\|off\|<distance>` | Changes vertical section culling immediately. |
| `/vro culling horizontal on\|off\|<distance>` | Changes horizontal section culling immediately. |
| `/vro lights` | Reports dynamic-light configuration and live engine counters. |
| `/vro lights on\|off` | Enables or disables VRO dynamic lights immediately. |
| `/vro lights entities on\|off` | Controls entity light sources. |
| `/vro lights block_entities on\|off` | Controls resource-defined block entity sources. |
| `/vro lights shaders on\|off` | Controls whether VRO lights remain active with shaders. |
| `/vro lights interval <1-20>` | Changes the independent per-source update interval. |
| `/vro create status` | Reports Create/Flywheel state and contraption-culling counters. |
| `/vro create shader_compat status` | Reports whether Flywheel shader compatibility is configured, active, or using its fallback. |
| `/vro create shader_compat on\|off` | Enables or disables Flywheel shader compatibility and immediately rebuilds Create renderers. |

Compare Mode deliberately leaves crash guards, unloaded-world cleanup, and
map-key compatibility active. Those are correctness features, not benchmarked
performance changes. Commands are client-side and require no server permission.

## Configuration

VRO writes `config/vault_render_optimization-client.toml`. Its generic render
fast paths are enabled by default and can be disabled individually. Compare
Mode is also saved there.

Update checks are enabled by default, but notices default to critical updates
only. The request is asynchronous, bounded, and fails closed; VRO never
downloads or installs a mod update automatically. Normal notices require
`/vro updates all` or the matching client-config value.

The complete option and coexistence reference is in
[Configuration and commands](docs/CONFIGURATION.md).

## Compatibility and safety

- Optional integrations load only when their target mod is present.
- Equivalent mixins yield to Entity Collision FPS Fix, BadOptimizations,
  Particle Core, Flerovium, Better Fps - Render Distance, and Dynamic Lights
  Reforged.
- Player renderer lookup behavior is not replaced.
- Renderer caches are discarded on resource reload.
- Particle subclasses with custom or full-bright lighting keep their own path.
- Ordinary particle billboard options and ownership can be changed without a
  client restart. Flerovium retains ownership when installed.
- No asynchronous rendering or particle ticking is introduced.
- Network packets, server gameplay, and server collision decisions are not
  modified. Create contraption meshes may be divided into equivalent render
  sections. On the strictly supported Oculus/Flywheel stack, VRO adapts
  Flywheel-generated shaders so Create can retain GPU instancing.

## Documentation

| Document | Purpose |
| --- | --- |
| [Installation](docs/INSTALLATION.md) | Install, upgrade, coexistence, removal, and reporting |
| [Configuration](docs/CONFIGURATION.md) | Every option, default, command, and immediate behavior |
| [Particle optimizations](docs/PARTICLE_OPTIMIZATIONS.md) | Billboard ownership, light caches, diagnostics, and safety boundaries |
| [Mana Stealer prototype](docs/MANA_STEALER_VISUAL_PROTOTYPE.md) | Removable visual-replacement module, tuning, and validation boundary |
| [Testing](docs/TESTING.md) | Compare Mode and repeatable benchmark procedure |
| [Performance validation](docs/PERFORMANCE_VALIDATION.md) | Four-client measured results and limitations |
| [Release notes 0.4.1](docs/releases/0.4.1.md) | Current release candidate: renderer, model, particle, and Mana Stealer work |
| [Release notes 0.4.0](docs/releases/0.4.0.md) | Previous published release with configurable update notices |
| [Release notes 0.3.5](docs/releases/0.3.5.md) | Previous release with dedicated Flywheel shader-pack programs |
| [Release notes 0.3.4](docs/releases/0.3.4.md) | Create contraption and Flywheel shader rendering |
| [Release notes 0.3.3](docs/releases/0.3.3.md) | Dynamic-light crash correction |
| [Release notes 0.3.2](docs/releases/0.3.2.md) | Expanded 0.3.2 release details |
| [Release notes 0.3.0](docs/releases/0.3.0.md) | Initial release history |
| [Changelog](CHANGELOG.md) | Version-to-version changes |
| [Credits](CREDITS.md) | Adapted code, design research, and compatibility attribution |
| [Third-party notices](THIRD_PARTY_NOTICES.md) | Exact shipped provenance and licenses |
| [Source provenance audit](docs/SOURCE_PROVENANCE_AUDIT.md) | Feature-by-feature copied, adapted, and original classification |
| [Research ledger](docs/PERFORMANCE_BACKPORT_RESEARCH.md) | Considered, rejected, and future candidates |

## Building

Requirements:

- JDK 17
- one supported local Vault Hunters jar for compile-only API verification

Build and run the complete installed-pack compatibility matrix:

```powershell
.\scripts\build-pack-compatibility.ps1
```

The reobfuscated release jar is copied to `libs/`. The build does not bundle
Vault Hunters or any optional compatibility mod.

Activate the repository's public identity pre-push protection once in each new
working copy:

```powershell
.\scripts\install-public-identity-hook.ps1
```

The same scan runs in CI and must pass before any public push or upload.

## Credits and license

VRO was developed by [HoYin1600p](https://github.com/HoYin1600p). The
learned-ability cache is adapted from Unobtanium work by `radimous`, the client
collision behavior is adapted from CorgiTaco's CC0 Entity Collision FPS Fix,
and two post-4.2.2 memory reductions are adapted from FerriteCore. Particle
Core, BadOptimizations, Better Fps - Render Distance, Dynamic Lights Reforged,
and other projects informed independently written features and compatibility
boundaries.

Read [CREDITS.md](CREDITS.md) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)
for exact revisions and relationships.

VRO is licensed under the [GNU Affero General Public License v3.0 or later](LICENSE).
No third-party mod jar, Vault Hunters source, or decompiled class is bundled.

Minecraft is a trademark of Microsoft. Vault Hunters belongs to its respective
authors. This independent project is not affiliated with Mojang, Microsoft,
Forge, Iskallia, or the credited projects.
