# Credits and research attribution

The Vault Render Optimization combines original project work, explicitly
adapted open-source mechanisms, independent reimplementations informed by
public research, and compatibility behavior learned from installed APIs. These
relationships are intentionally separated below.

## Shipped adapted implementations

### Embeddium stability fork - embeddedt, JellySquid, HoYin1600p, and contributors

- Upstream: [embeddedt/Embeddium](https://github.com/embeddedt/embeddium)
- Transfer ledger revision: `7b085088`
- License: LGPL-3.0

VRO independently packages guarded 1.18.2 mixins implementing eight renderer
corrections first developed or assembled in HoYin1600p's Embeddium stability
fork. Exact source commits, supported renderer layouts, ownership controls,
tests, and removal gates are recorded in
[`docs/EMBEDDIUM_OWNERSHIP_TRANSFER.md`](docs/EMBEDDIUM_OWNERSHIP_TRANSFER.md).
The original fork remains untouched until every removal gate passes.

### ModernFix - embeddedt, Fury_Phoenix, and contributors

- Source: [embeddedt/ModernFix](https://github.com/embeddedt/ModernFix)
- Relocated VHA baseline: `7d2a69943e6e5bee629759b5ad0c4c47f6a20980`
- License: LGPL-3.0-or-later

VRO includes eleven ModernFix-derived client render/graphics adaptations for
Minecraft Forge 1.18.2: chunk meshing, duplicate BufferBuilder protection,
entity-model cube compaction, bounded profile-texture hashing, model selector
and variant traversal caches, transformation hashing, OBJ cache concurrency,
guarded STB atlas stitching, Forge model-data concurrency, and CTM metadata
concurrency. Exact source paths, revisions, adaptation notes, ownership rules,
and compatibility gates are recorded in
[`docs/MODERNFIX_RENDER_BACKPORTS.md`](docs/MODERNFIX_RENDER_BACKPORTS.md).

The STB stitcher also preserves ModernFix's credit to
[GTNewHorizons/lwjgl3ify](https://github.com/GTNewHorizons/lwjgl3ify) at
`f21364cd3d178aef863458a2faa1f5718a4e350d` under LGPL-3.0. The complete LGPL
text is retained in the repository and runnable jar.

### Flerovium - MoePus and contributors

- Source: [MoePus/Flerovium](https://github.com/MoePus/Flerovium)
- Adapted source revision: `240f08c62745d57bf200440c9932e0c7907bc5f7`
- Adapted source path:
  `src/main/java/com/moepus/flerovium/mixins/Particle/SingleQuadParticleMixin.java`
- License: LGPL-3.0-only

VRO adapts Flerovium's camera-left/up particle billboard calculation, which
avoids rotating four separate corners with a quaternion. VRO isolates the
calculation in a reusable allocation-free geometry helper, adds a portable
Minecraft writer, reuses Rubidium/Embeddium's packed particle writer when it
is installed, and supplies hot ownership/configuration and diagnostics.
Flerovium remains the owner when installed, preventing duplicate output. The
complete upstream LGPL text is retained in the repository and runnable jar.

### Forge Update Notifier - HoYin1600p

- Source: [HoYin1600p/Forge-Update-Notifier](https://github.com/HoYin1600p/Forge-Update-Notifier)
- Integrated revision: `4f5cacebf9543c8f94a93fd070ae762fcd1d4e9c`
- Canonical VHA source revision: `483acee`
- License: MIT

VRO includes a package-relocated copy of the canonical Forge 1.18.2 update
notifier. The copied unit supplies bounded asynchronous manifest fetching,
version and severity parsing, coordinated menu rows, once-per-JVM reminder
cadence, persistence, and failure handling. VRO adds its own initialization,
client configuration, `/vro updates` commands, GitHub manifest, CurseForge
link, Forge metadata, documentation, and release workflow. The complete MIT
license is retained in the repository and release jar.

### Iris & Oculus Flywheel Compat - Red Face and contributors

- Source: [leon-o/iris-flw-compat](https://github.com/leon-o/iris-flw-compat)
- Adapted baseline: `7a981f9a845b402b49a82b4dae8d814b3480137e`
- License: MIT

VRO adapts the project's Flywheel shader-program compiler, extended vertex
format, render-layer integration, and Oculus pipeline access for Minecraft
1.18.2. VRO adds public Oculus/Rubidium build targeting, strict startup gates,
runtime configuration and diagnostics, Compare Mode ownership, automatic
fallback, and a VRO-owned shader-uniform accessor. The upstream copyright and
full MIT license are retained in the repository and release jar.

### Unobtanium - iwolfking, radimous, and contributors

- Source: [iwolfking/unobtanium](https://github.com/iwolfking/unobtanium)
- Relevant implementation:
  [`7bf6a658`](https://github.com/iwolfking/unobtanium/commit/7bf6a6585014e07b9fca622482ce40e83b73d8e8)
- License: AGPL-3.0-or-later

`radimous` implemented Unobtanium's learned client ability-list cache. VRO's
`ClientAbilityDataMixin` adapts that implementation, retaining the result until
Vault's ability update packet invalidates it. VRO adds its own Compare Mode
ownership and conservative cache publication.

This is adapted code, not design-only inspiration. VRO therefore ships under
the compatible AGPL-3.0-or-later license and includes the complete license in
the repository and jar.

Unobtanium also identified long-lived Create Addition and Powah world maps as
retention risks. VRO's cleanup for those maps was independently implemented
using Forge world-unload events and exact-key removal; those cleanup classes do
not copy Unobtanium's collection replacement or cancellation mixins.

Later Unobtanium work by `radimous` also identified avoidable retention through
Minecraft's shared empty item stack, eager Vault Loot Beams tooltip work, and
iSpawner renderer allocations. The relevant research commits are
[`a72a669`](https://github.com/iwolfking/unobtanium/commit/a72a6699ff36ace7237aa9e1458da84736adc0e3)
and
[`f491eb4`](https://github.com/iwolfking/unobtanium/commit/f491eb48c4f6f0e0d13fb6c94b29cb3b56add04d).
VRO's implementations were written independently against the installed
Minecraft 1.18.2 APIs. They use different injection boundaries, Compare Mode
behavior, unload cleanup, and coexistence rules; no Unobtanium implementation
block was copied.

VRO's Vault event-listener snapshot cache originated in VRO in June 2026. A
biome-only version was later contributed by HoYin1600p to an Unobtanium fork,
so that overlap does not represent code copied from Unobtanium into VRO.

### Entity Collision FPS Fix - CorgiTaco

- Source:
  [CorgiTaco-Archive/No-Client-Side-Entity-Collision-Checks](https://github.com/CorgiTaco-Archive/No-Client-Side-Entity-Collision-Checks)
- Inspected revision: `cc16e1843f592084e6ebea64d2c7399e5189fa09`
- License: CC0 1.0 Universal

VRO adapts the standalone mod's client-only wall-check and entity-push mixins.
VRO adds Compare Mode integration and automatic coexistence: when
`entitycollisionfpsfix` is installed, VRO does not apply its equivalent mixins.

### FerriteCore - malte0811, KJP12, and contributors

- Source: [malte0811/FerriteCore](https://github.com/malte0811/FerriteCore)
- Model-side reference: `b63de54a7c40135ba3910608a7f32c263ee29c4f`
- `faceSturdy` reference: `187114231d9dd4ed1f843cd78ad00f2f7f503190`
- License: MIT

VRO adapts the two memory reductions added after FerriteCore 4.2.2: compact
simple-model face lists and canonical block-state `faceSturdy` arrays. VRO does
not replace FerriteCore's existing 1.18.2 shape, property, quad, or model
caches; the additions are deliberately limited to data 4.2.2 does not own.

## Independently implemented design influences

### Particle Core - fzzyhmstrs

- Source: [fzzyhmstrs/pc](https://github.com/fzzyhmstrs/pc)
- Inspected revision: `1151fe6aca4e1c3b62459de3e3a99ec32af2ac99`
- License observed during research: MIT

Particle Core demonstrated the value of avoiding repeated particle-light
queries. VRO independently implemented a narrow Forge 1.18.2 cache keyed by
client tick and block position, then extended it with a bounded same-position
cache shared by particles during that tick. It does not port Particle Core's
renderer, asynchronous ticking, movement caching, spawn suppression, or
particle limits.

### BadOptimizations - Thosea

- Source: [imthosea/BadOptimizations](https://github.com/imthosea/BadOptimizations)
- Inspected revision: `5de4a3ad4299909178d8995dc0bc80626be48d44`
- License observed during research: MIT

BadOptimizations informed empty-work exits and stable renderer lookup caching.
VRO's Forge 1.18.2 implementations use separate classes, configuration,
resource-reload invalidation, and mod-coexistence gates. Shader-sensitive
lightmap and sky-color caches were deliberately rejected.

### Better Fps - Render Distance - someaddons

- Source: [someaddons/betterfpsdistances](https://github.com/someaddons/betterfpsdistances)
- Inspected revision: `6ada7eeb3f07c98f29bb15d955234f03766ca915`
- License observed during research: all rights reserved

The project established the general usefulness of separating terrain draw
distance from loaded chunk distance. VRO's implementation was written
independently and uses different bounds, configuration, mixins, renderer
integration, and defaults. No source code or formulas from the reference mod
are included.

### Dynamic Lights Reforged - LambdAurora and Forge port contributors

- Source: [Dynamic Lights Reforged](https://github.com/TeamDeusVult/Dynamic-Lights-Reforged)
- Inspected revision: `d85b337f8f7af328d78e8d380f19fc9b95e93318`
- License observed during research: MIT

The project informed the expected 1.18.2 feature surface: visual lightmap
combination, held and dropped item sources, water-sensitive definitions, and
terrain rebuild invalidation. VRO's implementation was written independently
with project-owned source state, 16-block spatial indexing, per-source tick
scheduling, coalesced section rebuilds, Forge resource loading, shader policy,
diagnostics, cleanup, and coexistence gates. No Dynamic Lights Reforged source
file is included.

## Compatibility and behavior sources

### Vault Hunters - Iskallia and contributors

Vault Hunters is VRO's primary compatibility target. Public runtime APIs and
locally installed, authorized pack jars were inspected to identify repeated
client work and stable layouts across official, Remastered, Wolds, and custom
versions. VRO does not bundle Vault Hunters classes, source, assets, or
decompiled output.

The newer Asgard Vault behavior informed use of Vault's own `GearDataCache` for
armor state. VRO's cache, invalidation, and mixin implementation were written
inside this project. The elixir-orb number fix was also developed from an
in-game render regression and the installed Vault particle API; VRO uses an
isolated text buffer and restores Minecraft's particle state without copying
Vault source.

### Vault Integrations and Powah

Client crash reports and installed runtime behavior identified deterministic
stale states in altar conduit ticking and Powah cable replacement. VRO's guards
repair those states on the physical client and leave server behavior unchanged.
The Powah unload cleanup uses a narrow Forge access transformer only to make
Powah's package-private `CableNet` class reachable by VRO's optional accessor;
the cache field remains private and normal cable behavior is not replaced. No
source from either project is bundled.

### Create Crafts & Additions

Create Addition's world-keyed energy-network manager is accessed only when the
mod is present. VRO removes an exact unloaded world during Forge's unload event.
The mod remains otherwise untouched.

### Xaero's World Map

Xaero's World Map exposed a Forge key-consumption conflict with The Vault's
default `M` binding. VRO changes only when The Vault's key context is active so
Xaero can receive `M` outside Vaults.

## Index-only terrain transparency sorting

Embeddium's embeddedt, Rubidium's NanoLive, Sodium/CaffeineMC and contributors
provide the original 1.18.2 sorting and GPU arena contracts. VRO's index-only
task/result/upload adapter was developed against HoYin1600p's fork at
`d95f90d1edb990943b30663b2e95a02ea5e7c2a8` and stock Embeddium 0.3.18.
The adapter files retain LGPL-3.0-only notices; the complete upstream LGPL/GPL
texts are included in `docs/licenses/embeddium-LGPL-3.0-only.txt` and the JAR.
VRO's overall project license remains AGPL-3.0-or-later.

Newer Sodium's separate index uploads inspired the optimization direction;
no newer Sodium implementation was copied. The native 1.18.2 sorter is called
unchanged. See `docs/INDEX_ONLY_SORTING.md` for precise inspected source paths.

## Native asynchronous chunk updates

Sodium, Embeddium and Rubidium contributors implemented the native deferred
chunk-update behavior used by VRO's default-on frame-pacing integration.
Minecraft/Forge provide the vanilla asynchronous compilation path. VRO adds
independent configuration, guarded field-read mixins and commands, without
copying or replacing their schedulers. Inspected versions and the exact local
fork revision are recorded in [the feature notes](docs/CHUNK_UPDATE_DEFERRAL.md).
No licensing change is required for these fresh VRO integration hooks.

## Foundations

VRO is built with and interoperates with:

- [Minecraft Forge](https://github.com/MinecraftForge/MinecraftForge)
- [SpongePowered Mixin](https://github.com/SpongePowered/Mixin)

Their APIs and implementation behavior make the mod possible. Their copyrights
and licenses remain with their authors.

## Research-only projects

Additional performance projects were reviewed to establish safety boundaries,
overlap rules, rejected approaches, and future candidates. None of their code
is shipped merely because it appears in the research ledger. The exact
revision and license table is maintained in
[`docs/PERFORMANCE_BACKPORT_RESEARCH.md`](docs/PERFORMANCE_BACKPORT_RESEARCH.md).

That ledger includes Particle Core, BadOptimizations, Flerovium,
MemoryLeakFix, Jasione, ImmediatelyFast, ImmediatelyFastReforged, Enhanced
Block Entities, Optimised Block Entities, Better Block Entities, Better Beds,
Entity Culling, More Culling, FerriteCore, ModernFix, and other accepted,
deferred, or rejected references.

## Attribution policy

Every future implementation materially informed by another project must record
the project, author, exact inspected revision, license, and relationship in
this file and `THIRD_PARTY_NOTICES.md` before release. Credit applies to copied
or adapted code, design research, compatibility discoveries, rejected ideas,
and test methods, but those categories must never be described as equivalent.
