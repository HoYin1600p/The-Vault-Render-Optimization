## VRO 0.4.1

This update improves frame pacing, visible particles, model preparation, and
long-session renderer stability while preserving normal visuals and gameplay.

### Smoother chunk rendering

- Uses the renderer's native deferred chunk-update path by default to reduce
  blocking rebuild stalls.
- Avoids re-uploading unchanged terrain vertices during supported Embeddium
  transparency sorting.
- Adds measured adaptive pacing for completed Embeddium terrain work, with a
  native fallback while initial or cached terrain is loading.
- Adds `/vro chunks` controls and diagnostics that apply immediately.

### Faster rendering and models

- Adds eleven guarded ModernFix improvements for chunk meshing, model caches,
  profile textures, texture stitching, and Forge/CTM concurrency.
- Adds eight supported Embeddium/Rubidium corrections for chunk rebuilds,
  bounded buffers, arena growth, custom block faces, fluid lighting, shader
  color, vertex writers, and optional CodeChickenLib rendering.
- VRO yields overlapping work when ModernFix or another supported owner is
  already handling the same feature.

### Lower-cost visible particles

- Reduces ordinary particle billboard, output, and light-lookup costs without
  hiding visible particles or changing their count, animation, color, or
  lifetime.
- Adds `/vro particles` controls and diagnostics that apply immediately.

### Fixes and compatibility

- Fixes two OpenGL errors during The Vault's grayscale shader setup.
- Keeps Flywheel model formats safe across Oculus startup and resource-reload
  transitions.
- Remains client-only for Minecraft 1.18.2 and Forge 40.3.11 or newer in the
  Forge 40.x line. No world migration, cache deletion, or settings reset is
  required.

[Full detailed changelog on GitHub](https://github.com/HoYin1600p/The-Vault-Render-Optimization/releases/tag/v0.4.1)
