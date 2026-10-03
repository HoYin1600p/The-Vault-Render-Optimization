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
most 8,192 positions per tick. World references are weak, and ParticleEngine
level changes clear the render-thread cache independently of lighting-mod ownership.

Particles that override `getLightColor` and never call `super.getLightColor` do not
enter the base-class cache. Overrides that call `super` (all of Vault's Nova
particles do) still use it.

### Diagnostics

`/vro particles diagnostics on` enables queue class snapshots, particle engine
render/tick timings, billboard writer counts, renderer passthrough counts,
per-particle and shared light hits, actual light lookups, and empty-render
skips. Use `reset` between benchmark conditions. Class snapshots intentionally
add profiling overhead, so diagnostics default off. The class census is sampled
every 250 ms, not every frame. Render timings include that sampled census work;
render/tick and writer counters still cover every instrumented call.

All particle commands and options are listed in
[`CONFIGURATION.md`](CONFIGURATION.md).

### Nova storms and other heavy bursts

Rule: VRO never culls particles. Every particle still spawns, ticks, collides
and renders exactly as its mod intends; these paths only make each one cheaper.
They target Vault Hunters Nova casts (Nova: 580 particles per cast; Frost Nova:
400; Poison Nova: up to about 1,800), whose particles collide every tick just
above the floor and are created in bursts of tens of thousands.

- **Exact collision cache** (`particle_collision_cache`). Particle movement
  collides with exactly the cells and shapes vanilla's `BlockCollisions` would
  visit, and vanilla's own `collideWithShapes` computes the response. Block
  state and collision shape are cached per position for one
  `ParticleEngine.tick`, when no block can change, so particles over the same
  floor share one read. A differential test compares it with a transcription of
  `BlockCollisions` over randomized worlds; `/vro particles collision verify on`
  compares every result in game and counts mismatches.
- **One-pass removal** (`particle_tick_compaction`). Dead particles are removed
  from the `ArrayDeque`-backed queue in one ordered pass instead of one array
  shift each. Tick order, death timing and particle-group limits are vanilla's.
- **Per-thread random generator** (`particle_shared_random`). Particles draw from
  one generator per thread with `java.util.Random`'s exact algorithm, instead of
  each allocating a `Random` (CAS, `nanoTime`, `AtomicLong`) and paying a CAS per
  draw. It also serves vanilla's constructor `Math.random()` calls, the Gaussian
  spread of particle packets, and Vault's Nova cloud, explosion and Frost/Poison
  Nova constructors.
- **Provider cache** (`particle_provider_cache`). Each particle type's provider
  is resolved once instead of through a registry key and hash lookup per spawn.
- **Allocation-free frustum test** (`allocation_free_frustum`). Forge's particle
  frustum check (and entity/block-entity checks) no longer allocates 48 vectors
  per test; the answer is bit-for-bit vanilla's.

Measure with `/vro particles diagnostics on` (tick/render ms, queue census) and
`/vro particles status` (collision cached/vanilla counts), and use Compare Mode
for the vanilla side of an A/B. `/vro particles stress nova|frost <casts per second>
<seconds> [radius]` replays casts beside the player through the same client code a
real cast runs (Vault's `NovaParticleMessage.spawnParticles` for Nova; a real
particle packet handed to the client's `handleParticleEvent` for Frost Nova).
It is client-only, sends nothing to the server and plays no sound; `stop` and
`status` control it. Poison Nova can be reproduced with vanilla `/summon` of an
`area_effect_cloud` using `the_vault:nova_dot`. Not done, because they would change what is
simulated or heard: building only the spawns vanilla's 16,384 queue would keep,
merging identical Nova sounds, and turning off Nova particle collision.

## Deferred work

Reusing one Embeddium particle writer per render batch needs a per-use
write-pointer resync and format check through Embeddium internals; it is left
until a profile shows the per-particle writer allocation matters.

GPU-side quad expansion is a
larger renderer/shader project. Asynchronous ticking remains rejected for the
current implementation because Vault Hunters particles commonly interact with
client-world, entity, and renderer state that is not thread-safe.

The local research and CMA benchmark setup remain in the external shadow workspace;
they are not shipped in release artifacts.
