# Particle optimizations

VRO's retained particle work lowers the CPU and allocation cost of particles
that are already eligible to render. It does not lower particle counts, shorten
their range, hide particles that are on screen, or tick particles on worker
threads.

## Retained paths

### Camera-basis billboards

Ordinary `SingleQuadParticle` instances normally derive four corners through
repeated rotation work. VRO adapts Flerovium's camera-left/up calculation and
applies particle roll to those two basis vectors once. The same four corners,
UV order, color, packed light, size, and interpolated position are written.

Particles that override the complete render method keep that method. With
Rubidium or Embeddium, VRO sends the calculated corners through the renderer's
packed particle sink. Without one, it uses Minecraft's normal
`VertexConsumer`. If Flerovium is installed, VRO applies neither render mixin.

Ownership is hot:

- `AUTO`: VRO geometry; packed renderer output when available.
- `RENDERER`: yield to Rubidium/Embeddium when present; otherwise safe VRO
  fallback.
- `VRO`: explicitly select VRO geometry.

### Particle light caches

The existing per-particle cache reuses light while one particle remains in the
same block during one client tick. The shared cache adds reuse between
particles in that same block and tick. It is thread-local, changes generation
with the client level or tick, reuses one mutable block position, and retains at
most 8,192 positions per tick.

Particles that override `getLightColor` do not enter the base-class cache.

### Diagnostics

`/vro particles diagnostics on` enables queue class snapshots, particle engine
render/tick timings, billboard writer counts, renderer passthrough counts,
per-particle and shared light hits, actual light lookups, and empty-render
skips. Use `reset` between benchmark conditions. Class snapshots intentionally
add profiling overhead, so diagnostics default off.

All particle commands and options are listed in
[`CONFIGURATION.md`](CONFIGURATION.md).

## Deferred work

Collision fast paths remain profile-gated future work because modded particles
can use unusual movement and collision semantics. GPU-side quad expansion is a
larger renderer/shader project. Asynchronous ticking remains rejected for the
current implementation because Vault Hunters particles commonly interact with
client-world, entity, and renderer state that is not thread-safe.

The local research and CMA benchmark setup remain in ignored repository notes;
they are not shipped in release artifacts.
