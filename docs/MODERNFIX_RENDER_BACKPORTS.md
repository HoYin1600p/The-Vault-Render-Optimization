# ModernFix render-backport provenance and ownership

VRO contains selected client rendering and graphics adaptations from
[ModernFix](https://github.com/embeddedt/ModernFix). The source files retain
their original copyright and `SPDX-License-Identifier: LGPL-3.0-or-later`
headers. VRO remains distributed as AGPL-3.0-or-later; the complete ModernFix
LGPL license is retained at
`docs/licenses/modernfix-LGPL-3.0-or-later.txt` and in the runnable jar.

The Minecraft 1.18.2 adaptations were first developed and validated in
VH Accelerator, then relocated into VRO from VHA commit
`7d2a69943e6e5bee629759b5ad0c4c47f6a20980`. VHA was treated as read-only
during this move.

## Included adaptations

| VRO feature | ModernFix source and revision | VHA feature commit | Preserved adaptation |
| --- | --- | --- | --- |
| Chunk meshing | `common/.../util/blockpos/SectionBlockPosIterator.java` at `2e52db6e932abc310a3bbaa391ab492a5486847e`; `common/.../perf/chunk_meshing/RebuildTaskMixin.java` at `7c550a1ce485f4a253f09447cddebf0e6839e554` | `a4d485b8c47c1797616797dc418cee3340eab385` | Vanilla X/Y/Z traversal, non-section fallback, position-checked per-task state reuse, Fluidlogged exclusion. |
| Duplicate BufferBuilder guard | `common/.../bugfix/buffer_builder_leak/RenderBuffersMixin.java` at `d51b0f60a23b167b6ee8459073c706ab8b20a6fe` | `881ed05725ac4c21c1d22b575ca9e6ac650d281f` | Forge 1.18.2 pre-allocation duplicate-key guard and the Isometric Renders/Wither Storm exclusions. |
| Entity-model cube compaction | `common/.../perf/compact_entity_models/CubeDefinitionMixin.java` at `5a9c49f8d405502c5c1e50a42cf27a8597e541a0` | `c34e8a9920c697efaeff2e4c55bb85f37eefe5af` | Minecraft 1.18.2 cube key, primitive float-bit fields, external concurrent cache, reload-generation clearing. |
| Profile texture URL/hash cache | `common/.../perf/cache_profile_texture_url/SkinManagerMixin.java` at `e859ce8eb6b7b05c79179becf67df32e3efc4ad5` | `260a30817c13f42f5f524fe9561063d8b24ee897` | Exact URL keys, 2,048-entry maximum, 60-second expiry after access. |
| Multipart selector cache | `common/.../perf/model_optimizations/SelectorMixin.java` at `fe855f15304ed788122a27cda4c2495a78374528` | `af232f206ffa2f9b61192df4e3915587cb8a2048` | Per-selector concurrent predicate cache with first-result publication. |
| Model variant traversal | `common/.../perf/model_optimizations/MultiVariantMixin.java` at `3ad4e2478e6965e902d1a77e2483d770d0a363d3` | `39458902700e150ba94737544b9da85f59f10ef7` | Allocation-light 1.18.2 dependency/material traversal with missing-texture reporting preserved. |
| Transformation hash cache | `common/.../perf/model_optimizations/TransformationMatrixMixin.java` at `fe855f15304ed788122a27cda4c2495a78374528` | `6801ae5737df02629ce3ed6a379b1c7979428003` | Primitive cached hash plus explicit computed state for immutable matrices. |
| Forge OBJ cache concurrency | `forge/.../perf/model_optimizations/OBJLoaderMixin.java` at `fe855f15304ed788122a27cda4c2495a78374528` | `a1630a9d856e5d09d1039695c1dbebf9a3ba5892` | Constructor-time replacement of material and model maps with concurrent maps. |
| Guarded STB atlas stitching | `common/.../textures/StbStitcher.java` and `common/.../perf/faster_texture_stitching/StitcherMixin.java`; audited at `94c848b0debbb5291ab3c709353e3f11613fd14d` | `dabcd637d82c50362d3c1e24adf2ba89cb55cd95`; ownership correction `7d2a69943e6e5bee629759b5ad0c4c47f6a20980` | Vanilla below 100 sprites, atlas-limit fallback, LWJGL binding compatibility, and active-ModernFix ownership. |
| Forge model-data concurrency | `forge/.../bugfix/model_data_manager_cme/ModelDataManagerMixin.java` at `e253833b685e0bb4ee5de62860ce81b7d886a311` | `4ecb9c76b758498faf2acc94f227af2e48bde009` | Concurrent key sets, client-thread block-entity refreshes, neighboring chunk refresh, legacy Rubidium exclusion. |
| CTM metadata concurrency | `forge/.../bugfix/ctm_resourceutil_cme/ResourceUtilMixin.java` at `5de87576ca17b920e88f9c4fc289f3df064ef694` | `4ecb9c76b758498faf2acc94f227af2e48bde009` | Optional string target, exact validated CTM layout, synchronized map that permits cached null values. |

The STB code also preserves ModernFix's attribution to
[GTNewHorizons/lwjgl3ify](https://github.com/GTNewHorizons/lwjgl3ify), source
commit `f21364cd3d178aef863458a2faa1f5718a4e350d`, licensed LGPL-3.0.

## Runtime ownership

Ownership is resolved once during client startup for each feature:

1. Reject an incompatible mod/layout or the wrong physical side.
2. During the temporary migration, yield when the exact current VHA feature
   class is present. Removing that class from VHA transfers ownership without
   relying on a broad VHA version check.
3. Yield when ModernFix reports the exact corresponding implementation active.
4. Fail closed if ModernFix ownership cannot be verified.
5. Respect launch-time Compare Mode and the individual VRO config switch.
6. Otherwise VRO owns the feature.

VRO never forces ownership of the texture stitcher or disables a stock
ModernFix implementation. `/vro backports` reports the immutable owner and
reason for every transferred feature in the current JVM.

## Publishing checklist

Before a release containing these files:

- run the complete unit-test and production-jar build;
- inspect the jar for the mixin list, access transformer, notices, and LGPL;
- test VRO alone, with the current VHA overlap, and with ModernFix 5.18;
- exercise Fluidlogged, Rubidium/Embeddium, and CTM gates where practical;
- verify resource reloads, models, textures, and client-thread behavior;
- preserve every file-level source header and this provenance ledger.
