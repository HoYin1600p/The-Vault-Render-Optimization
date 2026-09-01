# Embeddium-fork ownership transfer

This ledger records the renderer corrections transferred from HoYin1600p's
Embeddium stability fork into VRO. The source fork is read-only during this
work. Its implementations are not eligible for removal until the matching VRO
commit, renderer/version guards, automated tests, required in-game regressions,
and a control run after removal have all passed.

Validated renderer baselines:

- Embeddium `0.3.18+mc1.18.2`
- Rubidium `0.5.6`

Embeddium `0.3.18` publishes its own `embeddium` identity and a `rubidium`
compatibility alias from the same mod file. VRO treats that same-file pair as
one Embeddium implementation while continuing to fail closed when the two
renderer identities originate from different mod files.

Unknown versions fail closed. Every feature has an independent startup option
under `[embeddium_transfers]`; performance-sensitive paths yield in Compare
Mode while narrow correctness guards remain active. Startup diagnostics report
`APPLIED`, `YIELDED`, or `BLOCKED` with a reason.

| ID | Feature | VRO commit | Automated validation | Manual validation remaining | Source removable? |
|---|---|---|---|---|---|
| VRO-EMB-04 | Adjacent-position block occlusion | `1021ad6` | Unit ownership/config tests; stock renderer layouts; five-pack build | Framed/custom face hiding; Cake Vault temporary bedrock | No |
| VRO-EMB-05 | Null-buffer vertex sink | `782542d` | Guard unit test; stock renderer layouts; five-pack build | Flywheel/Create and loading transitions | No |
| VRO-EMB-10 | Direct CCL renderer lookup | `4f26ac5` | Embeddium-only ownership gate; CCL lambda layout; five-pack build | CCL blocks and fluids | No |
| VRO-EMB-02 | Bounded vertex-buffer retention | `332d75d` | Growth/overflow/retention unit tests; stock layouts; five-pack build | Long-session native memory | No |
| VRO-EMB-03 | Preemptive async arena growth | `7d88682` | Required/headroom/ceiling unit tests; stock layouts; five-pack build | Long-session VRAM and resize pressure | No |
| VRO-EMB-06 | Smooth non-luminous fluid lighting | `2f1f320` | Lighting policy and reload-cache tests; stock layouts; five-pack build | Modded fluids, reloads, dimensions | No |
| VRO-EMB-08 | Chunk-layer shader-color reset | `b0f9d8b` | Compare-mode ownership test; stock layouts; five-pack build | Animated text, shaders, DH, Create, Botania | No |
| VRO-EMB-01 | Chunk rebuild de-duplication | `06273e4` | Pending strength/task-state tests; separate renderer layouts; five-pack build | Rapid updates, fluids, block entities, mob-kill pressure, Vault Bedrock | No |

Source provenance is the repository ledger at
`VRO_OWNERSHIP_TRANSFER.md` in the Embeddium fork at commit `7b085088`,
plus the per-feature source commits recorded in the table and source headers.

## Automated validation snapshot

Completed on 2026-09-01 without installing into a Prism instance:

- `gradlew clean build`: 92 tests, 0 failures, 0 errors, 0 skipped.
- `scripts/verify-renderer-transfer-layouts.ps1`: PASS against stock Embeddium
  `0.3.18+mc1.18.2` and Rubidium `0.5.6`, including their distinct active-task
  fields and the Embeddium-only CodeChickenLib bridge.
- `scripts/build-pack-compatibility.ps1`: PASS for VaultCrafters Bootstrap,
  Asgard-SMP, Wolds Vaults 0.32.2, Wolds Vaults 0.33.0, and Vault Hunters
  Third Edition, followed by a clean universal build and all unit tests.
- Built artifact: `build/libs/vault_render_optimization.0.4.0.jar`, SHA-256
  `5274DDD2E7F219CFDD585B21C8BC118BD7036CE42A8909881D9DE3B73670F75B`.
- JAR inspection confirmed every renderer-transfer mixin/helper plus
  `META-INF/LICENSE`, `META-INF/THIRD_PARTY_NOTICES.md`, and the retained LGPL
  text. The JAR was not copied into tracked `libs`.

Automated implementation is complete, but none of the eight original fork
patches is eligible for removal yet. The table's manual regression cases and a
control run after each corresponding fork implementation is removed remain
mandatory.
