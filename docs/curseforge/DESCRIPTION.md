# The Vault Render Optimization

**Smoother Vault Hunters rendering, especially in busy bases and
particle-heavy scenes.**

The Vault Render Optimization (VRO) is a client-side performance and stability
mod for Minecraft 1.18.2 Forge. It reduces repeated Vault gear, HUD, event,
collision, particle, and renderer work without lowering animation rates or
removing visible effects.

The remote server does not need the mod.

## What it improves

- Vault armor durability, texture, model, tool, ability HUD, and damage-number
  rendering.
- High-frequency Vault render events used by chunk colors, lighting, dimension
  effects, and end-of-level rendering.
- Client CPU work around crowded mob farms and rapid kill systems.
- Repeated particle-light and entity/block-entity renderer lookups.
- Empty particle, toast, tutorial, and debug renderer work.
- Long-session world cleanup for Create Addition and Powah.

VRO also includes client-only recovery for two known stale-state crashes and
resolves the Vault/Xaero world-map `M`-key conflict.

## Measured results

A controlled 40-trial campaign used five enabled and five disabled runs in
each of four Vault Hunters clients. Across those clients, VRO averaged:

- 5.35% higher average FPS;
- 30.66% better 1% lows;
- 35.07% better 0.1% lows;
- 13.94% better p99 frame time;
- 4.49% lower average client CPU time.

Frames longer than 16.7 ms decreased in every tested client. Results depend on
hardware, scene, and mod stack; VRO's strongest repeatable benefit is smoother
frame delivery rather than a guaranteed FPS percentage.

## Compatibility

- **Minecraft:** 1.18.2
- **Mod loader:** Forge 40.3.11 or newer in the Forge 40.x line
- **Environment:** Client
- **Vault Hunters:** Official, Remastered, Wolds Vaults 0.32.2, and selected
  custom 1.18.2 compatibility baselines

Vault Hunters is an optional integration rather than a hard loading
dependency. Generic render fast paths remain available when Vault is absent.

VRO automatically yields overlapping work when Entity Collision FPS Fix,
BadOptimizations, Particle Core, or Flerovium is installed.

## Installation

1. Stop Minecraft.
2. Remove or disable older VRO jars.
3. Place `vault_render_optimization.0.3.0.jar` in the instance's `mods` folder.
4. Keep only one active VRO jar.

No server installation, world migration, or cache deletion is required.

## Compare Mode

VRO includes a persistent client-side comparison command:

| Command | Purpose |
| --- | --- |
| `/vro` | Show Compare Mode state |
| `/vro compare on` | Immediately disable performance optimizations |
| `/vro compare off` | Immediately enable configured optimizations |
| `/vro compare status` | Show Compare Mode state |

Crash guards, unloaded-world cleanup, and map-key compatibility remain active
because they are correctness fixes rather than performance changes.

## Credits and source

VRO was developed by
[HoYin1600p](https://github.com/HoYin1600p). Its learned-ability cache is
adapted from Unobtanium work by `radimous`, and its client collision behavior is
adapted from CorgiTaco's CC0 Entity Collision FPS Fix. Particle Core and
BadOptimizations informed independently written Forge 1.18.2 fast paths.

- [Source code and issue tracker](https://github.com/HoYin1600p/The-Vault-Render-Optimization)
- [Full release notes](https://github.com/HoYin1600p/The-Vault-Render-Optimization/blob/main/docs/releases/0.3.0.md)
- [Complete credits](https://github.com/HoYin1600p/The-Vault-Render-Optimization/blob/main/CREDITS.md)
- License: GNU Affero General Public License v3.0 or later

No third-party mod jar, Vault Hunters source, or decompiled class is bundled.
Minecraft is a trademark of Microsoft. Vault Hunters belongs to its respective
authors. This independent project is not affiliated with Mojang, Microsoft,
Forge, Iskallia, or the credited projects.
