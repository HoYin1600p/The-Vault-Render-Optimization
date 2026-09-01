# Draft changelog: renderer stability transfers

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

All entries remain unreleased and require the compatibility and in-game checks
recorded in `docs/EMBEDDIUM_OWNERSHIP_TRANSFER.md`.
