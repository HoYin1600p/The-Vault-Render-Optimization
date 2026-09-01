## VRO 0.4.1

This update improves visible particles, model preparation, chunk rendering,
and long-session renderer stability while preserving normal visuals and
server gameplay.

### Faster rendering and models

- Fixed two OpenGL errors raised while The Vault initializes its grayscale
  screen shader.
- Added eleven guarded ModernFix render/model improvements for chunk meshing,
  model caches, profile textures, texture stitching, and Forge/CTM concurrency.
- Added eight supported Embeddium/Rubidium corrections for chunk rebuilds,
  bounded buffers, arena growth, custom block faces, fluid lighting, shader
  color, vertex writers, and optional CodeChickenLib rendering.
- VRO yields overlapping work when ModernFix or another supported owner is
  already handling the same feature.

### Lower-cost visible particles

- Reduced the CPU and allocation cost of ordinary particle billboards.
- Shared safe light lookups between particles in the same block and tick.
- Reused packed Rubidium/Embeddium output where supported.
- Kept every visible particle, its normal count, animation, UVs, color, light,
  and lifetime. VRO does not asynchronously tick or hide on-screen particles.
- Added `/vro particles` controls and diagnostics that apply immediately.

### Experimental Mana Stealer visuals

- Added an optional pale-blue/navy orb replacement for the Remastered Mana
  Stealer trap, including a matching stream from affected players to the trap.
- Added `/vro mana_stealer preview <ticks> [x y z]` for a client-only visual
  preview without creating a trap or draining mana.
- The feature is isolated, hot-toggleable, and does not change the server's
  trap radius, mana drain, duration, or gameplay.

### Upgrade notes

VRO remains client-only for Minecraft 1.18.2 and Forge 40.3.11 or newer in the
Forge 40.x line. Stop Minecraft, replace the old VRO JAR, and keep only one VRO
version in the `mods` folder. No world migration, cache deletion, or settings
reset is required.

[Full detailed changelog on GitHub](https://github.com/HoYin1600p/The-Vault-Render-Optimization/releases/tag/v0.4.1)
