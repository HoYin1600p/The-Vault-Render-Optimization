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
| VRO-EMB-04 | Adjacent-position block occlusion | `1021ad6` | Unit ownership/config tests; stock renderer layouts; five-pack build; live stock-Embeddium startup/rebuild pressure | Framed/custom face hiding; Cake Vault temporary bedrock | No |
| VRO-EMB-05 | Null-buffer vertex sink | `782542d` | Guard unit test; stock renderer layouts; five-pack build; live Create/Flywheel startup, reload, dimension, and flight smoke tests | Narrow moving-contraption visual smoke test | No |
| VRO-EMB-10 | Direct CCL renderer lookup | `4f26ac5` | Embeddium-only ownership gate; CCL lambda layout; five-pack build | CCL blocks and fluids | No |
| VRO-EMB-02 | Bounded vertex-buffer retention | `332d75d` | Growth/overflow/retention unit tests; stock layouts; five-pack build; live rebuild/flight pressure | Long-session native-memory soak | No |
| VRO-EMB-03 | Preemptive async arena growth | `7d88682` | Required/headroom/ceiling unit tests; stock layouts; five-pack build; live causal isolation and fixed-increment regression | Long-session VRAM soak and windowed resize pressure | No |
| VRO-EMB-06 | Smooth non-luminous fluid lighting | `2f1f320` | Lighting policy and reload-cache tests; stock layouts; five-pack build; resource reload and dimension cycle | Human comparison of modded luminous/non-luminous fluids | No |
| VRO-EMB-08 | Chunk-layer shader-color reset | `b0f9d8b` | Compare-mode ownership test; stock layouts; five-pack build; shader reload with Oculus, Create/Flywheel, and Botania loaded | Animated rainbow/gradient text; shader-enabled visual smoke; DH on another instance | No |
| VRO-EMB-01 | Chunk rebuild de-duplication | `06273e4` | Pending strength/task-state tests; separate renderer layouts; five-pack build; 49,152 block mutations, block entities, fluids, and mass mob-kill pressure | Human stale-geometry check; Cake Vault temporary bedrock | No |

Source provenance is the repository ledger at
`VRO_OWNERSHIP_TRANSFER.md` in the Embeddium fork at commit `7b085088`,
plus the per-feature source commits recorded in the table and source headers.

## Automated validation snapshot

Completed on 2026-09-01 without installing into a Prism instance:

- `gradlew clean build`: 97 tests, 0 failures, 0 errors, 0 skipped.
- `scripts/verify-renderer-transfer-layouts.ps1`: PASS against stock Embeddium
  `0.3.18+mc1.18.2` and Rubidium `0.5.6`, including their distinct active-task
  fields and the Embeddium-only CodeChickenLib bridge.
- `scripts/build-pack-compatibility.ps1`: PASS for VaultCrafters Bootstrap,
  Asgard-SMP, Wolds Vaults 0.32.2, Wolds Vaults 0.33.0, and Vault Hunters
  Third Edition, followed by a clean universal build and all unit tests.
- Built artifact: `build/libs/vault_render_optimization.0.4.0.jar`, SHA-256
  `33D4BDA755BDB143DA258BA572A71FCCE585B05470DE3F0BBE6FBC18E050F2E2`.
- JAR inspection confirmed every renderer-transfer mixin/helper plus
  `META-INF/LICENSE`, `META-INF/THIRD_PARTY_NOTICES.md`, and the retained LGPL
  text. The JAR was not copied into tracked `libs`.

Automated implementation is complete, but none of the eight original fork
patches is eligible for removal yet. The table's manual regression cases and a
control run after each corresponding fork implementation is removed remain
mandatory.

## CMA Asgard live validation

Completed on 2026-09-01 using the production JAR above in the `CMA Asgard`
instance at 3840x2160, render distance 32, stock Embeddium
`0.3.18+mc1.18.2`, Rubidium compatibility alias `0.5.6`, Forge `40.3.11`, and
Minecraft `1.18.2`:

- Corrected the stock-Embeddium renderer-family detector (`d974846`) and the
  VRO-EMB-04 injection signature (`356706f`). Live startup then exposed two
  more invalid extended `@ModifyArg` signatures in VRO-EMB-06 and VRO-EMB-03.
  Commit `5fc8ee0` replaced those with one-argument modifiers plus HEAD-state
  capture and added structural regression tests.
- A controlled option toggle isolated a repeatable hard process exit to
  VRO-EMB-03. The port was compounding speculative arena growth from current
  capacity on every resize. It now derives one capped increment from the
  arena's initial capacity, so repeated growth is linear while still fitting
  the requested upload. The same build remained playable past the former exit
  window with VRO-EMB-03 enabled.
- Ownership diagnostics reported VRO-EMB-04, -05, -02, -03, -06, -08, and -01
  `APPLIED`. VRO-EMB-10 correctly reported `BLOCKED` because CodeChickenLib was
  absent.
- Automated rebuild pressure completed 12 stone/glass/air cycles over 4,096
  blocks per phase, five 256-chest create/remove cycles, six water/lava/air
  cycles, and a 48-zombie mass-kill cycle. The disposable region was restored
  to air afterward.
- Resource reload, Oculus shader reload with shaders disabled, Nether-to-
  Overworld dimension cycling, and post-action frame stabilization all passed.
  Create `0.5.1.i`, Flywheel `0.6.11-107`, and Botania `1.18.2-435` were loaded
  throughout. No VRO/Flywheel/Create renderer exception was observed; unrelated
  pack recipe/model warnings remain outside this transfer.
- A three-trial 60-block/second native flight smoke test completed all routes
  and returned to the same point. Across 2,051 frames it measured 119.06 average
  FPS, 7.76 ms median, 13.10 ms p95, 19.33 ms p99, and no frame over 50 ms.
  These are short 115-tick utility routes, not the planned long-duration
  comparative campaign.
- A separate Spark-instrumented diagnostic route completed and saved its local
  report outside the repository. Its measured route had no frame over 50 ms;
  profiler shutdown/upload caused a postflight sampling stall, after which the
  client recovered and stabilized normally.
- The final targeted log scan found no mixin application/injection failure,
  fatal renderer error, OpenGL error, render-thread violation, concurrent
  modification, failed buffer upload, or out-of-memory error.

CodeChickenLib and Distant Horizons were not installed in this instance, and
fullscreen mode prevented CMA's windowed-only resize action. Those paths remain
for a compatible test instance. The prior full Create/Flywheel campaign does
not need to be repeated; only the narrow moving-contraption visual check listed
in the table is required for these new paths.
