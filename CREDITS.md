# Credits and research attribution

The Vault Render Optimization combines original project work, explicitly
adapted open-source mechanisms, independent reimplementations informed by
public research, and compatibility behavior learned from installed APIs. These
relationships are intentionally separated below.

## Shipped adapted implementations

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
client tick and block position. It does not port Particle Core's renderer,
asynchronous ticking, movement caching, spawn suppression, or particle limits.

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
inside this project.

### Vault Integrations and Powah

Client crash reports and installed runtime behavior identified deterministic
stale states in altar conduit ticking and Powah cable replacement. VRO's guards
repair those states on the physical client and leave server behavior unchanged.
No source from either project is bundled.

### Create Crafts & Additions

Create Addition's world-keyed energy-network manager is accessed only when the
mod is present. VRO removes an exact unloaded world during Forge's unload event.
The mod remains otherwise untouched.

### Xaero's World Map

Xaero's World Map exposed a Forge key-consumption conflict with The Vault's
default `M` binding. VRO changes only when The Vault's key context is active so
Xaero can receive `M` outside Vaults.

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
