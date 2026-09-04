# Render optimization follow-up

Recorded 2026-09-03; updated after authorization to implement item 2.
Items 1 and 2 are implemented for local evaluation. Item 3 remains deferred;
no scheduled work or automatic deployment is requested.

1. **Index-only translucent sorting (implemented; in-game evaluation pending).** Keep unchanged terrain
   vertices and replace only drawing-order indices. The older Embeddium path
   copies both buffers and re-uploads vertices on sort-only updates. Preserve
   native sorting, shader vertex formats, cancellation and geometry generations.
2. **Adaptive build/upload budgets (revised after runtime regression; retest pending).**
   Per-machine feedback, measured worker/upload costs, FIFO batch pacing, queued
   memory backpressure and aged-class admission opportunities. See
   [the design and validation plan](ADAPTIVE_CHUNK_BUDGET.md). No benchmark on the
   development PC sets other players' throughput. Budgets are predictive, not
   hard timing or total-native-memory guarantees. The first version delayed cached
   full terrain; disabling it restored loading immediately, including with DH off.
   Revision 2 is default-off, bypasses initial terrain and yields to native
   throughput on backlog/wait pressure. It may spend most of a loading session in
   fallback and produce no performance gain. Item 3 remains a separate decision.
3. **Smarter scheduling/background visibility (deferred).** Consider age,
   distance, visibility and task type, then asynchronous visibility processing.
   Much larger compatibility surface: shader/DH integration, teleports and
   correctness when geometry changes must be independently validated.

Existing VRO work already covers native deferral, rebuild de-duplication,
bounded vertex-buffer retention and preemptive arena growth. Those are not new
backlog items. No performance percentage is promised for the candidates.

Upstream design references (inspect the applicable license before copying code):

- [Sodium index upload separation](https://github.com/CaffeineMC/sodium/blob/dev/common/src/main/java/net/caffeinemc/mods/sodium/client/render/chunk/region/RenderRegionManager.java)
- [Sodium scheduling/upload budgets](https://github.com/CaffeineMC/sodium/blob/dev/common/src/main/java/net/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager.java)
- [Asynchronous graph culling and frame-independent scheduling](https://github.com/CaffeineMC/sodium/pull/2887)

These are design precedents, not drop-in patches for Minecraft 1.18.2.
