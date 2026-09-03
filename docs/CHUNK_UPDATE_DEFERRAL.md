# Native chunk-update deferral

## Purpose and evidence

A two-minute client profile showed 8.5% of sampled render-thread time waiting
for Embeddium's important update futures. That wait queue can contain full
rebuilds and transparency sorts; the profile does not split their wait time.
The user's informal same-base comparison improved from roughly 70 FPS with
noticeable hitches to 100-110 FPS without those hitches when native deferral
was enabled. Broken blocks appeared very slightly later. These are user
observations, not a controlled benchmark or a measurement of this VRO build.

VRO therefore selects an existing, tested-by-upstream scheduling option rather
than introducing a new scheduler or a timeout around `CompletableFuture.join`.
This is independent of the existing VRO-EMB-01 rebuild de-duplication feature.

## Implementation boundaries

- Default: `chunk_updates.defer_updates=true`.
- Vanilla Forge 40.3.11 / Minecraft 1.18.2: redirect only the two priority-option
  reads in `LevelRenderer.compileChunks` to `PrioritizeChunkUpdates.NONE` while
  enabled. The original async compile list, light-ready check, dirty clearing,
  dispatcher, uploads, and unload handling remain unchanged.
- Embeddium `0.3.18+mc1.18.2` and validated fork `0.3.19+mc1.18.2`: redirect
  the manager's deferral-field read in `submitRebuildTasks`. Important rebuilds
  and sorts take the existing `scheduleDeferred` branch; task priorities and
  the native scheduling budget are otherwise unchanged.
- Rubidium `0.5.6`: its native deferral-field read is in `scheduleRebuild`, not
  `submitRebuildTasks`. Select its existing regular/deferred update behavior
  there. Rubidium's native priority policy is preserved, including its different
  treatment of deferred updates. Already-pending important jobs can still finish
  synchronously during a toggle; they are not dropped or rewritten.
- Exactly one backend is selected. Non-client, failed discovery, ambiguous
  renderers, unknown versions, Sodium-id implementations and detected OptiFine
  are blocked rather than falling back to the wrong scheduler.
- Only VRO's setting is saved. Native preferences and native manager state are
  never written by these mixins. Off/Compare Mode yields to native preferences.
- No additional executor, future queue, mesh storage, or per-frame log is added.
  A small last-observed preference snapshot supports `/vro chunks status`.
- Rendering correctness still relies on each renderer's native cancellation,
  result ordering, dirty updates, uploads, and resource release. This does not
  guarantee a hard frame-time budget or immediate visual updates under load.

## Commands

```text
/vro chunks status
/vro chunks defer on
/vro chunks defer off
```

Changes apply to new scheduling decisions; no restart or renderer reload is
required for the toggle. Existing queued jobs are not cancelled by the toggle.
Changing installed renderer mods always requires a restart.

## Automated verification

- `ChunkUpdatePolicyTest`: default request, native-setting preservation, Compare
  Mode, and repeatable stateless toggling.
- `ChunkUpdateBackendTest`: vanilla/no renderer, stock/fork Embeddium, Rubidium,
  ambiguous installations, unknown versions, unsupported renderer, discovery
  failure, and dedicated-server rejection.
- `scripts/verify-chunk-update-layouts.ps1`: inspect actual Forge/renderer JAR
  bytecode with `javap`, assert exact option-read counts and original sync/async
  branches. Inputs are `-ForgeMappedJar` and `-RendererJars` (an array).
- Run the existing five-pack compatibility build and complete unit suite.

Local validation on 2026-09-03: all five pack compilation targets and the final
clean build passed; all 104 unit tests passed (including seven new policy and
backend tests). The layout script passed against all four listed Forge/renderer
inputs. Vanilla production refmap entries were generated for both the compile
method and priority field. Only existing annotation-processor warnings remain.

Compilation and layout checks are not proof of successful runtime mixin
application. No new build should be called in-game validated until the checks
below have been completed on that build.

## Pending in-game checks

1. Vanilla, stock Embeddium 0.3.18, validated custom Embeddium 0.3.19, Rubidium
   0.5.6: startup, join, `/vro chunks status`, no injection errors.
2. Native deferral **off**, VRO on/off/on in the same scene: compare frame-time
   distribution and the render-thread wait subtree. Keep other conditions fixed.
3. Native deferral **on**, VRO off/Compare Mode on: confirm native deferral stays
   active. Toggle repeatedly with queued work; check that updates finish.
4. Rapid placement/removal, fluids, custom block models, nearby spawners,
   transparency while moving, and cake-room temporary Vault Bedrock removal:
   measure visual latency and check for permanently stale geometry.
5. Chunk unload/reload, dimensions, resource reload, shader toggles, Create,
   DH, and prolonged update pressure: no retained job growth or missing updates.

## Provenance

Fresh VRO policy, configuration, commands and narrow field-read mixins. No
Minecraft, Forge, Sodium, Embeddium, or Rubidium scheduling implementation is
copied or bundled. Native scheduling behavior was inspected in Forge 40.3.11,
stock Embeddium 0.3.18, Rubidium 0.5.6, and HoYin1600p's Embeddium fork at
`d95f90d1edb990943b30663b2e95a02ea5e7c2a8`.

Sodium and Embeddium contributors are credited for the native deferral behavior
this feature invokes. Newer Sodium scheduling was research-only, not backported.
See `CREDITS.md` and `THIRD_PARTY_NOTICES.md`.
