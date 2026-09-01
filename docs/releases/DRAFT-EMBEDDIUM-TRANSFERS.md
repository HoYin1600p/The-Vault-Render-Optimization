# Renderer stability transfers included in the 0.4.1 candidate

- Correct custom block face hiding to evaluate the adjacent block at its real
  world position.
- Guard optimized vertex writers during buffer-less loading transitions.
- Reduce CodeChickenLib renderer lookup overhead on supported Embeddium stacks.
- Retain useful chunk-build buffers with bounded peak native memory.
- Grow renderer buffer arenas preemptively with an explicit VRAM ceiling.
- Preserve smooth lighting for non-luminous modded fluids with reload-safe
  selection caching.
- Restore white shader color after each chunk render layer.
- Coalesce equivalent chunk rebuild requests without losing important updates.
- Recognize Embeddium's same-JAR Rubidium compatibility identity without
  misclassifying stock Embeddium as two installed renderers.
- Apply the adjacent-position face-hiding correction with the exact stock
  Embeddium and Rubidium invocation signature.
- Select smooth fluid lighting through a render-entry state capture and the
  exact one-argument stock renderer invocation signature.
- Apply bounded async arena growth through the exact one-argument stock resize
  invocation signature while retaining the requested upload size.
- Keep async arena headroom as a fixed, capped increment derived from initial
  capacity, avoiding unsafe compounding growth during heavy chunk uploads.

All compatibility, in-game, source-removal, and post-removal single-owner
checks are complete. The player-facing summary is incorporated into
`docs/releases/0.4.1.md`; the complete evidence remains in
`docs/EMBEDDIUM_OWNERSHIP_TRANSFER.md`.
