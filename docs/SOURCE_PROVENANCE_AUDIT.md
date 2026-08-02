# Source provenance audit

Audit date: August 2, 2026

This audit answers whether VRO's released behavior was copied, adapted,
independently implemented after research, or written from profiling and
compatibility work inside this project.

## Conclusion

VRO is not wholly clean-room code. Two small groups are explicit adaptations:

1. the learned-ability list cache adapts Unobtanium's AGPL implementation;
2. the client collision mixins adapt Entity Collision FPS Fix's CC0
   implementation.

The Unobtanium adaptation is why VRO 0.3.0 is distributed under
AGPL-3.0-or-later. Attribution alone would not have been sufficient while the
project remained marked All Rights Reserved.

The generic particle and render fast paths were independently written after
studying Particle Core and BadOptimizations. The world cleanup was independently
written after Unobtanium identified the affected static maps. No complete
third-party source file, mod jar, decompiled class, or Vault Hunters class is
bundled in VRO.

## Feature-by-feature origin

| VRO feature | Relationship | Primary source or evidence |
| --- | --- | --- |
| Vault armor durability cache | Original VRO implementation | Client profiles and Vault runtime APIs |
| Armor identified/damageable state | Original integration of newer behavior | Locally authorized Asgard Vault jar behavior and Vault `GearDataCache` API |
| Armor texture/model cache | Original VRO implementation | Vault runtime APIs and observed repeated render path |
| Identified Vault tool-model cache | Original VRO implementation | Vault runtime APIs and observed repeated render path |
| Learned ability-list cache | Adapted code | Unobtanium `7bf6a658`, contributor `radimous`, AGPL-3.0-or-later |
| Damage-number formatter reuse | Original VRO implementation | Client allocation profile and Vault's existing formatter |
| Vault event-listener snapshot | Original VRO implementation | Initial VRO commit `bd9bd73`; invalidation refined in later VRO commits |
| Biome listener cache in HoYin Unobtanium fork | Downstream from VRO | Unobtanium fork commit `00d1837` postdates VRO's implementation |
| Client wall/push skips | Adapted code | Entity Collision FPS Fix `cc16e184`, CC0-1.0 |
| Create Addition/Powah unload cleanup | Independent reimplementation | Unobtanium retention discovery; VRO uses a different Forge unload/key-removal design |
| Particle-light cache | Independent design adaptation | Particle Core `1151fe6`, MIT |
| Empty renderer/tick exits | Independent design adaptation | BadOptimizations `5de4a3a`, MIT |
| Entity/block-entity renderer caches | Independent design adaptation | BadOptimizations `5de4a3a`, MIT |
| Vault Integrations crash guard | Original VRO compatibility fix | Client crash trace and installed runtime behavior |
| Powah stale-cable crash guard | Original VRO compatibility fix | Client crash trace and installed runtime behavior |
| Vault/Xaero map key context | Original VRO compatibility fix | Reproduced Forge key-consumption conflict |
| Compare Mode, configuration, coexistence plugin | Original VRO implementation | VRO benchmark and compatibility requirements |
| Four-pack compatibility build | Original VRO build tooling | Locally installed supported Vault API baselines |

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

### VRO code later appearing in a fork

VRO's event snapshot existed in commit `bd9bd73` on June 15, 2026. The
HoYin1600p Unobtanium fork added its biome-only snapshot in commit `00d1837` on
July 13, 2026. That code flow was from VRO into the fork, not from Unobtanium
into VRO.

## Research-only inspection

The projects listed in `docs/PERFORMANCE_BACKPORT_RESEARCH.md` were inspected
to rank ideas, identify overlap, or reject unsafe approaches. A project in that
ledger is not a VRO source unless `CREDITS.md` and `THIRD_PARTY_NOTICES.md`
describe a shipped relationship.

## Release requirements

- Keep `LICENSE`, `CREDITS.md`, and `THIRD_PARTY_NOTICES.md` in the source
  release.
- Keep `LICENSE` and `THIRD_PARTY_NOTICES.md` embedded in the runnable jar.
- Publish complete corresponding source for every distributed VRO build under
  AGPL-3.0-or-later.
- Preserve exact source revisions and contributor names in future release
  notes.
- Audit and document every newly adapted or research-derived mechanism before
  release.
