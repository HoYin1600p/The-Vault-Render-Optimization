# The Vault Render Optimization 0.5.0

This is the biggest VRO update so far. It moves a lot of repeated entity, item
and particle drawing work onto your graphics card, adds an in-game settings
screen, and bundles ImmediatelyFast for HUD and text drawing. Nothing is hidden
to gain speed: no particle, entity or item is skipped or culled, and models,
textures and animations look the same.

## What's new

- **GPU entity models** (on by default). Mobs, armor stands, armor, chests,
  beds, shulker boxes, signs and Vault Hunters' GeckoLib mobs (knights, bosses,
  pets and more) have their vertices written by your graphics card instead of
  the CPU. The result is checked against vanilla's exact math at startup and
  during testing. In our test scenes this gave about +8-11% FPS in a crowded
  mob scene and about +50% with 192 GeckoLib mobs in view. Results depend on
  your hardware and scene.
- **GPU particles** (on by default, shaders off). Billboard particles are
  written the same way. Every particle is still drawn. Under heavy Nova
  particles the measured gain was in particle rendering time and memory churn;
  frame rate was within noise.
- **GPU items** (on by default). Dropped, framed and held items, including flat
  items such as gems and loot, use the same path. Enchanted items stay on the
  CPU. With 240 flat item stacks added to a scene, FPS rose about 17% with
  shaders off and about 4% with a shader pack.
- **Settings screen.** Opens from a key that is **unbound by default**: set it
  in Options > Controls, under "The Vault Render Optimization". Settings are
  grouped into tabs, each with a short summary you can click for a full
  explanation, and a "(restart)" badge where a change needs a restart.
  **Default** resets to the shipped settings and **Experimental** turns on
  the experimental settings that ship off (it lists them first); both ask first.
- **Report a bug** button (Diagnostics tab). It shows a preview of the exact
  GitHub issue text and of your newest crash report, with folder paths, player
  name and UUID removed. Nothing is uploaded; you choose whether to copy the
  report and open GitHub.
- **Built-in ImmediatelyFast** (on by default). VRO now carries a copy of
  ImmediatelyFast Reforged 1.18.2 for HUD and immediate-mode batching, faster
  text lookup, font atlas resizing, map atlases and buffer-upload reuse. It
  switches itself off if the standalone ImmediatelyFast mod is installed.
- **Cheaper particles, every particle kept.** Particle collisions read each
  block once per tick instead of once per particle, dead particles are cleared
  in a single pass, particles share one random generator, and particle
  providers are looked up once. These are aimed at Vault Hunters Nova bursts
  and keep vanilla's behavior.
- **Fastload** helper: with Fastload installed, VRO skips an idle event lookup
  during visibility checks. Under heavy Nova particles particle render time
  dropped about 17% in testing.
- **Farsight chunk bound** (on by default). With Farsight installed, client
  chunks the server has already unloaded and that are far outside your view
  are released, freeing memory Farsight would otherwise keep until you change
  levels. Carried over from VH Accelerator's fix.
- New testing tools for pack makers: `/vro feature ...`, `/vro gpuentity verify`,
  `/vro alloc ...` and `/vro particles stress ...`.
- All VRO screens use a fixed 960 x 540 layout so they look the same at every
  resolution and GUI scale.

## Default off or experimental

- GPU entity models, particles and items **with a shader pack**
  (`gpu_entity_models_with_shaders`, `gpu_particles_with_shaders`): default off.
  On a GPU-limited shader setup this cost about 4-8% FPS in testing, so it is
  meant for players held back by a slower CPU.
- Opt-in experiments, all default off: HUD text geometry reuse, CPU vertex
  buffer trimming, translucent sort geometry reuse, and an empty-air particle
  collision bypass. These have no measured FPS claims.
- GPU paths turn themselves off automatically with an Oculus shader pack (unless
  the shader options above are on), on unsupported drivers, in Compare Mode, or
  when another mod changes the same model rendering code. They need OpenGL 4.3
  (or the equivalent extensions) and a real GPU driver.

## Fixes and changes

- Your own block edits are no longer held back by the adaptive chunk budget.
- If the old Oculus Flywheel Compat mod (`irisflw`) is installed, VRO switches
  it off and uses its own Create shader support; you can remove irisflw.
- Create shader compatibility checks the installed Oculus jar first and turns
  itself off with a log message instead of failing, and now handles shader
  packs that declare `mc_Entity` as `vec2` or `vec3`.
- Embeddium/Rubidium transfers now match validated versions by exact bytes, so
  older fork builds that already contain the code no longer run twice.
- Fixed several memory leaks: Create contraption meshes, and references to
  destroyed Oculus pipelines.
- Flywheel instancing is now only turned on for the current session when your
  pack has it off; VRO no longer edits Flywheel's config file.
- Removed the unvalidated, default-off dynamic-lights feature and its
  `/vro lights` commands. Use a standalone dynamic-lights mod.

## Requirements and compatibility

- Minecraft 1.18.2, Forge 40.3.11 or newer in the Forge 40.x line, client only.
- **Cloth Config 6.5.102 or newer is optional** and needed only for the settings
  screen. Without it VRO runs normally and the settings key tells you where the
  settings live (the config file and `/vro` commands).
- Embeddium `0.3.18+mc1.18.2` and `0.3.19+mc1.18.2`, and Rubidium `0.5.6`, are
  the validated renderer versions. No renderer mod is required.
- Oculus 1.6.4 and the 1.6.5, 1.6.7 and 1.6.8 dh-compat builds are supported for
  Create shader compatibility.
- Vault Hunters, Create, Sophisticated Storage and the other integrations stay
  optional.

## Testing note

The settings screen, the Report a bug button and the built-in ImmediatelyFast
are new in this release and have had less in-game testing than the GPU paths.
If something looks wrong, turn Compare Mode on (`/vro compare on`) and report it.

VRO remains client-only. Stop Minecraft, replace the old VRO jar, and keep only
one version in your `mods` folder. No server update or world migration is
needed. New settings take their defaults, and removed dynamic-light settings
are dropped from your config file automatically.

Full technical release details:
https://github.com/HoYin1600p/The-Vault-Render-Optimization/releases/tag/v0.5.0
