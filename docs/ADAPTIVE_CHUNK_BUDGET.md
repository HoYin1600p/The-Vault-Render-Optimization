# Adaptive deferred chunk budgets

## Scope

Task 2 implements a VRO-owned feedback controller, not a benchmark-derived
allowance calibrated on one PC. Each renderer instance starts conservatively,
learns from its own successful worker jobs and CPU-side upload durations, and
adjusts during play. No learned state is shared across machines or saved to disk.

Supported: exact inspected Embeddium 0.3.18 and HoYin1600p's 0.3.19 fork.
Eleven input classes are fingerprinted: the nine shared index-sorting contracts
plus the update-type enum and base task. Unknown bytecode, Rubidium, vanilla and
ambiguous renderers do not select the hooks. These are input-JAR gates, not a
guarantee against later third-party mixin changes. Index-only sorting has an
independent toggle; the budget supports both ordinary and index-only results.

## Controls and fallback

- `chunk_updates.adaptive_budget=true` is the default in VRO's client TOML.
- `/vro chunks budget on|off|status` applies at the next renderer update.
- Compare Mode, off, or effective synchronous scheduling restores native behavior.
- Effective deferral may come from VRO's deferral option or the renderer's own
  preference. This feature does not edit that preference or force deferral.
- Off closes the controller, clears only observation metadata, and lets the
  original uploader drain its own remaining results. A large backlog may cause
  a one-time catch-up hitch. On starts a fresh conservative controller.

No worker count, visibility, render distance, model detail, block invalidation,
native task/future semantics, or native full-rebuild upload algorithm is changed.
Task 3 remains deferred. World destruction keeps native cancellation and native
buffer cleanup; no queue payload is owned or freed by the observer/controller.

## Feedback and limits

Initial estimated upload allowance is 0.75ms. It is bounded to 0.25-1.5ms and
at most 10% of the observed update interval, subject to that 0.25ms floor.
Intervals slower than approximately 60fps never raise the 1.5ms ceiling.
An upload overrun, or worsening intervals accompanied by material upload work,
reduces the allowance by 20%. Eight qualifying cheap-upload cycles permit 2%
recovery. Pauses over 250ms do not train higher allowances.

Measured nanoseconds per native payload byte translate the time target into a
batch byte allowance (4KiB-8MiB). Cost increases are learned quickly; decreases
slowly. A batch is a FIFO prefix of the existing completed-results queue and
still uses native region batching. A single indivisible oversized result always
progresses, and zero-byte results are limited by a 128-result maximum. Work
finishing continuously cannot extend the batch without bound.

**This is a predictive soft budget, not a hard timer or GPU time measurement.**
An upload cannot safely be interrupted midway. Native staging/allocation,
driver stalls, and one large section can exceed the target. Actual CPU-side
upload duration feeds the following cycles, including allocation overhead.
Submitted-byte measurements can include stale results the native uploader drops,
so they are conservative workload counters, not exact GPU traffic measurements.

Build and sort durations/output sizes have separate estimates. New admissions
respect native queue capacity, worker-duration estimates, estimated upload cost,
and reserved output space for queued and active workers. The native important
queues no longer receive unlimited submissions while this controller is active.
All priority classes share one cycle allowance (at most 32 new jobs), including
the native sorting minimum. No native pending entry is consumed just to defer it.

Completed-result backpressure starts at 128 results or a native payload watermark
of heap/128, clamped to 16-64MiB. Existing active jobs reserve estimated output
space. The observer tracks at most 4096 result identities; saturation reports
overload and stops new admissions. Bytes exclude cached heap snapshots, builder
scratch buffers, staging buffers and VRAM. **This is not a hard process/native
memory cap:** estimates, existing jobs, external producers and indivisible large
results can exceed it. The renderer continues draining instead of dropping data.

An update class waiting 250ms receives the next admission opportunity before
earlier classes can spend its allowance. This prevents cheap sorts from starving
aged rebuilds; it does not guarantee an absolute visual-update deadline. Within
each class, native ordering is unchanged. Completed results remain FIFO, with
at least one result progressing per update when the queue is nonempty. A machine
that cannot keep up must trade some visual update delay for frame consistency.

## Diagnostics

The status command reports the current allowance, observed queued native bytes,
result count, oldest observed result wait and peak, admissions, consumed results,
aggregate upload CPU time, budget overruns, recent update-interval p95/p99 and
upload CPU p95. Text snapshots refresh at most twice per second without logging
each frame. Percentiles use fixed 256-sample rings. The controller retains no
global world/renderer references; only a text snapshot is globally visible.

Queue ages begin when a result is first observed, not necessarily when the
worker completed. Update intervals include other game/driver/VSync work and are
not GPU-present frame times. Neither these statistics nor an APPLIED label proves
a performance improvement. APPLIED indicates the active deferred budget path.

## Verification and remaining runtime work

Completed offline validation: 154 tests passed against both stock Embeddium
0.3.18 and the inspected custom 0.3.19 JAR, including a 2000-cycle simulated
transition from cheap to expensive worker/upload load. All five pack compatibility
compiles and the final clean production build passed (six existing annotation
processor warnings). Production JAR inspection confirmed the new helpers/mixins
and upstream license, with no renderer classes or test dependencies bundled.

Automated tests exercise fast/slow upload learning, low-FPS feedback, gradual
recovery, pause handling, memory-pressure admission stops, in-flight reservations,
aged-class fairness, FIFO and oversized-result progress, bounded batch consumption,
off/unload ownership, native/index-only payload accounting, cancellation and
failure pass-through, bytecode gates and exact injection contracts.

The standalone tests use the real renderer classes where possible, with mocked
world/GL boundaries and the existing test-only render-pass enum. They do not
validate actual Forge mixin transformation or graphics-driver behavior.

Before claiming runtime readiness, explicitly authorize installation, then check:

1. Startup gate/mixin success, status APPLIED, increasing admissions/uploads.
2. Same-route on/off captures with deferral and sorting unchanged; compare frame
   tails, queued bytes/wait and visible update delay, not maximum FPS alone.
3. Sustained place/break, fluids, cake-room/temporary Vault Bedrock removal,
   teleport/dimension changes, world unload and hot off/on with work queued.
4. Shader changes/reloads, DH and Create where installed; inspect native-memory
   behavior and stale geometry. No shader/DH compatibility claim from mocks.
5. Repeat on lower-end hardware when available. Simulated slow costs test the
   controller but cannot substitute for actual driver/memory/CPU combinations.

No installation, renderer-fork edit, public push, version bump or publication
is part of this implementation.
