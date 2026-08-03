# Third-Party Notices

This file records shipped adaptations separately from projects used only for
research. Exact revisions are provided so the provenance of every retained
mechanism can be reconstructed.

## Shipped adaptations

### Unobtanium learned-ability cache

VRO's `ClientAbilityDataMixin` is adapted from Unobtanium's
`MixinClientAbilityData` implementation.

- Project: Unobtanium by iwolfking and contributors
- Relevant contributor: radimous
- Source: https://github.com/iwolfking/unobtanium
- Source commit: https://github.com/iwolfking/unobtanium/commit/7bf6a6585014e07b9fca622482ce40e83b73d8e8
- License: GNU Affero General Public License v3.0 or later
- Adaptation: retains the learned-ability list until Vault's ability update
  packet invalidates it; VRO adds Compare Mode ownership and conservative
  first-result publication.

Because this adaptation is retained, VRO is distributed under the GNU Affero
General Public License v3.0 or later. The full license is included in
`LICENSE` and in the release jar.

VRO's broader Vault event-listener snapshot cache is not derived from
Unobtanium. VRO's implementation predates the later biome-only implementation
contributed to the HoYin1600p Unobtanium fork.

### Entity Collision FPS Fix

VRO's client collision mixins are adapted from CorgiTaco's archived
`No-Client-Side-Entity-Collision-Checks` / `Entity Collision FPS Fix` project.

- Project: Entity Collision FPS Fix by CorgiTaco
- Source: https://github.com/CorgiTaco-Archive/No-Client-Side-Entity-Collision-Checks
- Inspected revision: `cc16e1843f592084e6ebea64d2c7399e5189fa09`
- License: CC0 1.0 Universal
- Adaptation: VRO adds its Compare Mode gate and automatically yields when the
  standalone `entitycollisionfpsfix` mod is installed.

### FerriteCore post-4.2.2 memory reductions

VRO's simple-model face-list compaction and block-state `faceSturdy` array
canonicalization are adapted for Minecraft 1.18.2 from later FerriteCore work.

- Project: FerriteCore by malte0811 and contributors
- Source: https://github.com/malte0811/FerriteCore
- Model-side reference commit: `b63de54a7c40135ba3910608a7f32c263ee29c4f`
- `faceSturdy` reference commit: `187114231d9dd4ed1f843cd78ad00f2f7f503190`
- License: MIT
- Adaptation: VRO uses project-owned compactors and a thread-safe array
  interner, while preserving compatibility with the installed FerriteCore
  4.2.2 systems that own the remaining block-state and model caches.

## Independently implemented from design research

No source files or implementation blocks from the projects in this section
were copied into VRO. Their public implementations were studied to identify
safe optimization boundaries, then VRO was written directly against Minecraft
Forge 1.18.2 with its own state, invalidation, configuration, and coexistence
rules.

### Particle Core

- Project: Particle Core by fzzyhmstrs
- Source: https://github.com/fzzyhmstrs/pc
- Inspected revision: `1151fe6aca4e1c3b62459de3e3a99ec32af2ac99`
- License observed at inspection: MIT
- Design influence: reuse repeated particle light lookups while position and
  client tick are unchanged.

VRO does not port Particle Core's particle suppression, distance limits,
asynchronous ticking, movement cache, or renderer implementation. Forge 1.18.2
already supplies particle frustum culling, so that feature was not duplicated.

### BadOptimizations

- Project: BadOptimizations by Thosea
- Source: https://github.com/imthosea/BadOptimizations
- Inspected revision: `5de4a3ad4299909178d8995dc0bc80626be48d44`
- License observed at inspection: MIT
- Design influence: avoid empty render/tick setup and cache stable renderer
  lookups.

VRO deliberately excludes BadOptimizations' shader-sensitive lightmap and sky
color caching. Equivalent VRO mixins disable themselves when BadOptimizations
is installed.

### Unobtanium world-retention research

Unobtanium's Create Addition and Powah memory-leak work identified these mods'
world-keyed static maps as long-session retention risks.

- Source: https://github.com/iwolfking/unobtanium
- Create Addition reference commit:
  `bbbdb54b9541946608aafa26911dbd8dbf83356b`
- Client-level reference commit:
  `a5cbc768977d883eadc8b034b78e9db3e4774325`
- License: GNU Affero General Public License v3.0 or later

VRO does not copy those mixins. It independently listens for Forge world-unload
events and removes only the exact unloaded world from the existing maps instead
of replacing collection types or cancelling network behavior.

## Compatibility behavior inspected

VRO interoperates with Minecraft Forge, SpongePowered Mixin, Vault Hunters,
Vault Integrations, Powah, Create Crafts & Additions, Xaero's World Map,
Embeddium/Rubidium render stacks, and optional optimization mods. Their code and
assets are not bundled. Locally installed Vault jars were inspected only to
identify stable public method and field layouts across supported pack versions.

The complete research ledger, including rejected and unimplemented projects,
is in `docs/PERFORMANCE_BACKPORT_RESEARCH.md`.
