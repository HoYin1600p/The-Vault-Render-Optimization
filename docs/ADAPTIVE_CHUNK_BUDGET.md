# Adaptive deferred chunk budgets: loading-safe experiment

## Status and scope

The first implementation passed offline tests but failed user terrain-loading
evaluation: cached full chunks appeared immediately when budgeting was disabled,
including with DH off. Bobby and Farsight were installed. A nearly empty
completed-results queue did not prove healthy throughput; the original display
omitted requests waiting to be submitted and worker activity.

Revision 2 is a default-on experiment on supported renderers. It aims to pace updates to terrain
that already has geometry without delaying initial terrain. This is not yet a
verified runtime fix or a demonstrated performance improvement.

Supported: exact inspected Embeddium 0.3.18 and HoYin1600p's 0.3.19 fork.
Eleven input classes are fingerprinted: nine shared index-sort contracts plus
the update-type enum and base task. Unknown bytecode, Rubidium, vanilla and
ambiguous renderers do not select these hooks. Input-JAR gates cannot guarantee
compatibility with later third-party mixin transformations.

## Controls

- `chunk_updates.adaptive_budget_v2=true` is the default; no opt-in is required.
- An existing explicit `adaptive_budget_v2=false` remains off. The retired
  `adaptive_budget` key is no longer read.
- `/vro chunks budget on|off|status` controls it without restarting.
- Effective deferred scheduling is required; VRO does not edit Embeddium's preference.
- Compare Mode, off, or effective synchronous scheduling restores native behavior.
- Off clears only controller/observation state. The native uploader retains all
  results and drains them normally; catch-up can produce a one-time hitch.
- Index-only sorting and native chunk-update deferral are independent features.

## Loading and backlog guard

1. INITIAL_BUILD admissions always retain the original native budget, and their
   tasks are not wrapped for budget timing.
2. Initial requests or completed results whose section is not built disable pacing.
   Zero-byte initial results count too: accepting them changes section state.
3. Startup and detected loading latch a conservative barrier until queued workers,
   active workers and ready results drain. Already-running jobs may have unknown
   origins, so this intentionally includes non-initial jobs.
4. Before upload, recheck for unbuilt sections. If found, leave the original
   native upload method untouched: the full queue retains its order, batching,
   acceptance checks and ownership. Initial results are not extracted or reordered.
5. Native fallback also applies at 128 pending requests, 128 completed results,
   the controller's payload watermark, 250ms of continuous pending-class activity,
   or 250ms of observed completed-result waiting.
6. When pressure clears, wait 500ms before resuming pacing. This prevents rapid
   alternation between modes. Native fallback may itself reintroduce hitches.

A continuously busy loading session may stay entirely in native fallback and
yield no performance benefit. The guard prefers native throughput over delayed
visible terrain. The 250ms criterion is a fallback trigger, not a promised
maximum visible-update delay. Requests are raw native queue entries, including
entries the renderer may later discard; class-busy time is not an individual
request's age. A worker result appended after the upload recheck may be noticed
on the next update.

There is no Bobby, Farsight or DH cache modification, invalidation or deletion.
The loading distinction follows the renderer's built/unbuilt section state,
not a mod-name assumption. Actual cache-mod integration still needs testing.

## Pacing when safe

Each renderer learns local successful worker costs and CPU upload costs; nothing
is calibrated to the development PC or persisted. The predicted upload target
starts at 0.75ms, bounded to 0.25-1.5ms and 10% of update interval subject to the
floor. Overrun pressure reduces it by 20%; eight qualifying cheap uploads allow
2% recovery. Pauses over 250ms do not train a higher allowance.

Measured cost per byte determines a 4KiB-8MiB FIFO batch allowance. At least one
indivisible oversized result progresses; consumption is bounded by 128 results
and the observed starting queue size. Native region upload still owns cleanup.
Build/sort estimates remain separate, with native queue capacity, worker capacity,
estimated output space, aged-class admission opportunities and at most 32 paced
admissions per cycle. Initial terrain never enters this admission cap.

This is a predictive soft budget, not a timer interrupt or GPU measurement.
Native allocation, staging, driver stalls and individual large sections may
exceed it. The payload watermark is heap/128 clamped to 16-64MiB, not a cap on
process native memory or VRAM. Observer metadata is capped at 4096 results;
inspection saturation forces native fallback. No queue payload is owned or
freed by the observer. Destroy/off retain native cancellation and cleanup.

## Diagnostics

Status reports `PACING` or `NATIVE FALLBACK` with a reason, the five pending
request classes, queued and active workers, completed results, longest
continuous pending-class busy time, and paced/native cycle counts.

The existing controller snapshot reports predicted allowance, observed native
payload, completed-result age/peak, paced admissions/uploads, upload CPU time,
overshoots and recent update-interval p95/p99 plus upload CPU p95. Upload timing
and counters cover paced interceptions, not the native fallback batches.
Already-built worker estimates can learn during fallback; initial jobs are
excluded. A mode change therefore changes the sample population.

Text refresh is at most twice per second; percentile rings hold 256 samples.
Result age starts on first observation, not necessarily worker completion.
Update intervals include game/driver/VSync work; they are not GPU-present times.
Neither a healthy-looking queue nor a PACING label proves an FPS improvement.

## Verification and runtime acceptance

Revision 2 offline validation: 165 tests passed against both stock Embeddium
0.3.18 and the inspected custom 0.3.19 JAR. All five pack compatibility compiles
and the final clean production build passed (six existing annotation-processor
warnings). JAR inspection verified the new guard/adapters, current embedded
third-party notices and upstream license, with no bundled renderer or test code.

Offline regression tests cover initial request bursts with no completed results,
startup/in-flight loading, zero-byte unbuilt results, late ready initial results,
native callback/queue preservation, backlog/wait fallback, recovery cooldown,
and continued eligibility of stable already-built updates. Existing tests cover
controller feedback, queue ownership, cancellation, bytecode gates and injection
contracts. Tests use real renderer contracts with mocked world/GL boundaries;
they do not apply the actual Forge mixins or validate a graphics driver.

Before claiming runtime success, install only with authorization and check:

1. With budgeting off, revisit a cached route and record terrain appearance.
2. Enable it and repeat with Bobby/Farsight unchanged, first with DH off, then on.
   Loading should report native fallback and terrain must not wait for toggling off.
3. Let loading settle. PACING should become eligible during ordinary updates;
   persistent fallback is safe but may mean the optimization offers no benefit.
4. Compare same-route frame tails and visual delay, not peak FPS alone. Keep
   shaders, deferral and index sorting constant between paired captures.
5. Place/break rapidly, run fluids and cake-room/temporary Vault Bedrock changes,
   teleport, change dimensions, reload shaders, unload, and toggle off/on with work.
6. Watch stale geometry, native memory and driver stalls; test slower hardware
   when available. Mocks and this workstation cannot establish universal benefit.

No installation, public push, version bump or publication is included in this revision.
