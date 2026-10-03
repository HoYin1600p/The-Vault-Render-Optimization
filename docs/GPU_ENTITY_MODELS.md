# GPU entity models

Status: **on by default** (`render_fast_paths.gpu_entity_models = true`). Validated in game with the
live verifier (0 mismatching vertices) on public Embeddium, public Oculus with shaders off, and
custom Embeddium/Oculus builds; it still turns itself off wherever the conditions below say so.

## What it does

Normally, every frame the CPU transforms every vertex of every entity model part (zombies,
skeletons, Vault mobs, armor) and writes 36 bytes per vertex into Minecraft's entity buffer.

With this feature on, an eligible model part instead **reserves** its vertices in that same buffer
and records one small instance (pose matrix, normal matrix, colour, light, overlay). Each part's
cubes are captured once into a GPU mesh arena. When vanilla uploads the buffer
(`BufferUploader._end`), a compute shader writes the reserved vertices straight into the vertex
buffer it has just uploaded, and then vanilla issues its own draw call.

Nothing about the draw changes. The following are all vanilla's:

- the buffer;
- the vertex order and draw order;
- the render type state;
- the shader and uniforms;
- the depth test and back-face culling.

Nothing is culled, skipped or reordered.

## GeckoLib 3 models

Vault Hunters draws many of its own mobs (knights, Death and Acid mobs, Naga, Scarabs, tanks,
bosses, pets) with GeckoLib 3, which builds every cube on the CPU in `IGeoRenderer.renderCube`.
`GeoEntityRenderer.renderRecursively` calls `renderCube` once per cube inside its own push/pop;
VRO redirects that call. For an eligible cube it runs `renderCube`'s own pose steps
(`translateToPivotPoint`, `rotateMatrixAroundCube`, `translateAwayFromPivotPoint`) and reserves the
cube's cached mesh, so the same compute shader writes the vertices `createVerticesOfQuad` would:

- positions through the pose matrix and normals through the normal matrix, without normalizing;
- GeckoLib's flat-cube fix: for a cube with zero size along another axis, a negative transformed
  normal component is negated. It is carried in the instance's flags word and is exact.

A cube stays on GeckoLib's own path when:

- the buffer is not a plain `BufferBuilder` (glowing outlines, enchantment glint, translucent);
- the renderer class declares its own `renderCube` or `createVerticesOfQuad`;
- another mod changed `IGeoRenderer.renderCube` or `createVerticesOfQuad` (audited at startup);
- a face is not a four-vertex quad, or the mesh arena is full.

GeckoLib's cube fields are mutable, so with `/vro gpuentity verify on` each cached mesh is also
compared with its live cube and recaptured if it changed (`meshes recaptured` in the stats).
`GeckoLibReferenceTest` runs GeckoLib's real `renderCube` as the oracle on 3000 random cubes (box
and face UVs, missing faces, flat cubes, mirroring, inflation, random poses).

Measured on CMA Asgard with 192 GeckoLib vault mobs in view: 90.5 FPS off, 135.6 FPS on (+50%),
render CPU 10.0 to 6.5 ms per frame, and 158.9 million live-verified vertices with 0 mismatches.
GeckoLib block, item, armor and layer renderers are not covered yet.

## Ars Nouveau and Citadel (only when installed)

- **Ars Nouveau** shades its own, older GeckoLib 3 copy under `software.bernie.ars_nouveau.geckolib3`.
  Its cube, quad and vertex arithmetic is identical; the same `renderCube` redirect and audit are
  applied to that package, with its `moveToPivot`/`rotate`/`moveBackFromPivot` pose steps.
- **Citadel** (`AdvancedModelBox`, used by Alex's Mobs) renders each part through a private `doRender`
  whose arithmetic is vanilla `ModelPart.compile`'s. VRO redirects that one call in `render`, so
  Citadel's own transforms, scaling and children are untouched; `doRender` itself is audited.

All of these are `@Pseudo` mixins: without the mod nothing applies. Each GeckoLib cube and Citadel
part carries one added field for its GPU mesh, so the per-frame lookup is a field read.
`ArsGeckoLibReferenceTest` and `CitadelReferenceTest` run each mod's real rendering as the oracle.

In CMA Wolds Vaults 0.34.1 with 621 different mobs in view (every spawnable entity type in the pack),
the live verifier checked 86.8 million vertices with 0 mismatches. With the path on, CPU vertex
building for entity models fell to about 5% of the render thread (GeckoLib renderers that replace
GeckoLib's cube code stay on the CPU); the rest of that scene's cost is per-entity work such as
animation, so its frame-rate gain is smaller than in the Vault Hunters GeckoLib scene.

## Items (`gpu_items`, default on)

Dropped items, items in frames and held items go through `ItemRenderer.renderQuadList`, one call per
side of the item model. VRO hooks that call; for an eligible list, the compute shader writes exactly
what the installed writer would.

- **Two writers.** The mixin audit reads the final `renderQuadList`:
  - Embeddium's overwrite gives the EMBEDDIUM writer.
  - Vanilla's body gives the FORGE writer (Forge's `putBulkData`).
  - Any other change keeps items on the CPU.
  - Quark's item-sharing hook is allowed only before VRO's. It draws its faded items itself and cancels.
- **The arithmetic.** Position and normal transforms are the same as for model parts. Colour lanes are
  `(baked * tint) / 255`, which equals both writers' float maths for all 65,536 pairs. The writers differ
  in two places, both reproduced (`ItemReference`):
  - the normal input: Forge takes `baked / 127`, carried over from the previous vertex when zero;
    Embeddium takes `byte * 0.007874016`, falling back to the face direction;
  - the baked-light merge.
- **With a shader pack**, Embeddium writes Oculus' extended format directly. The normal of the quad's
  fourth vertex goes to all four vertices, and the tangent is computed from that normal unnormalised.
  The item variant of the Oculus program reproduces this.
- **Eligible:** plain `BufferBuilder`s, including sorted render types. That covers solid and cutout block
  items, and also flat items (about 63% of Vault Hunters' item models) and translucent blocks, which use
  the sorted `entity_translucent_cull`. Vanilla's sort reads only each quad's vertex 0 and 2 positions
  (midpoint, squared distance, stable descending merge sort). So right before the sort, VRO writes just
  those two positions per reserved quad with the reference arithmetic. Vanilla then sorts and writes the
  indices itself. The holes stay for the GPU, and the CPU-written floats are never uploaded. Verify mode
  also checks those positions against the full reference.
- **Stay on the CPU:** glint and outline (wrapped consumers) and custom-renderer items. Batches under 96
  vertices (HUD icons, a held item) are filled exactly on the CPU, because a dispatch and barrier would
  cost more.
- **Oculus' batched buffer source** (active even with shaders off) gets the same sorting hint as
  vanilla's, so translucent model parts no longer reserve and then refill on the CPU.
- **Meshes** are cached per quad list, keyed by identity. Vault's gear models build a new wrapper for every
  stack but hand out the same lists. Each use checks the list's size and every quad's identity.
- **Records:** consecutive sides of one item that continue the arena range and the output share one GPU
  record.
- **Tests:**
  - `ItemReferenceTest` checks the FORGE reference against a real `BufferBuilder`. It checks the EMBEDDIUM
    reference against a transcription of Embeddium's code, and the Oculus variant against Oculus'
    `NormalHelper`.
  - The self-test covers both writers, with and without Oculus.
  - In verify mode, every list is also written through the installed writer into a scratch buffer and
    compared with the reference (the item oracle).

CMA Asgard, 480 floating item stacks (block items plus tinted, translucent, flat and glint ones):

| | GPU vs reference | Reference vs installed writer (oracle) | FPS off → on | Allocation |
|---|---|---|---|---|
| Shaders off | 21.6 M vertices, 0 mismatches | 28.3 M vertices, 0 mismatches | 217 → 232 (+7%) | 2.7 → 2.2 MB/frame |
| SolasVH | 11.0 M vertices, 0 mismatches | 14.4 M vertices, 0 mismatches | 92.7 → 93.2 (neutral) | 3.7 → 2.7 MB/frame |

The same scene plus 240 flat stacks (gems, ingots, tools, food, panes, ice), with sorted items on:

| | GPU vs reference | Oracle | Sorted batches | FPS off → on |
|---|---|---|---|---|
| Shaders off | 66.3 M, 0 mismatches | 64.9 M, 0 mismatches | 555, 0 position mismatches | 132 → 154 (+17%) |
| SolasVH | 39.8 M, 0 mismatches | 39.0 M, 0 mismatches | 334, 0 position mismatches | 91.0 → 94.6 (+4%) |

### Block entities (Vault Hunters and Wolds Vaults)

Every block entity of the_vault (145 types, 105 with renderers) and Wolds Vaults (10 renderers) was placed
in test scenes. Each was placed with state and NBT that make it draw its content: items on pedestals and
altars, filled chests, statues and so on.

| Scene | GPU vs reference | Item oracle | Sorted item batches | FPS off → on |
|---|---|---|---|---|
| Wolds, 137 variants, shaders off | 24.5 M, 0 mismatches | 3.8 M, 0 mismatches | 1,982, 0 mismatches | 363 → 390 (+7.5%) |
| Asgard, 117 variants, shaders off | 18.1 M, 0 mismatches | 2.2 M, 0 mismatches | 1,574, 0 mismatches | not measured |
| Asgard, 117 variants, SolasVH | 11.5 M, 0 mismatches | 1.4 M, 0 mismatches | 966, 0 mismatches | 112.9 → 112.6 (neutral, GPU-bound) |

- **Stay on the CPU by design:**
  - custom vertex consumers and buffer sources (ArtifactProjector, Wardrobe);
  - raw `vertex()` writers (challenge controllers, totems, sparks, cryo chambers);
  - text, and glint (EternalPedestal, UniqueWardrobe);
  - custom shaders (FinalVaultFrame, SoulPlaque).

  None of these were affected.
- **Immediate-mode renderers** (controller proxies, Monolith, GodAltar and others) upload through the
  normal `BufferUploader` path.
- **GeckoLib 3 block renderers** (CardBinder, CompanionIncubator, GodObelisk, VaultGlobe,
  VaultRoyalePillar):
  - Their cubes are reached through `IGeoRenderer.renderCubesOfBone`, an interface default method,
    and Mixin cannot hook it.
  - A `@Pseudo` mixin instead adds a `renderCube` override to `GeoBlockRenderer`. It tries the same
    reservation as for entities and otherwise runs GeckoLib's own default.
  - The audit checks that the merged method is VRO's.
- **Whole block models** (`ModelBlockRenderer.renderModel`: vault portals, spawners, crate crackers):
  - They use the item program. Both writers ignore the quads' baked colour and write
    `tinted ? clamp(r, g, b) : white` with alpha 255, and Embeddium's overwrite writes the raw light.
  - So an item mesh with its baked colour forced to white (and, for Embeddium, its baked light to 0)
    reproduces them exactly.
  - Only models whose quad lists don't depend on the random are used. The writers seed different
    generators with 42, and this is checked once per model.
  - `BlockModelReferenceTest` checks against the real Forge `renderModel`. The in-game oracle checks
    Embeddium's, including its Oculus writer.
- **ImmediatelyFast** (the mod) replaces the immediate buffer source, whose `getBuffer` never reached the
  sorting hint. Translucent parts were reserved and then refilled on the CPU before its sort, about
  7,000 times per 10 s in Wolds. It now gets the hint.
- **Small batches:** builders that keep flushing tiny batches stop reserving, and retry every 64th draw.

Wolds scene afterwards:
- GPU vertices: 26.6 M, 0 mismatches;
- oracles: items 4.4 M vertices and block models 214 k vertices, 0 mismatches;
- about 92 k GeckoLib block cubes and 3.9 k block models on the GPU per 12 s;
- pre-sort CPU refills: 0.

Asgard, block models:
- shaders off: 158 k oracle vertices, 0 mismatches;
- SolasVH: 87 k oracle vertices, 0 mismatches.
- **Mesh arena deduplication:** identical geometry now reuses one arena range, keyed on the exact bits.
  A renderer that builds a new `ModelPart` every frame therefore no longer grows the arena.

First-person held items and hotbar icons were verified with shaders off and on: 0 mismatches, and no CPU
fills from the hand-off.

## Exactness

The shader reproduces vanilla's float arithmetic bit for bit:

- It uses `precise` and vanilla's operation order: `((m00*x + m01*y) + m02*z) + m03`.
- It uses no `normalize`, `fma` or `packSnorm`.
- It applies vanilla's truncating normal and colour packing.

The Java reference it is checked against reproduces vanilla `ModelPart.render` output through a
real `BufferBuilder` byte for byte (400 random models and poses in `ReferenceExpanderTest`).

At startup, the shader must match that reference on every word of 512 random instances, and must
leave the words between them untouched. Any difference turns the feature off for the session.

On the development machine (RTX 5080, driver 610.88, Minecraft's own 3.2 core context), the shader
matched on over 7,000 instances. Without `precise`, the driver fused multiply-adds and the
self-test caught a one-ULP difference. `/vro gpuentity selftest` re-runs it in game with a new
seed.

The one theoretical exception is non-finite or subnormal intermediate values (NaN payload bits,
flushed denormals). They cannot change a rendered pixel. The self-test avoids them.

## When the CPU writes the reserved vertices instead

Reserved vertices never reach anything unwritten. When a buffer with reservations goes anywhere
other than the immediate upload, VRO first writes the exact vanilla bytes on the CPU. Those cases
are:

- quad sorting (translucent render types such as players);
- another uploader (`_endInternal`, a `VertexBuffer`, off-thread uploads);
- a driver without the needed features;
- a failed dispatch.

A discarded buffer drops its reservations with its vertices.

## When it is off

| Condition | Result |
|---|---|
| `gpu_entity_models = false` or Compare Mode | off |
| No compute support: needs OpenGL 4.3, or GLSL 1.50 plus ARB_compute_shader, ARB_shader_storage_buffer_object, ARB_program_interface_query, ARB_shader_image_load_store and ARB_gpu_shader5 | blocked |
| Software or translation renderer (llvmpipe, gl4es, Apple and others) | blocked |
| Shader compile/link failure or self-test mismatch | blocked |
| OptiFine installed | mixins not applied |
| An Oculus shader pack is active, unless `gpu_entity_models_with_shaders` is on and the extended program passed its self-test | paused (checked every frame) |
| Mixin audit fails: another mod injects into or overwrites `ModelPart.render`/`compile` or `ModelPart$Cube.compile` (e.g. wildbackport's `render` HEAD hook), or any VRO buffer hook is missing | blocked |
| A runtime GL failure | failed for the session; the CPU path takes over |

Other injections into `ModelPart.translateAndRotate` are fine, because the GPU path calls it. The
Vault's scale hook, otyacraftengine's and wildbackport's `translateAndRotate` hooks all fall in
this group.

Embeddium/Rubidium's `compile` overwrite is also allowed. It writes the same bytes.

Standalone ImmediatelyFast and VRO's built-in copy both draw through `BufferUploader`, so both are
supported.

## Eligible model parts

A part is eligible when all of these hold:

- it is drawn on the render thread into a plain `BufferBuilder`, or into a `SpriteCoordinateExpander`
  around one (block entities such as chests, beds and signs; the atlas remap is applied on the GPU);
  foil/glint and outline wrappers stay on the CPU;
- the builder is building a `NEW_ENTITY` quad buffer, with no fixed colour and no sort in progress;
- every cube face is a quad.

Other parts use vanilla's path unchanged.

The mesh arena holds up to 64 MiB and is rebuilt on resource reload. When it is full, new models
use the CPU path.

## Upload and Oculus

When an upload carries reserved vertices, VRO copies only the ranges the CPU wrote, and the GPU
fills the reserved ranges in place, so the driver never copies them. Adjacent reserved parts form
single runs.

With public Oculus 1.6.x installed and shaders off, entities go through Oculus' batched entity
rendering, which collects each render type's slice and draws it later through vanilla's upload:
- Reserved batches are parked by slice address while Oculus collects the slices.
- When a slice is drawn, its parked batch goes to the GPU, after a vertex-count and format check.
- A parked batch is dropped if its builder starts writing again, and at each frame start.

With a shader pack active the path pauses, unless `gpu_entity_models_with_shaders` is on
(experimental, default off; `/vro feature gpushaders on|off`). Oculus then switches entity buffers
to its 56-byte extended format and, after every four vertices, overwrites the normal with the quad's
face normal and writes the mid UV, the tangent and the captured entity, block entity and item IDs.
A second build of the compute program writes those vertices, one thread per quad:
- The float arithmetic is `NormalHelper.computeFaceNormal`, `computeTangent` and `packNormal`,
  step for step. `IrisEntityExtensionTest` checks the Java reference against Oculus' own
  `NormalHelper` on 200,000 random and degenerate quads.
- Oculus relies on Java's correctly rounded division and square root, and on
  `(float) (1.0 / Math.sqrt(x))`, which equals the correctly rounded float reciprocal square root
  for every float (checked exhaustively). GLSL's `/`, `sqrt` and `inversesqrt` only give a
  starting value, computed without subnormal arithmetic. It is then moved to the correctly rounded
  result by comparing the exact value with the midpoints to its neighbours in integer arithmetic
  (`umulExtended`). The real-GPU test checks 16 million operand pairs per operation, including
  subnormals, zeros, infinities and NaN.
- Reservations happen only between Oculus' quads (its own vertex counter is 0), and the IDs are
  captured at reserve time. The two padding bytes Oculus leaves unwritten are written as zero;
  they are not a vertex attribute.
- It has its own startup self-test; a failure only keeps the path paused under shader packs.

On CMA Asgard with SolasVH (RTX 5080, 4K), live verify checked 63.1 million GPU-written vertices
with 0 mismatches over 33,620 extended draws. Particles also stayed on the GPU. It was still slower,
113 FPS without the path against 104-107 with it: with a shader pack the GPU was at 99%, so the CPU
time saved did not matter and the extra compute passes cost GPU time. That is why it is off by default.

`gpu_particles_with_shaders` (`/vro feature gpushaderparticles on|off`, default off) keeps only
particles on the GPU under a shader pack. Particles keep the vanilla particle format, so no extended
program is involved. Under Nova stress with SolasVH it verified 6.6 million vertices with 0
mismatches and was FPS-neutral (24.0 against 23.4) on the same GPU-bound machine. Both toggles are
meant for players whose CPU, not GPU, is the limit with shaders.

## Commands

- `/vro gpuentity status`: the state, the reason for it, the mixin audit and the driver.
- `/vro gpuentity on|off`: toggles the config.
- `/vro gpuentity stats [reset]`: parts and vertices on the GPU, dispatches, CPU fills, arena use.
- `/vro gpuentity selftest`: re-runs the bit-exact self-test.
- `/vro gpuentity verify on|off`: reads back every dispatch and compares the whole uploaded buffer
  with the exact expected bytes. It stalls the GPU, so use it for testing only.

## Validation still required (in game)

- Status is `ACTIVE`, and the self-test passes on NVIDIA, AMD and Intel.
- Compare Mode A/B screenshots are identical for:
  - zombies and skeletons;
  - creepers (hurt flash);
  - villagers and iron golem cracks;
  - spider and enderman eyes;
  - sheep, baby mobs, armor stands with armor, Vault mobs and gear;
  - glowing and invisible entities;
  - Fabulous graphics.
- An Oculus shader pack toggled mid-session pauses and resumes the path cleanly.
- F3+T reload, dimension change and window resize behave normally.
- A Vault room with 100+ mobs: frame time, `/vro gpuentity stats` showing few CPU fills, and no GL
  errors.

Credit: the approach is adapted from Accelerated Rendering by Argon4W (MIT). See
`THIRD_PARTY_NOTICES.md`.
