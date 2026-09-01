# Source provenance audit

Audit date: August 31, 2026

This audit answers whether VRO's released behavior was copied, adapted,
independently implemented after research, or written from profiling and
compatibility work inside this project.

## Conclusion

VRO is not wholly clean-room code. Seven groups are explicit adaptations:

1. the client update-notification unit adapts the MIT Forge Update Notifier;
2. eleven render and graphics backports adapt ModernFix's LGPL implementation;
3. particle billboard geometry adapts Flerovium's LGPL implementation;
4. the Flywheel/Oculus bridge adapts Iris & Oculus Flywheel Compat's MIT
   implementation;
5. the learned-ability list cache adapts Unobtanium's AGPL implementation;
6. the client collision mixins adapt Entity Collision FPS Fix's CC0
   implementation;
7. simple-model face-list compaction and block-state `faceSturdy` array
   canonicalization adapt later FerriteCore MIT implementations to the
   Minecraft 1.18.2 data layouts not covered by FerriteCore 4.2.2.

The Unobtanium adaptation is why VRO is distributed under
AGPL-3.0-or-later. Attribution alone would not have been sufficient while the
project remained marked All Rights Reserved.

The particle light caches and empty render fast paths were independently
written after studying Particle Core and BadOptimizations; the billboard
geometry is the separately documented Flerovium adaptation. The world cleanup
was independently written after Unobtanium identified the affected static
maps. Section-distance culling and dynamic lighting were independently
implemented after studying Better Fps - Render Distance and Dynamic Lights
Reforged respectively. No third-party mod jar, decompiled class, or Vault
Hunters class is bundled in
VRO. The notifier is a documented package-relocated source adaptation from its
MIT integration repository.

## Feature-by-feature origin

| VRO feature | Relationship | Primary source or evidence |
| --- | --- | --- |
| Update manifest, menu row, and reminder system | Adapted source-copy unit | Forge Update Notifier `4f5cace`; canonical VHA source `483acee`; MIT |
| Vault armor durability cache | Original VRO implementation | Client profiles and Vault runtime APIs |
| Armor identified/damageable state | Original integration of newer behavior | Locally authorized Asgard Vault jar behavior and Vault `GearDataCache` API |
| Armor texture/model cache | Original VRO implementation | Vault runtime APIs and observed repeated render path |
| Identified Vault tool-model cache | Original VRO implementation | Vault runtime APIs and observed repeated render path |
| Learned ability-list cache | Adapted code | Unobtanium `7bf6a658`, contributor `radimous`, AGPL-3.0-or-later |
| Damage-number formatter reuse | Original VRO implementation | Client allocation profile and Vault's existing formatter |
| Elixir-orb number render-state isolation | Original VRO compatibility fix | In-game visual regression and installed Vault particle API |
| Ordinary particle billboard geometry | Adapted code | Flerovium `240f08c`, LGPL-3.0-only; VRO adds portable/packed writers and hot ownership |
| Shared same-tick particle-light cache | Independent design adaptation | Particle Core `1151fe6`, MIT; bounded VRO-owned primitive cache |
| Vault event-listener snapshot | Original VRO implementation | Initial VRO commit `bd9bd73`; invalidation refined in later VRO commits |
| Biome listener cache in HoYin Unobtanium fork | Downstream from VRO | Unobtanium fork commit `00d1837` postdates VRO's implementation |
| Client wall/push skips | Adapted code | Entity Collision FPS Fix `cc16e184`, CC0-1.0 |
| Create Addition/Powah unload cleanup | Independent reimplementation | Unobtanium retention discovery; VRO uses a different Forge unload/key-removal design |
| Shared empty-stack entity-reference guard | Independent reimplementation | Unobtanium `a72a669`; VRO redirects only the singleton assignment |
| Vault Loot Beams lazy tooltip/cache cleanup | Independent reimplementation | Unobtanium `f491eb4`; VRO preserves the mod's lazy query and adds Forge unload cleanup |
| iSpawner display-item selection/frustum culling | Independent reimplementation | Unobtanium `f491eb4`; VRO uses direct inventory passes, Compare Mode, and preserves original view distance |
| Particle-light cache | Independent design adaptation | Particle Core `1151fe6`, MIT |
| Empty renderer/tick exits | Independent design adaptation | BadOptimizations `5de4a3a`, MIT |
| Entity/block-entity renderer caches | Independent design adaptation | BadOptimizations `5de4a3a`, MIT |
| Simple-model face-list compaction | Adapted code | FerriteCore `b63de54a`, MIT; adapted to coexist with FerriteCore 4.2.2 |
| Block-state `faceSturdy` array interning | Adapted code | FerriteCore `18711423`, MIT; project-owned concurrent interner |
| Vertical/horizontal terrain-section culling | Independent design adaptation | Better Fps - Render Distance `6ada7eeb`, all rights reserved; no source or formula copied |
| Spatial dynamic-light engine | Independent design adaptation | Dynamic Lights Reforged `d85b337f`, MIT; independently written engine and lifecycle |
| Vault Integrations crash guard | Original VRO compatibility fix | Client crash trace and installed runtime behavior |
| Powah stale-cable crash guard | Original VRO compatibility fix | Client crash trace and installed runtime behavior |
| Powah unload access bridge | Original VRO compatibility fix | Exit crash trace; narrow Forge class access transformer for the independent unload cleanup |
| Vault/Xaero map key context | Original VRO compatibility fix | Reproduced Forge key-consumption conflict |
| Xaero stale cache-write crash guard | Original VRO compatibility fix | Client crash trace and installed Xaero cache lifecycle behavior |
| Compare Mode, configuration, coexistence plugin | Original VRO implementation | VRO benchmark and compatibility requirements |
| Four-pack compatibility build | Original VRO build tooling | Locally installed supported Vault API baselines |
| ModernFix render/graphics backports | Adapted code | Exact per-feature provenance in `docs/MODERNFIX_RENDER_BACKPORTS.md`, LGPL-3.0-or-later |
| Flywheel/Oculus shader bridge | Adapted code | Iris & Oculus Flywheel Compat `7a981f9`, MIT |

## Forge Update Notifier adaptation

VRO package-relocates the canonical nine-class Forge 1.18.2 notifier unit and
its seven contract test suites from Forge Update Notifier revision
`4f5cacebf9543c8f94a93fd070ae762fcd1d4e9c`. That integration repository
records VHA revision `483acee` as the canonical source. VRO retains the
asynchronous bounded request, fixed download-link validation, parsing and
filtering, coordinated menu placement, persistence, five/ten-launch cadence,
and once-per-JVM session state. VRO-owned code supplies the physical-client
initialization, config values, commands, metadata, raw GitHub manifest,
CurseForge URL, and documentation.

## Unobtanium comparison

### Direct adaptation

VRO's `ClientAbilityDataMixin` follows the same three-part lifecycle as
Unobtanium's `MixinClientAbilityData`:

- return a retained learned-ability list;
- store the computed list;
- clear it after `updateAbilities`.

VRO changes names, adds Compare Mode, and avoids replacing an already published
entry, but the implementation remains materially derived and is documented as
such.

### Independently implemented cleanup

Unobtanium replaces or cancels behavior around world-keyed Create Addition and
Powah state. VRO instead subscribes to Forge's world-unload event and removes
only the exact world key from each existing map. The target problem was learned
from Unobtanium, but the retained expression and lifecycle are VRO's own.

The empty-stack, Vault Loot Beams, and iSpawner changes follow the same
provenance category. VRO retained the observed safety/performance boundaries,
then implemented them directly against Minecraft 1.18.2 and the installed mod
APIs. It does not use Unobtanium's method bodies, helper layout, or distance
reduction. VRO also disables the overlapping mixins when Unobtanium is present.

### VRO code later appearing in a fork

VRO's event snapshot existed in commit `bd9bd73` on June 15, 2026. The
HoYin1600p Unobtanium fork added its biome-only snapshot in commit `00d1837` on
July 13, 2026. That code flow was from VRO into the fork, not from Unobtanium
into VRO.

## FerriteCore adaptations

FerriteCore 4.2.2 already owns the major Minecraft 1.18.2 block-state, shape,
property, quad, and model caches. VRO adapts only two later reductions absent
from that release: immutable compact simple-model face lists and canonical
`faceSturdy` boolean arrays. The retained VRO classes are shaped around Forge
1.18.2 and coexist with the installed 4.2.2 systems rather than replacing them.

## Independently implemented design-derived systems

Better Fps - Render Distance established the usefulness of separating terrain
draw distance from chunk loading. VRO copied neither its source nor its distance
formula; it uses project-owned camera-to-section bounds, separate vanilla and
Embeddium/Rubidium hooks, independent horizontal and vertical controls, and
different defaults.

Dynamic Lights Reforged established the expected behavior surface for held and
dropped item lights, resource definitions, water sensitivity, lightmap
combination, and terrain invalidation. VRO's engine was independently written
with spatial cells, per-source scheduling, deduplicated rebuilds, explicit
cleanup, shader policy, diagnostics, and a coexistence gate.

## Research-only inspection

The projects listed in `docs/PERFORMANCE_BACKPORT_RESEARCH.md` were inspected
to rank ideas, identify overlap, or reject unsafe approaches. A project in that
ledger is not a VRO source unless `CREDITS.md` and `THIRD_PARTY_NOTICES.md`
describe a shipped relationship.

## Release requirements

- Keep `LICENSE`, `CREDITS.md`, and `THIRD_PARTY_NOTICES.md` in the source
  release.
- Keep `LICENSE` and `THIRD_PARTY_NOTICES.md` embedded in the runnable jar.
- Keep the exact Flerovium, ModernFix, Forge Update Notifier, and Iris/Flywheel
  compatibility license texts embedded in the runnable jar.
- Publish complete corresponding source for every distributed VRO build under
  AGPL-3.0-or-later.
- Preserve exact source revisions and contributor names in future release
  notes.
- Audit and document every newly adapted or research-derived mechanism before
  release.
