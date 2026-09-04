# Index-only terrain transparency sorting

## Scope and ownership

VRO owns creation and upload of index-only sort jobs on inspected Embeddium
`0.3.18+mc1.18.2` and HoYin1600p's `0.3.19+mc1.18.2` fork. Both new mixins
are selected together, independently of the older renderer transfers and the
native deferral feature. Exact hashes of nine relevant renderer classes are
checked before selection. Missing/changed bytecode, vanilla, Rubidium 0.5.6,
ambiguous renderers and failed discovery block this feature, not client startup.
The hashes describe the input JAR, not changes made later by third-party mixins;
arbitrary renderer-overwriting mods still need compatibility testing.

The renderer's translucent-sorting preference remains authoritative. VRO does
not enable sorting when the renderer would not schedule it. Unknown/direct
snapshot storage falls back to the renderer's existing task creation path.

## Work removed

The old sort-only path duplicates cached heap vertices, copies them to a native
result buffer, then replaces/uploads GPU vertices along with indices. VRO keeps
the exact same native sorting algorithm but instead:

1. Captures the renderer's immutable heap snapshot as a geometry-generation token.
2. Shares vertex bytes through private read-only views; copies only indices.
3. Produces native index-only results through the existing task/future queues.
4. Uploads indices in batches, retaining the existing vertex segment and format.
5. Installs new index ownership before freeing the old index segment.

For a successful single-pass sort with V bytes of vertices, this avoids two
V-byte CPU copies and a V-byte GPU upload from the old path. Index copying,
sorting scratch arrays and index uploads remain. This is a work/byte accounting
statement, not a measured FPS or frame-time claim. Buffer allocation/upload is
still on the render thread. No new scheduler, worker, queue, upload budget,
distance cutoff, format conversion or reduced visual detail is introduced.

## Correctness boundaries

- Before uploading, verify native result acceptance, live region/resources,
  matching cached-geometry object identity, and matching segment lengths.
- A full rebuild clears/replaces the geometry token; stale sorting results are
  discarded and freed without advancing the accepted-build timestamp.
- Keep native full-rebuild batch order. Batch only uninterrupted sort runs;
  choose the newest completed sort for each section and track frame order
  across intervening native batches. Do not coalesce across a geometry rebuild.
- Reuse vertices without freeing them; new index allocations are rolled back
  if installation fails. Index arena growth invalidates cached tessellations.
- Source native payloads are freed after copying into staging. Result cleanup
  is idempotent because the native outer lifecycle also deletes results.
- Cancellation before/after sorting allocates no output or frees partial output.
  Queued job/result lifetime remains governed by the renderer, not VRO caches.
- No world, camera, GPU arena or executor is held in a static cache. Per-job
  heap snapshots are released with the job; counters contain numbers only.

There is temporarily both an old and replacement index segment during commit,
until the old one is freed. This avoids destroying a working state before an
upload succeeds; it does not retain old vertex or index peaks across jobs.

## Controls

- Default: `chunk_updates.index_only_sorting=true` in the normal VRO client TOML.
- `/vro chunks sorting on|off`: persisted runtime toggle for new jobs.
- `/vro chunks sorting status`: selection/config status, scheduled/applied,
  stale/fallback counts, avoided vertex copy bytes and avoided vertex upload bytes.
- Compare Mode stops creating optimized jobs. Hooks remain installed so existing
  VRO jobs finish correctly, including after off/on or Compare Mode toggles.

Counters are cumulative for the client process and measure work performed or
avoided, not elapsed-time savings. APPLIED means the supported path is enabled;
the applied-job counter shows whether actual sorts have passed through it.

## Automated verification

- `SortBufferViewsTest`: byte-order/cursor isolation, read-only vertex views,
  independent indices and generation checks; 240 byte-for-byte native-sort
  comparisons covering compact/float formats, geometry samples and camera points.
- `IndexSegmentCommitTest`: ownership transfer, upload failure and installation
  rollback without freeing retained vertices or existing indices.
- `IndexOnlyUploadsTest`: actual adapter/state/result code with simulated world,
  GL and render-layer boundaries. Tests index-only uploads, latest-result
  selection, rebuild/unload rejection, mixed batch ordering, arena growth,
  failure cleanup, cancellation and index-only native allocation.
- `IndexSortCompatibilityTest`: actual input-JAR hashes plus missing, changed,
  unreadable and unsupported backend rejection.
- `IndexSortStructureTest`: native mixin target/callback/metadata contracts and
  separation of vertex/index paths in compiled VRO code.
- Run `gradlew test` against stock Embeddium, and again with
  `-Pindex_sort_test_jar=<validated-fork.jar>` for the fork. Test dependencies and
  the test-only render-layer token stub are never packaged with VRO.
- Run `scripts/build-pack-compatibility.ps1` and inspect the final production JAR.

The standalone JVM cannot bootstrap the full transformed Forge rendering stack.
Tests use its real sorter and graphics-state classes, but a test-only five-token
render-pass enum and mocked graphics device. These checks do not prove runtime
mixin application, shader/DH compatibility, or actual driver performance.

## Pending in-game validation

Completed offline validation for this implementation: all 129 tests passed
against both stock 0.3.18 and the inspected custom 0.3.19 renderer, including
the 240 sorter comparisons and 100 repeated index-replacement/cleanup cycles.
The five-pack compatibility matrix and final clean production build passed.
JAR inspection confirmed the new adapters, mixins and complete upstream license
are present, with no bundled renderer classes, test stub or test dependencies.
These results do not replace the in-game checks below.

After an explicitly authorized installation, verify the startup bytecode-gate
message and that `/vro chunks sorting status` reports increasing applied jobs.
Then test movement around glass/water/tripwire, rapid placement/removal through
transparent surfaces, shader off/on and reload, dimension/world changes,
Create/DH if present, and hot off/on with work queued. Watch for flicker, stale
geometry, OpenGL/mixin failures, buffer errors and growing native memory.
Compare the same scene with this toggle on/off; keep native deferral unchanged.
Profile before claiming FPS gains. Do not remove renderer source code: this
feature replaces its behavior at runtime and the original remains the fallback.

## Provenance and future work

Behavioral source paths and the inspected fork revision are in
`THIRD_PARTY_NOTICES.md`; three adapter classes carry LGPL-3.0-only notices.
Newer Sodium's separate index-upload design is inspiration, not copied code.
VRO remains AGPL-3.0-or-later overall. Full upstream license texts are embedded.

Adaptive upload/build budgeting and background visibility work are deliberately
deferred in [the backlog](RENDER_OPTIMIZATION_BACKLOG.md). Re-evaluate after
several days using this change; neither is part of this implementation.
