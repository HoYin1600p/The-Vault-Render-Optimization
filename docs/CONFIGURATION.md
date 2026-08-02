# Configuration and commands

VRO stores client settings in:

```text
config/vault_render_optimization-client.toml
```

All release fast paths are enabled by default. Forge reloads changes made by
VRO's command immediately. For manual file edits, stop Minecraft first.

## Compare Mode

| Key | Default | Purpose |
| --- | --- | --- |
| `benchmark.compare_mode` | `false` | Disables all VRO performance optimizations for comparison |

Compare Mode does not disable client crash guards, unloaded-world cleanup, or
Vault/Xaero map-key compatibility. Those behaviors are intentionally kept out
of performance comparisons.

Changing Compare Mode clears Vault gear and tool caches so the next frame does
not reuse data created under the previous state.

## Render fast paths

| Key | Default | Purpose |
| --- | --- | --- |
| `render_fast_paths.particle_light_cache` | `true` | Reuses unchanged particle light during one client tick |
| `render_fast_paths.skip_empty_particle_render` | `true` | Avoids particle renderer setup when all queues are empty |
| `render_fast_paths.skip_empty_toast_render` | `true` | Avoids toast renderer work when no toast is active |
| `render_fast_paths.skip_empty_debug_render` | `true` | Avoids debug renderer work when supported overlays are inactive |

Particle lighting subclasses that replace the normal light method retain their
own behavior. VRO does not reduce particle counts or move particle work to
another thread.

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

## Commands

| Command | Result |
| --- | --- |
| `/vro` | Shows Compare Mode state |
| `/vro compare` | Shows Compare Mode state |
| `/vro compare status` | Shows Compare Mode state |
| `/vro compare on` | Saves and immediately disables VRO performance paths |
| `/vro compare off` | Saves and immediately enables configured performance paths |

These are client commands in multiplayer. They require no server permission
and work even when the remote server does not have VRO.

## Optional-mod ownership

Coexistence is decided during client startup. When an overlapping standalone
mod is detected, VRO leaves that feature to the standalone mod regardless of
the VRO config value. Restart after adding or removing an overlap mod.
