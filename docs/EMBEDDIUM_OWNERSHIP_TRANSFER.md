# Embeddium-fork ownership transfer

This ledger records the renderer corrections transferred from HoYin1600p's
Embeddium stability fork into VRO. The source fork is read-only during this
work. Its implementations are not eligible for removal until the matching VRO
commit, renderer/version guards, automated tests, required in-game regressions,
and a control run after removal have all passed.

Validated renderer baselines:

- Embeddium `0.3.18+mc1.18.2`
- Rubidium `0.5.6`

Unknown versions fail closed. Every feature has an independent startup option
under `[embeddium_transfers]`; performance-sensitive paths yield in Compare
Mode while narrow correctness guards remain active. Startup diagnostics report
`APPLIED`, `YIELDED`, or `BLOCKED` with a reason.

| ID | Feature | VRO commit | Automated validation | Manual validation remaining | Source removable? |
|---|---|---|---|---|---|
| VRO-EMB-04 | Adjacent-position block occlusion | pending | pending | Framed/custom face hiding; Cake Vault temporary bedrock | No |
| VRO-EMB-05 | Null-buffer vertex sink | pending | pending | Flywheel/Create and loading transitions | No |
| VRO-EMB-10 | Direct CCL renderer lookup | pending | pending | CCL blocks and fluids | No |
| VRO-EMB-02 | Bounded vertex-buffer retention | pending | pending | Long-session native memory | No |
| VRO-EMB-03 | Preemptive async arena growth | pending | pending | Long-session VRAM and resize pressure | No |
| VRO-EMB-06 | Smooth non-luminous fluid lighting | pending | pending | Modded fluids, reloads, dimensions | No |
| VRO-EMB-08 | Chunk-layer shader-color reset | pending | pending | Animated text, shaders, DH, Create, Botania | No |
| VRO-EMB-01 | Chunk rebuild de-duplication | pending | pending | Rapid updates, fluids, block entities, mob-kill pressure, Vault Bedrock | No |

Source provenance is the repository ledger at
`VRO_OWNERSHIP_TRANSFER.md` in the Embeddium fork at commit `7b085088`,
plus the per-feature source commits recorded in the table and source headers.
