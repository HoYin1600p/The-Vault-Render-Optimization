# The Vault Render Optimization 0.5.2

Stability update for the 0.5 GPU rendering. Recommended for everyone on 0.5.x.

## Fixed

- GPU rendering stays on the render thread: mods that build or upload buffers on
  their own threads no longer interfere with VRO's GPU work.
- Block entities and items that show different models in the same frame (for
  example dynamic or state-dependent models) can no longer borrow another one's
  geometry.
- VRO turns its GPU path off instead of running half-hooked when another mod
  changes the same rendering code, and a lost Oculus hook can no longer delay
  unrelated drawing.
- Very large draws that exceed the graphics card's limits stay on the CPU.
- Several native-memory safety fixes for out-of-memory situations.
- A very large texture atlas no longer hangs the loading screen (texture
  stitching falls back to vanilla instead).
- The GPU self-test no longer fails because of a leftover error from another
  mod, which could switch GPU rendering off for the whole session.
- Built-in ImmediatelyFast: a buffer still in use is never freed after the PC
  sleeps mid-frame.
- Opening the settings screen with an incompatible Cloth Config shows a message
  instead of crashing; the particle cache refreshes on resource reload.

## Changed

- Internal code tidy-up with no change in behaviour, and new diagnostics for
  Oculus' batched entity rendering in `/vro gpuentity stats`.

Tested on Vault Hunters 3rd Edition Remastered, Asgard and Wolds Vaults, with
and without shaders: GPU output matched vanilla exactly. Without shaders VRO
measured from about +1% (storage-heavy scene) to +69% (particle-heavy scene) FPS
over Compare Mode, with +15% to +29% in the mob scenes.

VRO remains client-only: stop Minecraft, replace the old VRO jar, and keep only
one version in your `mods` folder.

Full technical release details:
https://github.com/HoYin1600p/The-Vault-Render-Optimization/releases/tag/v0.5.2
