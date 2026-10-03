# The Vault Render Optimization

**Client-side performance and stability for Vault Hunters: GPU entity, particle and item rendering, an in-game settings screen, faster Create contraptions, particles, terrain and storage displays.**

The Vault Render Optimization (VRO) is a Minecraft 1.18.2 Forge mod that cuts repeated client work while preserving normal models, textures, animations, effects, loot, and server gameplay. Version 0.5 moves a large share of entity, item and particle drawing work onto your graphics card and adds an in-game settings screen. VRO works with official and custom Vault Hunters packs, and its generic optimizations remain available when Vault Hunters is not installed.

VRO is client-side. The remote server does not need it.

## GPU rendering (new in 0.5)

VRO writes repeated model vertices with a compute shader on your graphics card, straight into the buffer the game has just uploaded, instead of building them on the CPU. The result matches vanilla's exact output: every path passes a startup self-test, and `/vro gpuentity verify` compares the GPU output with vanilla's while you play. Nothing is hidden or skipped to gain speed.

*   **Entity models** (on by default): mobs, armor stands, armor, chests, beds, shulker boxes, signs, Vault Hunters' GeckoLib mobs (knights, bosses, pets and more), GeckoLib block entities, and Citadel models.
*   **Items** (on by default): items on the ground, in item frames, held in hands and on armor stands, including flat items such as gems and loot. Whole block models drawn by block entities use the same path. Enchanted items stay on the CPU.
*   **Particles** (on by default without shaders): billboard particles are written the same way. Every particle is still drawn.
*   **Shader packs:** with an Oculus shader pack the GPU paths pause by default, because shader packs usually make the graphics card the bottleneck already. Players held back by a slower CPU can keep them on in the settings screen.

In test scenes this gave about +8-11% FPS in a crowded mob scene and about +50% with 192 GeckoLib mobs in view. Results depend on your hardware and scene. The GPU paths need OpenGL 4.3 (or the equivalent extensions) and switch themselves off on unsupported drivers, in Compare Mode, or when another mod changes the same rendering code; normal CPU rendering then takes over.

## Settings screen (new in 0.5)

Every VRO setting now has an in-game screen, grouped into tabs: GPU rendering, Chunks & terrain, Entities & particles, Interface & HUD, Mod compatibility, ImmediatelyFast, Updates and Diagnostics.

*   Open it with a key you choose in **Options > Controls** under "The Vault Render Optimization" (unbound by default).
*   Each setting has a short summary; click it to expand a plain-English explanation. Settings that need a restart are marked, and saving tells you which ones changed.
*   **Default** resets everything to the shipped settings; **Experimental** turns on the experimental settings that ship off, listing them first. Both ask before applying.
*   **Report a bug** (Diagnostics tab) previews a GitHub issue with your versions, rendering mods and VRO status, with your user folder and player name removed. It can copy your newest crash report to the clipboard and open the issue page; nothing is uploaded.
*   The screen keeps the same layout at every resolution and GUI scale.
*   Needs Cloth Config (included in the Vault Hunters packs). Without it, VRO runs normally and the key tells you where the settings live.

VRO can also check its update manifest in the background and show an update row on the main menu plus occasional CurseForge reminders in chat; see Update notices below.

## Vault Hunters improvements

*   Caches repeated Vault gear, armor, tool-model, ability HUD, durability, and damage-number work.
*   Reduces repeated client render-event processing used by Vault lighting, biome colors, dimension effects, and end-of-level rendering.
*   Defers unused Vault Loot Beams tooltip work and clears retained data when a world unloads.
*   Reduces iSpawner display-item work without changing its animation or configured viewing distance.
*   Resolves the Vault map and Xaero's World Map `M`\-key conflict.
*   Isolates Vault elixir number rendering so it cannot corrupt later particle colors.

## Create and shader improvements

*   Divides large Create contraptions into render sections so off-screen pieces can be skipped without lowering model detail.
*   Skips off-screen contraption actors and special block entities when it is safe to do so.
*   Keeps Flywheel GPU instancing available with supported Oculus shader stacks, avoiding the severe fallback-renderer slowdown seen around large moving contraptions.
*   Works with supported Rubidium and Embeddium configurations.
*   Allows shader packs to provide dedicated Flywheel scene and shadow programs. When they are unavailable or fail to compile, VRO automatically retries its generated compatibility path and then falls back safely.
*   Turns on Flywheel's instancing renderer for the current session when a pack ships it disabled, without changing Flywheel's own config file, while keeping its hardware and shader failure safeguards.
*   Keeps Flywheel model formats synchronized across Oculus startup and resource-reload pipeline transitions.

Use `/vro create status` to see the active Flywheel backend, shader path, and contraption-culling activity. Shader compatibility can be changed immediately with `/vro create shader_compat on|off|status`.

## General performance and memory

*   Reduces client collision work around dense mob farms and rapid kill systems.
*   Builds ordinary particle billboards from the camera basis, reuses Rubidium/Embeddium packed output, shares same-tick light results, and skips empty particle, toast, tutorial, debug, and renderer setup work without hiding visible effects.
*   Makes particle-heavy moments such as Vault Hunters Nova bursts cheaper: collisions read each block once per tick instead of once per particle, dead particles are removed in one pass, particles share one random generator, and particle providers are looked up once. Every particle is kept.
*   Includes a built-in copy of ImmediatelyFast for HUD, text, map and buffer-upload batching. It switches itself off when the standalone ImmediatelyFast mod is installed.
*   With Farsight installed, releases client chunks the server has already unloaded once they are far outside your view, so memory no longer grows for the whole session.
*   Adapts eleven later ModernFix render/model improvements for chunk meshing, model caches, profile textures, texture stitching, and Forge/CTM concurrency.
*   Carries eight guarded Embeddium/Rubidium corrections for chunk rebuilds, bounded native buffers, arena growth, custom block faces, fluid lighting, shader color, vertex writers, and optional CodeChickenLib rendering.
*   Uses renderer-native asynchronous chunk scheduling by default to reduce blocking rebuild stalls without changing another mod's settings.
*   Avoids copying and uploading unchanged terrain vertices during supported Embeddium transparency re-sorts.
*   Paces already-built Embeddium terrain work under measured load while leaving initial and cached-terrain loading on the renderer's native path.
*   Makes Sophisticated Storage barrel displays cheaper by culling hidden or covered front faces, caching quantity labels, emitting fill bars without temporary vertex objects, and avoiding chunk rebuilds for count/fill-only updates. Tier badges are unchanged.
*   Compacts baked-model and block-state data beyond the reductions already present in FerriteCore 4.2.2.
*   Adds separate vertical and horizontal terrain-section culling. Vertical culling is enabled by default; horizontal culling is optional.
*   Cleans up retained Create Addition, Powah, Vault Loot Beams, and empty-item references during long sessions and world changes.

## Renderer ownership and safe fallback

VRO applies each backport only when its exact supported target is present and no validated owner is already handling that path. It yields to genuinely active ModernFix features, recognizes Embeddium's same-JAR Rubidium compatibility identity, and blocks unknown or ambiguous renderer layouts instead of guessing.

The supported renderer-transfer baselines are Embeddium `0.3.18+mc1.18.2` and `0.3.19+mc1.18.2`, plus Rubidium `0.5.6`. Compare Mode yields performance-sensitive work while narrow correctness guards remain active. Use `/vro backports` for ModernFix ownership details; renderer-transfer decisions are also reported in the startup log.

## Stability fixes

VRO includes client-side guards for several known stale-state crashes, including Powah cable replacement, Vault Integrations altar conduits, and Xaero's World Map cache writes. It also prevents The Vault's native grayscale shader from uploading startup values without its OpenGL program bound. These fixes do not change server behavior.

## Update notices

VRO checks its repository-owned update manifest in the background and never downloads or installs files. When an allowed release is available, it can show a small coordinated main-menu row and an occasional in-world reminder linking to this official CurseForge page.

Update checks are enabled by default, while displayed update types default to critical-only. Use `/vro updates all` to include normal release notices or `/vro updates off` to disable VRO's menu and chat notices immediately. Short timeouts, response limits, and fail-closed parsing keep network or manifest problems nonfatal. Rejoining, changing dimensions, or transferring servers does not repeat a reminder during the same Minecraft launch.

## Compatibility

*   **Minecraft:** 1.18.2
*   **Forge:** 40.3.11 or newer in the Forge 40.x line
*   **Environment:** Client only
*   **Vault Hunters:** Official Third Edition, Remastered, Wolds Vaults, and selected custom 1.18.2 baselines
*   **Create shader path:** Create 0.5.1.i, Flywheel 0.6.11, Oculus 1.6.x, and supported Rubidium or Embeddium releases
*   **Renderer-transfer baselines:** Embeddium 0.3.18/0.3.19 and Rubidium 0.5.6
*   **Sophisticated Storage display path:** Sophisticated Storage 1.18.2-0.9.8.915 with Sophisticated Core 1.18.2-0.6.4.604
*   **Oculus:** 1.6.4 and the 1.6.5, 1.6.7 and 1.6.8 dh-compat builds
*   **Settings screen:** Cloth Config 6.5.102 or newer (optional; only the screen needs it)

Optional integrations load only when their target mod is present. VRO yields overlapping work when Entity Collision FPS Fix, BadOptimizations, Particle Core, Flerovium, or Better Fps - Render Distance is installed.

## Installation

1.  Stop Minecraft.
2.  Remove or disable older VRO jars.
3.  Download the latest release and place its VRO jar in the instance's `mods` folder.
4.  Keep only one active VRO jar.

No server installation, world migration, cache deletion, or settings reset is required.

## In-game controls

| Command                                 |Purpose                                                        |
| --------------------------------------- |-------------------------------------------------------------- |
| Settings key (set in Controls)          |Open the VRO settings screen.                                  |
| <code>/vro</code>                       |Show the current Compare Mode state.                           |
| <code>/vro gpuentity status|stats|verify on|off</code> |Show GPU rendering status and counters, or compare GPU output with vanilla. |
| <code>/vro feature list</code>          |List the GPU feature switches (`/vro feature <name> on|off`).  |
| <code>/vro compare on|off|status</code> |Compare VRO optimizations without restarting Minecraft.        |
| <code>/vro updates on|off|status|critical|all</code> |Control update checks and choose critical-only or all notices. |
| <code>/vro backports</code>             |Show the startup owner and reason for each ModernFix-derived backport. |
| <code>/vro particles</code>             |Control hot billboard ownership, shared light caching, and particle diagnostics. |
| <code>/vro chunks status</code>         |Show chunk deferral, sorting, adaptive pacing, and observed renderer state. |
| <code>/vro chunks defer on|off</code>   |Control renderer-native asynchronous chunk updates immediately. |
| <code>/vro chunks sorting on|off|status</code> |Control supported Embeddium index-only transparency sorting. |
| <code>/vro chunks budget on|off|status</code> |Control supported Embeddium adaptive chunk pacing and diagnostics. |
| <code>/vro storage on|off|status</code> |Control Sophisticated Storage display paths and diagnostics. |
| <code>/vro culling</code>               |View or change vertical and horizontal terrain culling.        |
| <code>/vro create status</code>         |Show Create, Flywheel, shader-path, and culling status.        |
| <code>/vro create shader_compat on|off|status</code> |Control Flywheel compatibility with Oculus shaders.            |

Crash guards, world cleanup, and map-key compatibility remain active in Compare Mode because they are correctness fixes rather than performance features.

## Source, support, and credits

*   [Source code](https://github.com/HoYin1600p/The-Vault-Render-Optimization)
*   [Issue tracker](https://github.com/HoYin1600p/The-Vault-Render-Optimization/issues)
*   [Installation and compatibility](https://github.com/HoYin1600p/The-Vault-Render-Optimization/blob/main/docs/INSTALLATION.md)
*   [Create shader compatibility](https://github.com/HoYin1600p/The-Vault-Render-Optimization/blob/main/docs/FLYWHEEL_SHADER_COMPAT.md)
*   [ModernFix backports](https://github.com/HoYin1600p/The-Vault-Render-Optimization/blob/main/docs/MODERNFIX_RENDER_BACKPORTS.md)
*   [Embeddium/Rubidium ownership transfers](https://github.com/HoYin1600p/The-Vault-Render-Optimization/blob/main/docs/EMBEDDIUM_OWNERSHIP_TRANSFER.md)
*   [Particle optimizations](https://github.com/HoYin1600p/The-Vault-Render-Optimization/blob/main/docs/PARTICLE_OPTIMIZATIONS.md)
*   [Chunk update deferral](https://github.com/HoYin1600p/The-Vault-Render-Optimization/blob/main/docs/CHUNK_UPDATE_DEFERRAL.md)
*   [Adaptive chunk budgets](https://github.com/HoYin1600p/The-Vault-Render-Optimization/blob/main/docs/ADAPTIVE_CHUNK_BUDGET.md)
*   [Sophisticated Storage rendering](https://github.com/HoYin1600p/The-Vault-Render-Optimization/blob/main/docs/SOPHISTICATED_STORAGE.md)
*   [Index-only transparency sorting](https://github.com/HoYin1600p/The-Vault-Render-Optimization/blob/main/docs/INDEX_ONLY_SORTING.md)
*   [Complete changelog](https://github.com/HoYin1600p/The-Vault-Render-Optimization/blob/main/CHANGELOG.md)
*   [Credits](https://github.com/HoYin1600p/The-Vault-Render-Optimization/blob/main/CREDITS.md)
*   [Third-party notices](https://github.com/HoYin1600p/The-Vault-Render-Optimization/blob/main/THIRD_PARTY_NOTICES.md)

VRO was developed by [HoYin1600p](https://github.com/HoYin1600p) and is licensed under GNU AGPL v3.0 or later. Complete adapted-source attribution and license notices are included in the public repository and release jar.

No third-party mod jar, Vault Hunters source, shader pack, or decompiled class is bundled. The built-in ImmediatelyFast copy is relocated, attributed source used under its license. Minecraft is a trademark of Microsoft. Vault Hunters belongs to its respective authors. This independent project is not affiliated with Mojang, Microsoft, Forge, Iskallia, or the credited projects.
