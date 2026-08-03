# The Vault Render Optimization

[![Minecraft](https://img.shields.io/badge/Minecraft-1.18.2-62b47a)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-40.3.11%2B-e04e39)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.18.2.html)
[![Environment](https://img.shields.io/badge/Environment-Client-4b8bbe)](#requirements-and-support)
[![License](https://img.shields.io/badge/License-AGPL--3.0--or--later-blue.svg)](LICENSE)
[![Release](https://img.shields.io/badge/Release-0.3.0-7b68ee)](docs/releases/0.3.0.md)

The Vault Render Optimization (VRO) is a client-side Minecraft Forge 1.18.2
mod that reduces repeated rendering and client simulation work in Vault
Hunters. It targets frame-time consistency in busy bases, Vault HUD and gear
rendering, particle-heavy scenes, and long sessions with repeated world or
dimension changes.

VRO does not remove visible effects, lower animation rates, change loot, or
modify server gameplay. The remote server does not need the mod.

## Highlights

- Caches expensive Vault gear, armor, tool-model, ability HUD, and event
  lookups that would otherwise repeat during rendering.
- Avoids client-only entity collision work in crowded mob-processing areas.
- Reuses safe particle-light and renderer lookups and skips renderer setup when
  there is nothing to draw.
- Releases stale Create Addition and Powah world references after unloads.
- Repairs two known client-only stale-state crashes without changing the
  server.
- Resolves the Vault map and Xaero's World Map `M`-key conflict.
- Provides immediate in-game Compare Mode for repeatable enabled/disabled
  benchmarks.
- Includes opt-in spatially indexed dynamic lighting for held and dropped
  items, luminous entities, and resource-defined block entities.
- Automatically yields overlapping work to Entity Collision FPS Fix,
  BadOptimizations, Particle Core, and Flerovium when present.

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
- skip particle renderer setup when every retained queue is empty;
- skip toast rendering when no toast is queued, visible, or transitioning;
- skip the completed tutorial's empty tick when no timed tutorial toast exists;
- skip debug rendering when no supported debug overlay is active;
- cache non-player entity and block-entity renderer lookups, rebuilding them
  after resource reloads.

These paths are independently configurable and do not intentionally change
visible output. Shader-sensitive lightmap and sky-color caching were rejected.

### Optional dynamic lights

VRO 0.3.1 includes a fresh dynamic-light engine that is disabled by default.
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

VRO also repairs two deterministic stale client states:

- Vault Integrations altar conduits receive their missing placement position
  before the client tick continues;
- Powah replaces an obsolete cable entry at the same position and continues
  its normal adjacent-network refresh.

These guards are client-only and remain active in Compare Mode.

### World-map key compatibility

The Vault's map key binding is active only while a client Vault is active. This
allows Xaero's World Map to receive `M` in the overworld while preserving the
Vault map inside Vault dimensions.

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
3. Place `vault_render_optimization.0.3.0.jar` in the instance's `mods`
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
| `/vro culling` | Reports vertical and horizontal terrain-culling settings. |
| `/vro culling vertical on\|off\|<distance>` | Changes vertical section culling immediately. |
| `/vro culling horizontal on\|off\|<distance>` | Changes horizontal section culling immediately. |
| `/vro lights` | Reports dynamic-light configuration and live engine counters. |
| `/vro lights on\|off` | Enables or disables VRO dynamic lights immediately. |
| `/vro lights entities on\|off` | Controls entity light sources. |
| `/vro lights block_entities on\|off` | Controls resource-defined block entity sources. |
| `/vro lights shaders on\|off` | Controls whether VRO lights remain active with shaders. |
| `/vro lights interval <1-20>` | Changes the independent per-source update interval. |

Compare Mode deliberately leaves crash guards, unloaded-world cleanup, and
map-key compatibility active. Those are correctness features, not benchmarked
performance changes. Commands are client-side and require no server permission.

## Configuration

VRO writes `config/vault_render_optimization-client.toml`. Its generic render
fast paths are enabled by default and can be disabled individually. Compare
Mode is also saved there.

The complete option and coexistence reference is in
[Configuration and commands](docs/CONFIGURATION.md).

## Compatibility and safety

- Optional integrations load only when their target mod is present.
- Equivalent mixins yield to Entity Collision FPS Fix, BadOptimizations,
  Particle Core, Flerovium, and Better Fps - Render Distance.
- Player renderer lookup behavior is not replaced.
- Renderer caches are discarded on resource reload.
- Particle subclasses with custom or full-bright lighting keep their own path.
- No asynchronous rendering or particle ticking is introduced.
- No framebuffers, shaders, chunk meshes, network packets, or server collision
  decisions are modified.

## Documentation

| Document | Purpose |
| --- | --- |
| [Installation](docs/INSTALLATION.md) | Install, upgrade, coexistence, removal, and reporting |
| [Configuration](docs/CONFIGURATION.md) | Every option, default, command, and immediate behavior |
| [Testing](docs/TESTING.md) | Compare Mode and repeatable benchmark procedure |
| [Performance validation](docs/PERFORMANCE_VALIDATION.md) | Four-client measured results and limitations |
| [Release notes 0.3.0](docs/releases/0.3.0.md) | Initial public release details |
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
learned-ability cache is adapted from Unobtanium work by `radimous`, and the
client collision behavior is adapted from CorgiTaco's CC0 Entity Collision FPS
Fix. Particle Core, BadOptimizations, and other projects informed independent
research and compatibility boundaries.

Read [CREDITS.md](CREDITS.md) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)
for exact revisions and relationships.

VRO is licensed under the [GNU Affero General Public License v3.0 or later](LICENSE).
No third-party mod jar, Vault Hunters source, or decompiled class is bundled.

Minecraft is a trademark of Microsoft. Vault Hunters belongs to its respective
authors. This independent project is not affiliated with Mojang, Microsoft,
Forge, Iskallia, or the credited projects.
