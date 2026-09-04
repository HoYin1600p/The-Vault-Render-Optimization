# Create/Flywheel startup format correction

## Evidence and scope

The reported login crash rejected an 11-element extended block format in
Flywheel `BlockModel`, called by Create `FlwContraption.buildLayers` during a
client tick. In the original session, Oculus created a pipeline at 22:07:37,
DH requested a reload at 22:07:41, and the pipeline was destroyed. No replacement
pipeline was logged before the 22:08:05 model-construction failure.

Read-only inspection of Oculus-xhfp-backport revision
`bb9e122629084c0f1a60c7a73eb10645330547f5` and its runtime JAR matched the
installed Oculus SHA-256:
`207B34530EB4BA1CB0B210B057F6AF8C2CF6FB8701D00E163E50A0A676FF8B62`.
That implementation retains `BlockRenderingSettings`' extended-format flag
after pipeline destruction, while its shader API reports no active shader
without a pipeline. Reload before a world exists defers pipeline recreation.
This explains why shader-state-based format inference can disagree with
actual buffered geometry. It is not a deterministic live reproduction.

## Correction

- Capture the input `DrawState.format()` in both native BlockModel constructor
  paths. Accept exactly vanilla BLOCK and Oculus TERRAIN, and select readers
  by that captured format regardless of current pipeline/configuration state.
- Preserve extended attributes, vertex stride, shade boundaries and native
  buffer ownership. Reject unsupported or truncated input explicitly.
- Pause Create's contraption render-manager tick and Flywheel's instance tick
  while the extended format is enabled but no shader pipeline exists. Do not
  consume their pending model work; the next eligible tick retries normally.
  This affects client visual preparation (including Flywheel's visual clock),
  not server simulation. There is no timeout that consumes unsafe work.
- Resume on a new pipeline, or when Oculus genuinely disables extended format.
  Existing optional-mod/version gates and startup kill switch remain intact.
- Remove the unreachable vertex-format forcing hook and its private Iris
  BufferBuilder-field accessor. No direct shader flag resets are introduced.
- Release the delegate and superclass native copies on extended-reader deletion,
  preserving Flywheel's existing exactly-once deletion contract.

Chunk budgeting, its default, world data, and other mods are unchanged. The
pre-update and post-update Flywheel compatibility bytes were identical before
this fix; an indirect timing effect from chunk scheduling cannot be excluded.

## Offline coverage and acceptance boundary

Regression tests exercise 32/52-byte readers, multiple vertices, extended
attributes, ownership after producer mutation, shade boundaries, malformed
input, actual mixin handler format capture, pipeline-gap/recovery decisions,
native-copy cleanup calls, and exact native constructor/tick bytecode targets.
The mapped Oculus fixture preserves byte layout but deliberately does not
model GL bindings or apply Oculus's constructor-relaxation mixin. Structural
tests are not a full Mixin transformation or an in-game acceptance test.

Required before declaring the reported crash resolved in play: repeated login
at the affected base with DH and SolasVH enabled, moving Create contraptions,
shader toggles/reloads, dimensions, and a shaders-off control. Check for missing
geometry, bad lighting, delayed recovery, native errors and injection failures.
No Prism installation or game launch is part of this source-fix task.

## Local validation, 2026-09-03

- Five pack compile checks passed: VaultCrafters Bootstrap, Asgard-SMP, Wolds
  Vaults 0.32.2/0.33.0, and Vault Hunters Third Edition.
- Final universal clean build passed: 174 tests, zero failures/errors/skips.
- Additional VaultCrafters compile/test control with the exact inspected
  Oculus 1.6.7 DH-compatible JAR passed, also 174 tests. Only the six existing
  annotation-processor warnings were emitted; no new bridge warning.
- Runnable JAR contains the new reader/guard classes and the correct mapped
  `DrawState.format()` target. Removed forcing classes, test fixtures, JUnit,
  and Mockito are absent. Embedded third-party notices match the source.
- Candidate remains `vault_render_optimization.0.4.1.jar`; SHA-256:
  `2874D9C6BF2B163AA62DF2FFC74CB1096815A3D0012DD54970C793CCB9B72B53`.
- Installed VaultCrafters VRO hash remains unchanged. No public operations,
  renderer-source edits, version change, or tracked JAR replacement occurred.
