# Client Performance Backport Research

Research date: 2026-08-01

Follow-up audit: 2026-08-02

Second follow-up audit: 2026-08-15

This document records performance projects and implementation ideas that may be
useful to a Forge 1.18.2 Vault Hunters client. It is a design and provenance
ledger, not permission to copy source code. No candidate described here has
been implemented unless a later commit and `THIRD_PARTY_NOTICES.md` entry say
otherwise.

## Product Baseline

`Vault Hunters Third Edition` is the only Prism instance treated as the
shipping modpack baseline. The Bootstrap, Asgard-SMP, Wolds, and other local
instances contain private customizations and are compatibility test
environments only. A mod appearing in those other instances is not assumed to
be available to ordinary users.

VH Accelerator is the deliberate exception: it may be installed in a copy of
Third Edition to verify compatibility before shipping this mod.

The inspected Third Edition client already includes these relevant projects:

| Installed project | Inspected jar | Existing responsibility |
| --- | --- | --- |
| Embeddium | `embeddium-0.3.18+mc1.18.2.jar` | Chunk and immediate renderer optimization |
| ModernFix | `modernfix-forge-5.18.0+mc1.18.2.jar` | Memory leaks, startup, resource and general fixes |
| FerriteCore | `ferritecore-4.2.2-forge.jar` | Object and model memory reductions |
| LazyDFU | `lazydfu-1.0-1.18+.jar` | Deferred data-fixer initialization |
| FastWorkbench | `FastWorkbench-1.18.2-6.1.1.jar` | Crafting recipe lookup |
| Clumps | `Clumps-forge-1.18.2-8.0.0+17.jar` | Experience-orb aggregation |
| Spark | `spark-1.10.38-forge.jar` | Profiling and diagnostics |

The shipping baseline does **not** currently include MemoryLeakFix, Saturn,
Entity Culling, Starlight, Smooth Boot, Fastload, or ImmediatelyFast. Their
presence in another local instance does not make them a baseline dependency.

VH Accelerator already owns parallel and guarded model/blockstate preparation,
persistent model/material/recipe/fuel/JEI caches, asynchronous JEI search and
validation, registry/CTM/voxel-shape work, and launch/login/world-transfer
optimizations. VRO should not reproduce those systems.

## Existing shipped provenance

This research ledger predates the public-release attribution audit. Two
mechanisms are retained adaptations rather than independent research:

- the learned client ability-list cache is adapted from Unobtanium commit
  `7bf6a6585014e07b9fca622482ce40e83b73d8e8` under AGPL-3.0-or-later;
- the client wall-check and entity-push mixins are adapted from Entity
  Collision FPS Fix revision `cc16e1843f592084e6ebea64d2c7399e5189fa09`
  under CC0-1.0.

The exact authorship, adaptation, and distribution obligations are recorded in
`CREDITS.md` and `THIRD_PARTY_NOTICES.md`. VRO is released under
AGPL-3.0-or-later.

## 2026-08-15 Unobtanium and Create review

Unobtanium was refreshed through revision
`bb3e8a523807f919d217fa8008fefcb4b8e7fb13`. Three low-risk ideas were accepted
as independent VRO implementations: preventing the shared empty item stack
from retaining an entity, deferring Vault Loot Beams' eager tooltip work with
unload cleanup, and replacing iSpawner's per-render stream/list construction
with direct inventory passes. VRO deliberately did not adopt Unobtanium's
shorter iSpawner display distance because that would remove visible output.

Create 0.5.1.i was inspected from the installed 1.18.2 jar, and newer official
Create renderer branches were reviewed through
`87b3c6a65fd00c023a07b37b0353144bc7e6a5bf`. Super Glue itself is not normally
drawn; the likely correlation is that glue assembles larger moving
contraptions, whose structure and special block entities must then be rendered.
The installed renderer repeats Flywheel backend/world eligibility checks for
each moved block entity every frame. A modern behavioral reference, Create:
Catalyst, reports caching that decision per tick, but its source is not being
used and its project is all rights reserved.

The safest future Create candidate is a VRO-owned cache of the Flywheel
eligibility decision for one contraption render pass or client tick. Before
shipping it, collect frame-time and allocation diagnostics with the problem
contraption in view, with shaders both off and on. Whole-contraption distance
culling helps only distant structures and changes visible output; server tick
throttling changes gameplay and is outside VRO's client-only scope.

## Implementation Rules

1. Preserve gameplay and visible output by default. Optional visual compromises
   must be clearly named, configurable, and disabled by default.
2. Prefer small independent mechanisms with measurable hot paths over wholesale
   ports of another optimization mod.
3. Implement from first principles against Minecraft/Forge APIs. Do not copy
   source simply because a license would permit it.
4. Record the design source, inspected revision, license, and adaptation in
   `THIRD_PARTY_NOTICES.md` when an implementation is committed.
5. Respect author policies that are stricter than the repository license.
   MemoryLeakFix and MoreCulling explicitly request that their work not be
   merged into other mods without permission.
6. Automatically disable an optimization when its original standalone mod is
   present, unless coexistence has been proven safe.
7. Keep shader-sensitive, multithreaded, and renderer-lifecycle changes behind
   separate switches and commits so they can be isolated quickly.
8. Validate first against a copied Third Edition instance. Use custom Oculus,
   Embeddium, Distant Horizons, and shader instances only as additional
   compatibility tests.

## Ranked Shortlist

Implementation update: the current development branch now contains the safe P1
particle-light and empty-work paths plus the P2 entity/block-entity renderer
lookup caches. Forge 1.18.2 was confirmed to already perform particle frustum
culling, so that portion was not duplicated. The standalone trials and P3 work
remain unimplemented.

| Priority | Candidate | Likely value | Risk | Recommendation |
| --- | --- | --- | --- | --- |
| P1 | Particle frustum culling and light cache | Better frame time in particle-heavy scenes; less repeated lighting work | Low to medium | Light cache implemented; Forge already supplies culling |
| P1 | Empty-render and zero-work fast paths | Small CPU reductions every frame with almost no behavior change | Low | Implemented as individually gated mixins |
| P1 | MemoryLeakFix 1.1.5 standalone test | Better long-session retention and fewer stale references | Low to medium | Test the public mod; do not merge its source without permission |
| P1 | Jasione standalone test | Lower allocation and GC pressure without visual changes | Medium | A/B test its public 1.18.2 Forge build before considering integration |
| P2 | ImmediatelyFast/Reforged A/B test | Potentially substantial GUI, text, entity, and upload gains | Medium to high | Test standalone first; only isolate proven mechanisms later |
| P2 | Entity and block-entity renderer lookup cache | Small gain in entity-heavy scenes | Medium | Implemented with reload invalidation |
| P2 | Compact simple-model face lists | Lower retained model memory, commonly up to about 20 MB | Low | Post-4.2.2 FerriteCore feature; implement independently and retain its MIT notice |
| P2 | Block-state `faceSturdy` array deduplication | Modest retained block-state memory reduction | Low to medium | Post-4.2.2 FerriteCore feature; verify an independent mixin alongside FerriteCore 4.2.2 |
| P2 | Independent vertical/horizontal section-distance culling | Less chunk-section traversal and drawing at high render distances | Medium | Implement independently; vertical enabled by default, horizontal disabled by default |
| P2 | Optional corrected dynamic-light engine | Replaces Dynamic Lights Reforged with bounded nearby-source lookups and cleaner lifecycle handling | Medium to high | Implement independently, disabled by default, and yield to the standalone mod |
| P3 | Static block-entity model batching | Large gain in chest/bed-heavy areas | High | Dedicated project with a strict vanilla allowlist |
| P3 | Occlusion culling | Potential gain in dense bases | High | Prefer a standalone trial; compatibility holes are likely |

## P1: Conservative Particle Work

### Design sources

- Particle Core: https://github.com/fzzyhmstrs/pc
- Flerovium: https://github.com/MoePus/Flerovium
- BadOptimizations: https://github.com/imthosea/BadOptimizations

Particle Core provides the clearest modern reference. Its feature set includes
particle frustum checks, particle light lookup caching, movement/position
caching, render-distance and count controls, asynchronous ticking, and spawn
suppression. Forge support begins after 1.18.2, which makes the safe mechanisms
real backport candidates.

The first retained implementation is deliberately narrower:

- Cache packed light for a particle within a client tick when its block position
  has not changed.
- Share identical block-light results among particles in the same block during
  that tick, with a strict bound.
- Build ordinary billboard geometry from the camera left/up basis while
  preserving roll, UV order, color, light, and renderer-native packed writes.
- Invalidate naturally on position/tick change, world change, and renderer
  teardown.
- Do not reduce particle count, distance, or spawn rate.
- Do not move particle ticking to worker threads.

Embeddium already accelerates particle vertex submission. VRO reuses that
packed writer when present and can yield the full billboard path back to the
renderer without restarting. A portable writer covers clients without that
renderer. Forge 1.18.2 already performs its established particle-frustum step,
so VRO does not add another visible-particle culling policy.

Asynchronous particle ticking is deferred. Modded particles commonly touch
world, entity, texture, and renderer state that is not thread-safe, so it has a
much larger stability surface than culling or same-tick caching.

## P1: Zero-Work Render Fast Paths

### Design source

- BadOptimizations: https://github.com/imthosea/BadOptimizations

The safest ideas are early exits that preserve the exact rendered result:

- Return before particle extraction/render setup when every particle queue is
  empty.
- Return before debug-renderer setup when no debug renderer has work.
- Skip tutorial ticking outside the demo/tutorial conditions.
- Skip toast-renderer setup when there is no current, queued, or transitioning
  toast.
- Skip effect-scale FOV calculations when the configured effect scale is zero.

Each fast path should be a separate mixin/config key. This keeps failures
attributable and permits a single optimization to be disabled without losing
the rest.

BadOptimizations also contains entity/block-entity renderer lookup caching,
lightmap caching, and sky-color caching. Renderer lookup caching is a possible
P2 item if it is invalidated on resource reload and renderer registration.
Lightmap and sky-color caching are not suitable for the first batch: this pack
has custom shaders, dynamic lighting, Vault dimensions, and a history of
lighting regressions. Every modded input and invalidation event would need to be
identified first.

## P1: Standalone MemoryLeakFix Trial

### Source

- MemoryLeakFix: https://github.com/FxMorin/MemoryLeakFix

Forge 1.18.2 builds exist. Relevant fixes include stale client hit/crosshair
references, screenshot native-buffer cleanup on failure, the 1.18.2 TagKey
interner leak, retained living-entity brain memories, failed resource-read
buffers, and a biome temperature ThreadLocal issue.

ModernFix 5.18 already enables its biome-temperature-cache removal and several
world/buffer leak repairs, so that area is overlapping and must not be applied
twice. The remaining value is long-session retention rather than immediate
average FPS.

Recommendation: test the public MemoryLeakFix 1.1.5 jar in a copied Third
Edition instance, profile retained heap over a multi-hour session, and check
disconnect/reconnect and dimension transitions. Do not merge MemoryLeakFix code
into VRO without author permission. For independently implemented vanilla bug
fixes, use the corresponding Mojang issue and vanilla-version correction as the
primary design source.

## P1: Standalone Jasione Trial

### Sources

- Jasione: https://github.com/decce6/Jasione
- Project page: https://modrinth.com/mod/jasione

Jasione analyzes bytecode uses of `Enum.values()`. When its analysis proves that
the returned array is neither modified nor allowed to escape, it redirects the
call to a generated cached holder instead of allocating a clone. Unsafe or
unproven uses remain unchanged.

The project supplies a native Forge 1.18.2 target against Forge 40.3.12. It is a
good allocation/GC experiment because it does not intentionally alter visuals
or game logic. It is also very new and transforms a broad set of loaded classes,
so it should remain a standalone A/B test first. Capture startup transformation
failures and compare allocation profiles, GC pauses, launch time, and mod
compatibility before considering it for the shipping stack.

Reimplementing this inside VRO is not recommended initially. A general
bytecode-analysis transformer has a much larger maintenance and compatibility
surface than VRO's targeted mixins.

## P2: ImmediatelyFast and ImmediatelyFastReforged

### Sources

- ImmediatelyFast: https://github.com/RaphiMC/ImmediatelyFast
- ImmediatelyFastReforged: https://github.com/CCr4ft3r/ImmediatelyFastReforged

The official discontinued 1.18.2 branch implements immediate-mode batching,
faster buffer uploads, text/font lookup improvements, font atlas resizing, HUD
batching, and a map texture atlas. Newer branches add stronger batching,
redundant-framebuffer-switch avoidance, sign text buffering, and further text
paths.

This family has the highest plausible direct FPS upside in GUI/text/entity-heavy
scenes, but it intersects areas that have already produced pack-specific bugs:
Hydrate animated text, Xaero overlays, Oculus framebuffers, frozen HUD state,
and shader toggles. The correct first step is a public standalone Forge 1.18.2
A/B test in a copied Third Edition instance, not a wholesale merge.

If profiling proves a specific path valuable, isolate it behind a VRO setting.
Map atlas work and non-UI buffer upload are safer initial candidates than HUD
batching, screen batching, sign buffering, or framebuffer lifecycle changes.
VRO must disable an equivalent feature when either ImmediatelyFast variant is
installed.

## P2: Independent Section-Distance Culling

### Design reference

- Better Fps - Render Distance: https://github.com/someaddons/betterfpsdistances

The Forge 1.18.2 project is all rights reserved. VRO must not copy its source,
mixin structure, configuration code, or formulas. The permissible input is the
general rendering goal: avoid traversing and drawing chunk sections that are
outside a separately configured camera-centered horizontal or vertical range.
The VRO implementation must be written independently against Minecraft and the
installed renderer APIs.

The planned VRO variation has these requirements:

- vertical and horizontal behavior have independent enable switches and
  independently configurable distances;
- vertical section-distance culling is enabled by default;
- horizontal corner/distance culling is disabled by default so VRO preserves
  the configured horizontal render distance unless the user opts in;
- the calculation is symmetric around the active camera and does not rotate or
  change shape with player yaw;
- it affects rendering only. It does not change server view distance, client
  chunk retention, chunk generation, simulation distance, or Distant Horizons
  storage;
- vanilla rendering and the supported Embeddium/Rubidium path receive separate,
  narrowly targeted implementations;
- VRO yields this entire feature when mod ID `betterfpsdist` is installed;
- entity render distance is not changed as part of the default feature;
- settings can be disabled for immediate A/B comparisons without replacing a
  jar, although renderer reload may be required when a distance changes.

VRO 0.3.2 implements this design with a default vertical distance of 12
sections and horizontal culling disabled. Validation must continue to include
tall Vault rooms, mountains, deep caves, the Nether, spectator flight,
32-chunk render distance, shaders, and Distant Horizons transition boundaries.
Acceptance requires no missing terrain, camera-angle-dependent popping, or
stale sections after dimension changes or shader reloads.

## P2: Optional Corrected Dynamic Lights

### Design reference

- Dynamic Lights Reforged 1.18:
  https://github.com/txnimc/DynamicLightsReforged/tree/1.18

The inspected Forge port and its inherited LambDynamicLights source are MIT
licensed. VRO may legally adapt that version with attribution, but a fresh
implementation is preferred so the subsystem can fit VRO's lifecycle,
configuration, diagnostics, and comparison controls. Newer LambDynamicLights
branches use a different restrictive license and are not implementation
sources.

The installed 1.18 implementation has several weaknesses:

- every light lookup iterates every active dynamic light source, so work grows
  with both the number of rendered light lookups and the total source count;
- reduced-quality modes share one update timestamp across all entities, which
  can let one source consume the update opportunity while other sources wait;
- an entity path checks the block-entity lighting setting instead of the entity
  lighting setting;
- filtered cleanup stops after the first matching source, which can retain
  stale sources when a category is disabled or a world changes;
- the `OnlyUpdateOnPositionChange` setting is declared but not honored;
- moving sources can request overlapping section rebuilds independently instead
  of coalescing them before submission.

The planned VRO subsystem must correct those issues:

- index active sources into camera-independent spatial cells and inspect only
  nearby cells when calculating dynamic light at a position;
- maintain per-source update state or a fair central scheduler rather than one
  shared timestamp;
- use separate entity and block-entity controls and remove every matching source
  when either category is disabled;
- clear all sources, spatial cells, tracked sections, and pending rebuilds on
  disconnect, level replacement, dimension change, and renderer teardown;
- deduplicate section rebuild requests for each update cycle;
- either implement position-only updates correctly or omit that setting;
- preserve resource-driven item luminance, water-sensitive lights, held and
  dropped items, burning entities, projectiles, TNT, and supported modded items;
- expose update counts, active-source counts, nearby candidates, and rebuild
  counts to VRO diagnostics and benchmark archives;
- disable itself when mod ID `dynamiclightsreforged` is installed.

This is functionality consolidation, not a guaranteed FPS improvement. The
feature is disabled by default for the general VRO release because it changes
lighting and moving lights inherently cause render work. A modpack replacing
Dynamic Lights Reforged can enable it in that pack's VRO client configuration.
Shader-active behavior must be separately configurable because shader packs may
provide their own held-light implementation.

Acceptance requires stable lighting with one source and crowded source scenes,
no ghost lights after source removal or world changes, bounded lookup work as
unrelated source counts rise, no repeated rebuild storm, correct shader on/off
transitions, and immediate cleanup after disconnect and dimension changes.

## Implemented dev candidate: Create contraption rendering

The installed Create 0.5.1.i renderer builds one mesh per render layer for an
entire contraption. Create first frustum-tests the contraption's combined AABB,
but once that test passes the complete layer mesh is submitted. Five stationary
contraptions containing roughly 1,600 to 1,800 blocks each can therefore retain
substantial draw work even when only a small portion of each structure is in
view. This remains relevant when the contraptions are not moving.

The VRO dev candidate independently implements these conservative changes:

1. report Flywheel backend state and loaded contraption geometry through
   `/vro create status`;
2. skip the shared buffer flush only when no special contraption block entity
   was eligible to submit geometry;
3. reuse primitive matrix transforms and a mutable light position while
   rendering virtual block entities;
4. frustum-test non-instanced special block entities and movement actors with
   inflated local bounds;
5. split contraptions above a configurable block threshold into cached local
   16-block mesh sections and frustum-test each transformed section;
6. replace unnecessarily spherical render bounds for selected Create machinery
   with conservative directional bounds.

The implementation uses Create's existing `WorldModelBuilder`, model data,
virtual render world, render layers, lighting, textures, and Flywheel shader
binding. It does not implement LOD, block substitution, distance hiding,
reduced animation, asynchronous GL work, or server changes. Both Flywheel and
fallback SuperByteBuffer paths retain explicit invalidation. Compare Mode
reloads Create's world renderers to prevent cross-condition cache reuse.

Research inputs were Create's installed 0.5.1.i bytecode, the official 1.18
source at `b4ebd54c9cf9b1988189d192b3038dbce02af876`, modern Create renderer
architecture, and the public behavioral claims of Create: Catalyst. No
Create: Catalyst code was available or copied.

This candidate requires visual and performance acceptance testing before a
release merge. Its main tradeoff is higher one-time mesh construction and more
draw objects in exchange for less recurring geometry submitted when a large
contraption is only partly visible.

## P3: Static Block-Entity Rendering

### Design sources

- Enhanced Block Entities: https://github.com/FoundationGames/EnhancedBlockEntities
- Optimised Block Entities: https://github.com/maDU59/OptimisedBlockEntities
- Better Block Entities: https://github.com/ceeden/betterblockentities
- Better Beds: https://github.com/TeamMidnightDust/BetterBeds

These projects replace some immediate block-entity rendering with baked models,
terrain/chunk meshes, or hybrid renderers that use immediate rendering only
while animated. This can be valuable in chest-heavy bases because static
geometry no longer incurs a complete block-entity render call every frame.

It is not low-risk for this pack. Custom chests, Sophisticated Storage,
Botania, Create, resource packs, model data, shaders, and animation state all
create invalidation and compatibility requirements. A future implementation
should begin with an exact allowlist of vanilla beds or vanilla chests, keep the
normal renderer as fallback, and rebuild affected chunks whenever animation or
model data changes. Custom block entities must remain untouched until tested
individually.

## P3: Entity and Face Culling

### Design sources

- Entity Culling: https://github.com/tr7zw/EntityCulling
- More Culling: https://github.com/FxMorin/MoreCulling
- Culler: https://github.com/iMeeTake/Culler
- Cull Less Leaves Reforged: https://github.com/CCr4ft3r/CullLessLeavesReforged

Entity Culling performs asynchronous occlusion/path checks and has a Forge
1.18.2 release. It is suitable for a standalone compatibility test, but its
custom protective license means its implementation should not be copied.

More Culling covers block-face, leaf, rain, item-frame/map, and painting
culling. Its author explicitly prohibits merging it into another mod without
permission. Generic first-principles ideas such as avoiding the back face of a
wall-mounted painting may still be researched from Minecraft's renderer, but
the project code should not be incorporated.

Culler and leaf-culling projects intentionally remove distant or interior
visuals. That violates the default no-visible-change rule. They are optional
ideas only and are not recommended for the shipping configuration.

## Existing Projects With Little Backport Value

### FerriteCore

The shipping instance already has FerriteCore 4.2.2, the official 1.18.2 line.
VRO must not duplicate its neighbor maps, property maps, multipart predicate and
model deduplication, model-resource-location handling, block-state shape cache,
baked-quad deduplication, or threading detector. FerriteCore remains the owner
of those foundational systems.

Two independent features were added upstream after 4.2.2 and remain viable for
VRO:

- commit `b63de54a7c40135ba3910608a7f32c263ee29c4f` converts the face lists in
  simple baked models to immutable, right-sized lists and shares a canonical
  all-empty side map. Upstream describes the usual saving as no more than about
  20 MB. This is client-only, does not change rendering, and should coexist with
  FerriteCore 4.2.2;
- commit `187114231d9dd4ed1f843cd78ad00f2f7f503190` canonicalizes identical
  `faceSturdy` boolean arrays in block-state caches. A VRO implementation must
  be independent of FerriteCore internals and explicitly tested with 4.2.2.

Both commits are MIT licensed and require attribution if adapted. Later fixes
to FerriteCore's existing quad cache and fast-map implementation are not VRO
candidates: they modify systems still owned by the installed 4.2.2 jar. Modern
data-component compaction depends on Minecraft systems absent from 1.18.2.

### ModernFix

ModernFix 5.18 already covers many 1.18.2 memory, resource, model, world, and
thread-priority concerns. Newer branches contain promising names such as
attribute-supplier deduplication, compact entity models, profile-texture URL
caching, and tag-ID caching, but they need API-by-API analysis and an overlap
check against VH Accelerator. They are research candidates, not a first batch.

The installed configuration leaves dynamic entity renderers, dynamic resources,
faster item rendering, packet leak repair, and spawn-chunk removal disabled.
Those defaults likely reflect compatibility or behavior tradeoffs and should
not be silently bypassed by VRO.

### Flerovium

Flerovium's camera-basis particle billboard calculation is now adapted under
LGPL-3.0-only from commit
`240f08c62745d57bf200440c9932e0c7907bc5f7`. VRO does not copy Flerovium's
native-memory writer, entity writer, particle culling, item-render LOD, or face
culling. Its geometry is paired with VRO's portable output or the installed
Rubidium/Embeddium packed writer and yields entirely when Flerovium is present.

## Rejected or Deferred

| Project or idea | Decision | Reason |
| --- | --- | --- |
| AsyncParticles / asynchronous particle ticking | Defer | Modded particle and renderer state is frequently not thread-safe |
| Exordium and Gnetum | Reject for shipping | Reduced HUD update rates can freeze or visibly step time-sensitive overlays |
| Video Tape framebuffer cleanup | Reject | The current author describes the approach as unreliable; Cleaner-driven GL teardown can conflict with Oculus/DH ownership |
| GPUBooster framebuffer/VBO pooling | Defer | Broad OpenGL lifecycle changes, shader risk, and substantial version mismatch |
| ThreadTweak and StutterFix | Reject for default | Global thread/yield changes conflict with ModernFix and tuned Java 24 arguments; hardware dependent |
| Krypton/Pluto | Defer | Network optimization offers little direct client FPS; Third Edition already has Connectivity |
| Lithium/Canary/C2ME/Noisium/Starlight | Out of current scope | Primarily server, integrated-world, generation, or lighting architecture; high compatibility cost |
| Lazy Language Loader/Lightspeed/FastQuit | Out of current scope | Startup, reload, or shutdown only; VH Accelerator already owns much of this area |
| Nvidium/VulkanMod | Reject | Vendor/backend constraints and shader incompatibility |
| Saturn | Reject | Source/provenance is unavailable and behavior overlaps FerriteCore/ModernFix |
| Fast Paintings | Reject | No usable source and restrictive distribution terms |
| Dynamic FPS | Not relevant | Saves resources while unfocused; does not improve active gameplay FPS |

## Validation Gates

Every candidate must pass these gates before it is considered shippable:

1. Build against every supported Vault jar using
   `scripts/build-pack-compatibility.ps1`.
2. Test in a copy of Vault Hunters Third Edition with no custom-instance-only
   performance mod accidentally present.
3. Record an equivalent baseline and candidate Spark client profile in the same
   location, time of day, camera path, and workload.
4. Measure frame-time percentiles and allocations, not only average FPS.
5. Exercise particle-heavy mob deaths, item processing, dense block entities,
   animated text, HUD overlays, inventories, maps, and resource reloads.
6. Exercise disconnect/reconnect, dimension changes, death/respawn, window
   resizing, fullscreen changes, and a multi-hour session.
7. In a separate compatibility instance, test shaders off/on/reload plus Oculus
   and Distant Horizons toggles. No renderer optimization may retain stale
   framebuffer or world state.
8. Compare screenshots or deterministic captures. Default optimizations must not
   suppress visible content or alter lighting, animation timing, transparency,
   or model state.
9. Confirm automatic coexistence behavior with each original standalone mod.
10. Archive profiles, logs, configuration, jar hash, and exact commit used for
    every accepted result.

## Source Revision Ledger

All revisions below were inspected on 2026-08-01. A revision identifies the
research input; it does not imply code was copied.

| Project | Revision | License observed | Notes |
| --- | --- | --- | --- |
| AsyncParticles | `87636d03147a5f8ceaa35540f8ac6ce63acdbb61` | LGPL-3.0 | Modern branch; deferred multithreading reference |
| BadOptimizations | `5de4a3ad4299909178d8995dc0bc80626be48d44` | MIT | Primary zero-work and cache reference |
| Better Fps - Render Distance 1.18.2 | `6ada7eeb3f07c98f29bb15d955234f03766ca915` | All rights reserved | Behavioral reference only; independent implementation required |
| Better Block Entities | `cb2937e94ec9399f0f9f54905aff81b9ae5c1797` | LGPL-3.0+ | Modern block-entity reference |
| Better Beds | `0a9f7ea0a1cdb0b1924492da4489414cd627fb49` | MIT | Narrow static bed-model reference |
| Cull Less Leaves Reforged | `c132993ad7968b43a4d49986d656a4d3ce087684` | LGPL-3.0 | Optional visual compromise only |
| Culler | `da5e258ec3d6f966ffec4d4ffede76a9764a2377` | MIT | Distance-culling reference |
| Dynamic Lights Reforged 1.18 | `d85b337f8f7af328d78e8d380f19fc9b95e93318` | MIT | Legacy behavior and provenance reference; corrected independent subsystem candidate |
| Enhanced Block Entities | `b0b202a18a1acbddfc038e5403bde973bece7c98` | LGPL-3.0 | Static/hybrid block-entity reference |
| Entity Culling | `2b5135a5b6003235b57cb16fa61b312fbc03cc66` | tr7zw Protective License | Standalone test only |
| Exordium | `15f93fe30cc105d9e58c1d4a26eadc6cd27a9333` | LGPL-3.0 | HUD throttling rejected |
| FerriteCore | `0cef1f2add1f1329aa6e690e8e292acd625c5c6d` | MIT | Modern branch comparison |
| FerriteCore model-side compaction | `b63de54a7c40135ba3910608a7f32c263ee29c4f` | MIT | Post-4.2.2 independent VRO candidate |
| FerriteCore `faceSturdy` deduplication | `187114231d9dd4ed1f843cd78ad00f2f7f503190` | MIT | Post-4.2.2 independent VRO candidate |
| Flerovium | `240f08c62745d57bf200440c9932e0c7907bc5f7` | LGPL-3.0-only | Adapted camera-basis particle geometry |
| Gnetum | `8eb41b5c9399c3bf1864803a96571853674fb475` | LGPL-3.0 | HUD throttling rejected |
| GPUBooster | `5ed54b4936dac15f1e9f0208037173d241fa428f` | GPL-3.0 | OpenGL lifecycle work deferred |
| ImmediatelyFast | `ead67a194e5e330d0a410adeec092bd0ca5d19d6` | LGPL-3.0 | Modern branch comparison |
| ImmediatelyFast 1.18.2 | `acc0bc96e9dc60174cc42c4b0cf16cac891e7446` | LGPL-3.0 | Discontinued official 1.18.2 branch |
| ImmediatelyFastReforged | `903a113b2cdef681d39d42f44599de9e8c39088d` | LGPL-3.0 | Forge port reference |
| Jasione | `2e5d28d25e05660f7b7f9e8bdfb0a9d5f380168f` | LGPL-3.0-only | Tag 1.0.6; includes Forge 1.18.2 target |
| Krypton | `e5f006ad6ebbb44572114f165d7bcc2406eabb54` | LGPL-3.0 | Network work deferred |
| Lazy Language Loader | `58a5c0cc2b76564c51497d3439168515855ea225` | LGPL-3.0 | Startup/reload only |
| Lithium | `c42972b6e9d21c8ff45559df6b271802050a22e2` | LGPL-3.0 | Mostly non-render architecture reference |
| MemoryLeakFix | `988f54c14db0d86e13dd5dcce284178b2278e581` | LGPL-2.1-only plus author merge policy | Standalone test; permission required to merge |
| ModernFix | `535b389e1b9858a007f1eda095f95f41f71a6bef` | LGPL-3.0 | Current branch comparison |
| ModernFix 1.18.2 | `94c848b0debbb5291ab3c709353e3f11613fd14d` | LGPL-3.0 | Branch/tag basis for installed 5.18 behavior |
| More Culling | `3d3c1bdb1dd2b90c820e6fef1cbe0b8f7f19d787` | GPL-3.0 plus author merge policy | Permission required to merge |
| Optimised Block Entities | `7f50c538b7370c6936e186a268d37ce33644162f` | LGPL-3.0+ | Modern block-entity reference |
| Particle Core | `1151fe6aca4e1c3b62459de3e3a99ec32af2ac99` | MIT | Primary particle reference |
| StutterFix | `05eb1855e3812687c130b8e11515ef923543d954` | MIT | Global scheduling change rejected |
| ThreadTweak | `d418794b049f29b128c877b2cf030346b6df4ac5` | MIT | Global scheduling change rejected |
| Create 1.18 renderer | installed `0.5.1.i`; source comparison `b4ebd54c9cf9b1988189d192b3038dbce02af876` | MIT code / ARR assets | Installed bytecode plus older 1.18 source branch; glue and contraption renderer investigation |
| Create modern renderer | `87b3c6a65fd00c023a07b37b0353144bc7e6a5bf` | MIT code / ARR assets | Newer contraption visual architecture and changelog comparison |
| Create: Catalyst | CurseForge project `1620723`, inspected 2026-08-15 | All rights reserved | Behavioral claims only; no source copied |
| Unobtanium | `bb3e8a523807f919d217fa8008fefcb4b8e7fb13` | AGPL-3.0-or-later | Ability adaptation plus independently implemented world, empty-stack, Vault Loot Beams, and iSpawner design references |
| Video Tape | `4b330488fb9510fe9ff8e7b02aee8b8016a1e56e` | MIT-0 | Framebuffer cleanup rejected |

Before implementing from any entry, refresh its upstream repository, record the
new exact revision, review its current license and author policy, and add a
specific notice describing what was independently adapted.
