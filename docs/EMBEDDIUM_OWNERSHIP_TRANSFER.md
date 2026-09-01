# Embeddium-fork ownership transfer

This ledger records the renderer corrections transferred from HoYin1600p's
Embeddium stability fork into VRO. The source fork was read-only during the
transfer. The matching VRO commits, renderer/version guards, automated tests,
and accepted gameplay regressions are complete. The source implementations
were surgically removed in Embeddium commit `ced34c84`, and the resulting
single-owner control is recorded in Embeddium commit `565016a2`.

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

| ID | Feature | VRO commit | Automated validation | Validation remaining | Source status |
|---|---|---|---|---|---|
| VRO-EMB-04 | Adjacent-position block occlusion | `1021ad6` | Unit ownership/config tests; stock renderer layouts; five-pack build; live stock-Embeddium startup/rebuild pressure; operator-confirmed custom-block and Cake-room behavior | None | Removed in `ced34c84` |
| VRO-EMB-05 | Null-buffer vertex sink | `782542d` | Guard unit test; stock renderer layouts; five-pack build; live Create/Flywheel startup, reload, dimension, and flight smoke tests; inherited stable-fork gameplay | None | Removed in `ced34c84` |
| VRO-EMB-10 | Direct CCL renderer lookup | `4f26ac5` | Embeddium-only ownership gate; CCL lambda layout; five-pack build; CCL-present empty-registry pack scan; dormant-path risk accepted; post-removal guarded `BLOCKED` decision | None | Removed in `ced34c84` |
| VRO-EMB-02 | Bounded vertex-buffer retention | `332d75d` | Growth/overflow/retention unit tests; stock layouts; five-pack build; live rebuild/flight pressure; inherited gameplay sessions up to 12 hours; post-removal single ownership | None | Removed in `ced34c84` |
| VRO-EMB-03 | Preemptive async arena growth | `7d88682` | Required/headroom/ceiling unit tests; stock layouts; five-pack build; live causal isolation and fixed-increment regression; inherited gameplay sessions up to 12 hours; post-removal single ownership | None | Removed in `ced34c84` |
| VRO-EMB-06 | Smooth non-luminous fluid lighting | `2f1f320` | Lighting policy and reload-cache tests; stock layouts; five-pack build; resource reload and dimension cycle; operator-confirmed fluid behavior; post-removal single ownership | None | Removed in `ced34c84` |
| VRO-EMB-08 | Chunk-layer shader-color reset | `b0f9d8b` | Compare-mode ownership test; stock layouts; five-pack build; shader reload with Oculus, Create/Flywheel, and Botania loaded; more than one month of stable-fork DH use; post-removal DH initialization | None | Removed in `ced34c84` |
| VRO-EMB-01 | Chunk rebuild de-duplication | `06273e4` | Pending strength/task-state tests; separate renderer layouts; five-pack build; 49,152 block mutations, block entities, fluids, and mass mob-kill pressure; operator-confirmed Cake-room and stable-fork gameplay; post-removal single ownership | None | Removed in `ced34c84` |

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

Automated implementation, accepted gameplay validation, source removal, and
the post-removal single-owner control are complete for all eight transfers.

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
fullscreen mode prevented CMA's windowed-only resize action. The prior full
Create/Flywheel and stable-fork campaigns do not need to be repeated. The later
post-removal control below covers the custom Embeddium/Distant Horizons stack.

## Inherited long-session validation

The operator confirmed on 2026-09-01 that the source Embeddium fork has been
used routinely for long gameplay sessions, including sessions up to 12 hours,
without a native-memory or VRAM growth failure. VRO preserves the fork's
vertex-buffer retention behavior and fixed-increment asynchronous arena growth,
with additional capacity bounds, deterministic cleanup, unit coverage, and the
live pressure checks above. This real-world history is accepted as the required
long-session validation for VRO-EMB-02 and VRO-EMB-03; a separate synthetic
overnight soak is no longer required.

The operator also confirmed custom-block occlusion, fluid rendering, and Cake
room behavior from the stable fork. Those behaviors are accepted together with
VRO's source-equivalence, guarded-load, structural, and live startup checks.
The source implementations were subsequently removed and the required control
run verified the resulting single-owner stack.

## CodeChickenLib lookup scope

VRO-EMB-10 is not a particle optimization. It was added to the Embeddium fork
on 2026-06-15 in source commit `25e57646` and transferred to VRO in `4f26ac5`.
It affects only CodeChickenLib custom block and fluid renderers. The original
compatibility path scanned every registered block or fluid renderer entry for
each lookup; the transferred path retrieves the renderer directly from the
registry-delegate map, then preserves the same `canHandleBlock` check and bridge
render call. Global renderers retain their required ordered scan.

The local Wolds Vaults 0.33.0 pack contains CodeChickenLib, but a bytecode-
reference scan found no other installed JAR referencing `ICCBlockRenderer` or
`BlockRenderingRegistry`. The compatibility layer can therefore load while the
optimized registries remain empty. The operator accepts the negligible dormant-
path compatibility risk for this end-of-life 1.18.2 pack generation; a future
consumer incompatibility can be corrected if one appears. VRO-EMB-10 is complete
and no longer blocks source removal.

## Source-fork removal authorization

On 2026-09-01 the operator explicitly authorized removal of all eight
transferred implementations from the custom Embeddium fork:

- VRO-EMB-04: `BlockOcclusionCache.java`, source `0da4a463`.
- VRO-EMB-05: `MixinBufferBuilder.java`, source `7071ee1c`.
- VRO-EMB-10: `CCLCompat.java`, source `25e57646`.
- VRO-EMB-02: `VertexBufferBuilder.java`, source `7071ee1c`.
- VRO-EMB-03: `AsyncBufferArena.java`, source `0da4a463`.
- VRO-EMB-06: `FluidRenderer.java`, sources `7071ee1c` and `075e1886`.
- VRO-EMB-08: `SodiumWorldRenderer.java`, source `0da4a463`.
- VRO-EMB-01: `RenderSection.java` and `RenderSectionManager.java`, source
  `a8cebc3a`.

Removal was performed surgically because the source commits contain surrounding
fork work. Embeddium commit `ced34c84` removed only the eight transferred
implementations while preserving the fork's GL boundary reset, stable block-
entity diffuse lighting, empty loading-screen guard, and high-distance
spectator visibility.

## Post-removal single-owner control

Embeddium commit `565016a2b7af449d2d42724a7fe1f0b6dd95a75b` records the
completed control and tracked standard-name JAR update:

- Embeddium `gradlew clean build`: PASS with three pre-existing warnings.
- Embeddium runtime JAR SHA-256:
  `D71FA18869A6C3831B44635E5E467C6C4017867AFD0E947BB199CAC37EE9B072`.
- VRO `gradlew clean build`: PASS with known annotation-processor warnings.
- VRO runtime JAR SHA-256:
  `FFCF6632E20FFD16F4C673CCEC4F20FCE1DC944E24F0D2115F47553BA3B25E29`.
- The dedicated DH Test Client used Embeddium `ced34c84`, VRO `0.4.0`,
  Distant Horizons `3.2.1-b-dev`, and Oculus `1.6.5-dh-compat`.
- VRO-EMB-01, -02, -03, -04, -05, -06, and -08 each reported `APPLIED`
  exactly once. VRO-EMB-10 reported `BLOCKED` exactly once because CCL was
  absent, matching the accepted dormant-path policy.
- No duplicate ownership mixin, mixin application/injection failure,
  VRO/Embeddium/Oculus fatal error, or OpenGL error was present.
- Distant Horizons initialized a `CLIENT_SERVER` world and all configured DH
  levels before the old test world's login timed out during unrelated Forge
  registry synchronization. The captured thread was waiting in
  `HandshakeHandler.handleRegistryLoading` after local Vault registry mappings;
  this is a test-instance limitation after renderer/DH initialization, not a
  transfer failure.

No transferred renderer implementation remains in the Embeddium fork source.
All eight ownership transfers and the source cleanup are complete.
