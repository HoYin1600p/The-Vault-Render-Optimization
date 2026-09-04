# Third-Party Notices

This file records shipped adaptations separately from projects used only for
research. Exact revisions are provided so the provenance of every retained
mechanism can be reconstructed.

## Shipped adaptations

### Embeddium stability-fork renderer corrections

VRO contains fresh, optional mixin adaptations of eight renderer corrections
from HoYin1600p's Embeddium stability fork at transfer-ledger revision
`7b085088`. Embeddium derives from Sodium and is licensed under LGPL-3.0.
Each VRO source file records its exact behavioral source commit. The complete
LGPL terms already retained in `docs/licenses/modernfix-LGPL-3.0-or-later.txt`
cover LGPL-3.0 redistribution and are embedded in the runnable jar. VRO as a
whole remains AGPL-3.0-or-later.

Complete implementation and validation status is recorded in
`docs/EMBEDDIUM_OWNERSHIP_TRANSFER.md`.

### ModernFix render and graphics backports

VRO contains selected source adapted from ModernFix for Minecraft Forge 1.18.2.

- Project: ModernFix by embeddedt and contributors
- Source: https://github.com/embeddedt/ModernFix
- Relocated VHA baseline: `7d2a69943e6e5bee629759b5ad0c4c47f6a20980`
- Copyright: embeddedt, Fury_Phoenix, and the contributors identified in the
  retained source headers and ModernFix history
- License: LGPL-3.0-or-later
- Complete provenance: `docs/MODERNFIX_RENDER_BACKPORTS.md`

Every adapted Java file retains its LGPL SPDX identifier, upstream source path,
exact source revision, original copyright, and VRO adaptation note. VRO's
project license remains AGPL-3.0-or-later. The complete ModernFix LGPL text is
retained at `docs/licenses/modernfix-LGPL-3.0-or-later.txt` and embedded in the
runnable jar.

ModernFix's STB atlas stitcher adapts work from GTNewHorizons/lwjgl3ify:

- Source: https://github.com/GTNewHorizons/lwjgl3ify
- Revision: `f21364cd3d178aef863458a2faa1f5718a4e350d`
- License: LGPL-3.0

VRO preserves that transitive source and license attribution.

### Flerovium particle billboard geometry

VRO's particle geometry helper and ordinary-particle render injections adapt
Flerovium's camera-basis billboard calculation.

- Project: Flerovium by MoePus and contributors
- Source: https://github.com/MoePus/Flerovium
- Source commit: `240f08c62745d57bf200440c9932e0c7907bc5f7`
- Source path:
  `src/main/java/com/moepus/flerovium/mixins/Particle/SingleQuadParticleMixin.java`
- License: LGPL-3.0-only
- Adaptation: reusable allocation-free geometry, a renderer-neutral
  `VertexConsumer` fallback, Rubidium/Embeddium packed-writer integration, hot
  ownership and configuration, Compare Mode, and diagnostics.

VRO does not include Flerovium's native-memory writer, entity writer, particle
culling, or other mixins. Flerovium wins ownership when installed. VRO's
project license remains AGPL-3.0-or-later, and the complete upstream LGPL text
is retained at `docs/licenses/flerovium-LGPL-3.0.txt` and embedded in the
runnable jar.

### Forge Update Notifier

VRO's client update-notification unit under `client/update` is a
package-relocated copy of Forge Update Notifier.

- Project: Forge Update Notifier by HoYin1600p
- Source: https://github.com/HoYin1600p/Forge-Update-Notifier
- Integrated revision: `4f5cacebf9543c8f94a93fd070ae762fcd1d4e9c`
- Canonical VHA source revision: `483acee`
- License: MIT
- Adaptation: uses VRO's client initialization, client config, `/vro updates`
  commands, raw GitHub manifest, fixed CurseForge project URL, Forge metadata,
  documentation, and release process while preserving the canonical bounded,
  fail-closed request and once-per-JVM reminder behavior.

This is adapted source code. The complete MIT license is retained at
`docs/licenses/forge-update-notifier-MIT.txt` and embedded in the runnable jar.

### Iris & Oculus Flywheel Compat

VRO's optional Create shader-instancing bridge under
`compat/flywheelshader` is adapted from Iris & Oculus Flywheel Compat.

- Project: Iris & Oculus Flywheel Compat by Red Face and contributors
- Source: https://github.com/leon-o/iris-flw-compat
- Adapted baseline: `7a981f9a845b402b49a82b4dae8d814b3480137e`
- Copyright: Copyright (c) 2022 Red Face
- License: MIT
- Adaptation: targets public Oculus 1.6.4 and Rubidium 0.5.6, adds strict
  optional-mod and version gates, VRO configuration and Compare Mode control,
  an early startup recovery switch, diagnostics, automatic renderer refresh,
  and a project-owned accessor for Minecraft's final shader uniform field.

This is adapted source code. The complete upstream MIT license is retained at
`docs/licenses/iris-flw-compat-MIT.txt` and embedded in the runnable jar.

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
  client tick are unchanged and share identical same-tick block-light results.

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

### Better Fps - Render Distance

- Project: Better Fps - Render Distance by someaddons
- Source: https://github.com/someaddons/betterfpsdistances
- Inspected revision: `6ada7eeb3f07c98f29bb15d955234f03766ca915`
- License observed at inspection: all rights reserved
- Design influence: make terrain rendering distance independently controllable
  from chunk loading.

No source, mixin structure, configuration code, or distance formula from Better
Fps - Render Distance is included. VRO uses its own camera-to-section bounds,
vanilla render context, Embeddium/Rubidium render-list filter, configuration,
and coexistence gate.

### Dynamic Lights Reforged

- Project: Dynamic Lights Reforged, based on LambDynamicLights by LambdAurora
- Source: https://github.com/TeamDeusVult/Dynamic-Lights-Reforged
- Inspected revision: `d85b337f8f7af328d78e8d380f19fc9b95e93318`
- License observed at inspection: MIT
- Design influence: expected dynamic-light sources, resource-defined item
  luminance, water sensitivity, lightmap combination, and terrain invalidation.

VRO does not include a Dynamic Lights Reforged source file. Its engine was
implemented independently using 16-block spatial cells, independent per-source
scheduling, deduplicated end-of-tick section rebuilds, explicit world cleanup,
Oculus-aware policy, diagnostics, and a startup coexistence gate. VRO does not
apply its dynamic-light mixins when `dynamiclightsreforged` is installed.

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

### Unobtanium dropped-item and optional-renderer research

Later Unobtanium fixes identified three additional client costs:

- Minecraft's shared `ItemStack.EMPTY` retaining an item entity;
- Vault Loot Beams eagerly generating tooltip data for every dropped item;
- iSpawner allocating a stream and list while choosing its displayed item.

- Project: Unobtanium by iwolfking and contributors
- Relevant contributor: radimous
- Source: https://github.com/iwolfking/unobtanium
- Empty-stack reference commit:
  `a72a6699ff36ace7237aa9e1458da84736adc0e3`
- Optional-renderer research commit:
  `f491eb48c4f6f0e0d13fb6c94b29cb3b56add04d`
- License: GNU Affero General Public License v3.0 or later

No code from those implementations is included. VRO independently redirects
only the empty-stack assignment, defers only Vault Loot Beams' eager cache
call while preserving its normal lazy query, clears retained tooltip entries
on world unload, and performs two direct iSpawner inventory passes. VRO keeps
iSpawner's original display interval, copied stack, and view distance. These
mixins yield when Unobtanium is installed.

### Create contraption-rendering research

- Project: Create by simibubi and contributors
- Source: https://github.com/Creators-of-Create/Create
- Installed target: Create `0.5.1.i` for Minecraft 1.18.2
- Source comparison revision: `b4ebd54c9cf9b1988189d192b3038dbce02af876`
- License observed for code: MIT
- Additional behavioral reference: Create: Catalyst, CurseForge project
  `1620723`, inspected 2026-08-15, all rights reserved

No Create or Create: Catalyst source file or implementation block was copied
into VRO. The installed Create jar and official 1.18 source were inspected to
identify the exact render lifecycle and compatibility boundaries. VRO then
implemented its own fixed-section grouping, transformed frustum tests,
diagnostic counters, cache lifecycle, and configuration directly against the
public Create/Flywheel APIs. Create: Catalyst was used only as evidence that
contraption rendering is a practical optimization target; its code was not
available or used.

VRO does not bundle Create, Flywheel, or their assets. Sectioned contraption
meshes are generated at runtime from Create's existing models and preserve the
same render layers, model data, lighting, and textures. The directional
machinery bounds are independently calculated from each installed block
entity's position, facing, extension, or belt chain direction.

## Index-only terrain transparency sorting

- Behavioral source: Embeddium 0.3.18 and HoYin1600p's Embeddium fork at
  `d95f90d1edb990943b30663b2e95a02ea5e7c2a8` (Minecraft 1.18.2).
- Upstream authors: embeddedt, NanoLive, JellySquid/CaffeineMC and contributors.
- Source project: https://github.com/embeddedt/embeddium
- Source license as declared in that branch's `mods.toml`: LGPL-3.0-only.
- Source paths under `me/jellysquid/mods/sodium/client/`:
  `render/chunk/tasks/ChunkRenderSortTask.java`,
  `render/chunk/compile/ChunkBufferSorter.java`,
  `render/chunk/compile/ChunkBuildResult.java`,
  `render/chunk/region/RenderRegionManager.java`, and
  `render/chunk/ChunkGraphicsState.java`.

VRO's `IndexOnlySortTask`, `IndexOnlySortResult` and `IndexOnlyUploads` adapt
these contracts with explicit LGPL-3.0-only source notices. They reuse the
installed native sorter, retain geometry, and own only replacement index
results. The new policy, generation checks, controls and tests are VRO-owned.
No complete renderer or newer Sodium code is bundled. The complete LGPLv3
and GPLv3 texts from the source repository are retained together at
`docs/licenses/embeddium-LGPL-3.0-only.txt` and embedded in the production JAR.
VRO's overall project license remains AGPL-3.0-or-later.

## Adaptive deferred chunk budgets

Behavioral contracts inspected in HoYin1600p's Embeddium stability fork revision
`d95f90d1edb990943b30663b2e95a02ea5e7c2a8` (LGPL-3.0-only), and stock Embeddium
0.3.18: `render/chunk/RenderSectionManager.java`,
`render/chunk/compile/ChunkBuilder.java`, `render/chunk/compile/ChunkBuildResult.java`,
`render/chunk/tasks/ChunkRenderBuildTask.java` and
`render/chunk/region/RenderRegionManager.java` under the native Sodium namespace.
Upstream authors: embeddedt, JellySquid, the Rubidium lineage and contributors.

`TimedChunkTask` and `EmbeddiumAdaptiveBudgetMixin` are fresh VRO adaptations
of those lifecycle contracts, marked LGPL-3.0-only. The feedback policy,
bounded observer/drain, compatibility checks, diagnostics and tests are VRO-owned.
No full renderer or newer Sodium code is shipped. Full upstream LGPLv3/GPLv3
texts are already included in `docs/licenses/embeddium-LGPL-3.0-only.txt` and
embedded in the JAR. VRO remains AGPL-3.0-or-later overall.

## Native chunk-update deferral

VRO's chunk-update deferral is a fresh integration with existing Minecraft
Forge, Sodium-derived Embeddium, and Rubidium behavior. It changes only the
effective preference read at validated scheduling sites. The projects retain
ownership of their queues, scheduling, cancellation, result handling and GPU
uploads; their code is neither copied nor bundled by this feature. Credits,
inspected versions and local source revision are in
`docs/CHUNK_UPDATE_DEFERRAL.md`. Newer Sodium scheduling is research-only.

## Compatibility behavior inspected

VRO interoperates with Minecraft Forge, SpongePowered Mixin, Vault Hunters,
Vault Integrations, Powah, Create Crafts & Additions, Xaero's World Map,
Embeddium/Rubidium render stacks, and optional optimization mods. Their code and
assets are not bundled. Locally installed Vault jars were inspected only to
identify stable public method and field layouts across supported pack versions.

VRO's Vault elixir-orb fix uses the installed particle class as a mixin target,
supplies a project-owned text buffer, and restores standard Minecraft particle
render state. Its Powah unload compatibility uses a Forge access transformer to
widen only the package-private `CableNet` class; no Powah field is made public
and no Powah source file is included.

The complete research ledger, including rejected and unimplemented projects,
is in `docs/PERFORMANCE_BACKPORT_RESEARCH.md`.
