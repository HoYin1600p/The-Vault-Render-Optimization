# CurseForge upload sheet: The Vault Render Optimization 1.0.1

## Project metadata

| Field | Value |
| --- | --- |
| Project ID | `1637635` |
| Project slug | `vault-render-optimization` |
| Summary | `Client-side Vault Hunters performance: optional GPU entity, item and particle rendering with an in-game benchmark, a settings screen, faster Create, chunks, particles and storage displays.` |
| Environment | Client only |
| Minecraft | `1.18.2` |
| Forge | `40.3.11+` in the Forge 40.x line |
| Java | 17 |

## File upload

| Field | Value |
| --- | --- |
| Upload file | `vault_render_optimization.1.0.1.jar` |
| Display name | `The Vault Render Optimization 1.0.1` |
| Release channel | Release |
| Changelog type | Markdown |
| Changelog | `CHANGELOG-1.0.1.md` |
| Manual release | No |
| Game-version IDs | `9008`, `7498`, `9638`, `8326` |

The compatibility IDs map to Minecraft 1.18.2, Forge, Client, and Java 17.
Confirm them with the authenticated catalog check at upload time.

## Dependencies

| Project | Relation |
| --- | --- |
| Cloth Config API (348521) | **Optional dependency** (only the settings screen needs it; also a project default relation) |

Cloth Config is `compileOnly` and is not bundled in the jar. Sophisticated
Storage, Sophisticated Core, Embeddium, Rubidium, Oculus, ImmediatelyFast,
ItemBorders, and the other integrations are optional runtime integrations; they
must not be bundled or marked as required dependencies.

## Integrity

```text
File: vault_render_optimization.1.0.1.jar
Size: 1204315 bytes
SHA-256: 57f49abeae3277aaf6ed293fc64354166e64c0c7d737bf064644527c52b12738
Source state: v1.0.1 release commit and tag (reproducible build)
```

## Synchronized links

- GitHub Release:
  https://github.com/HoYin1600p/The-Vault-Render-Optimization/releases/tag/v1.0.1
- CurseForge project:
  https://www.curseforge.com/minecraft/mc-mods/vault-render-optimization
- Expected public file URL after upload:
  `https://www.curseforge.com/minecraft/mc-mods/vault-render-optimization/files/{fileId}`

## Update manifest plan

| Field | Value |
| --- | --- |
| Version | `1.0.1` |
| Critical | `false` |
| Message | `Vault 22.0.x compat, GPU/barrel fixes, angle occlusion & sound culling` |
| Production state | Prepared in `update.json` on branch (activates only when merged and pushed to `main`) |

## Final safety checks

- Verify the exact release commit, tag, and jar metadata before pushing.
- Verify the GitHub Release asset and the CurseForge upload are the same jar.
- Confirm Cloth Config is listed as an Optional dependency and nothing is Required.
- Confirm the jar contains `LICENSE`, `CREDITS.md`, `THIRD_PARTY_NOTICES.md`,
  and all required upstream license texts, with no third-party mod classes.
- Synchronize CurseForge project description with `DESCRIPTION.md` when publishing.
