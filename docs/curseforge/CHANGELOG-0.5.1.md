# The Vault Render Optimization 0.5.1

Crash fix for 0.5.0. Update if you play with Oculus installed.

## Fixed

- Fixed a crash (`IndexOutOfBoundsException` in `HoleBatch.fillItem`) when
  Oculus' batched entity rendering drew a buffer segment differently from how
  VRO had prepared it. VRO now fills the vertices into the correct buffer, and
  every fallback fill checks its bounds: anything that would not fit is skipped
  and counted in `/vro gpuentity stats` instead of crashing the game.

Nothing else changed from 0.5.0. VRO remains client-only: stop Minecraft,
replace the old VRO jar, and keep only one version in your `mods` folder.

Full technical release details:
https://github.com/HoYin1600p/The-Vault-Render-Optimization/releases/tag/v0.5.1