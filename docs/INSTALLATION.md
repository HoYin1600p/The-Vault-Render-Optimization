# Installation

## Requirements

- Minecraft `1.18.2`
- Forge `40.3.11` or newer in the Forge 40.x line
- Physical client

Vault Hunters is supported but not a hard loading dependency. The generic
render fast paths can load without it.

## Install

1. Stop Minecraft completely.
2. Open the instance's `mods` directory.
3. Remove or disable every older VRO jar.
4. Place `vault_render_optimization.0.4.0.jar` in the directory.
5. Confirm that only one VRO jar ends in `.jar`.
6. Launch the client. VRO creates
   `config/vault_render_optimization-client.toml` with release defaults.

Update checks are enabled by default and display only critical notices unless
the user explicitly selects all update types. Checks are asynchronous and
never download or install files. VRO stores the reminder cadence separately in
`config/vault_render_optimization-update-notice-state.json`.

The remote server does not need VRO. Do not install the sources jar as a mod.

## Upgrade

Stop Minecraft before replacing the jar. VRO 0.4.0 requires no cache deletion
or configuration reset. New configuration keys receive their documented
defaults when absent.

## Optional overlap

VRO automatically avoids duplicate mixins when these mods are present:

| Installed mod | VRO behavior |
| --- | --- |
| Entity Collision FPS Fix | Leaves client collision work to the standalone mod |
| BadOptimizations | Leaves equivalent empty-work and renderer lookup paths to it |
| Particle Core | Leaves particle-light caching to it |
| Flerovium | Leaves particle-light caching and ordinary billboard rendering to it |
| Better Fps - Render Distance | Leaves terrain-distance culling to it |
| Dynamic Lights Reforged | Leaves all dynamic-light behavior to it |

It is safe to retain those mods during migration. Removing a standalone mod
allows VRO's equivalent path to activate on the next launch.

## Removal

1. Stop Minecraft.
2. Remove or disable the VRO jar.
3. Optionally remove `config/vault_render_optimization-client.toml`.
4. Optionally remove
   `config/vault_render_optimization-update-notice-state.json`.

VRO does not alter world saves or server data. Its client configuration and
update-reminder cadence file are its only persistent state.

## Issue isolation

Use `/vro compare on` first when investigating a performance or visual issue.
It disables VRO's performance paths immediately while retaining crash guards,
world cleanup, and map-key compatibility.

For an issue report, include:

- Minecraft, Forge, Vault Hunters, and VRO versions;
- the complete latest log and crash report, if any;
- whether Compare Mode changes the issue;
- whether shaders, Distant Horizons, or a resource pack are active;
- the names and versions of Entity Collision FPS Fix, BadOptimizations,
  Particle Core, Flerovium, Better Fps - Render Distance, or Dynamic Lights
  Reforged if installed;
- exact reproduction steps and screenshots or video for visual issues.
