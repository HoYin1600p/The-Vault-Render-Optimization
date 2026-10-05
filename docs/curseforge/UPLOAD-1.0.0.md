# CurseForge upload sheet: The Vault Render Optimization 1.0.0

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
| Upload file | `vault_render_optimization.1.0.0.jar` |
| Display name | `The Vault Render Optimization 1.0.0` |
| Release channel | Release |
| Changelog type | Markdown |
| Changelog | `CHANGELOG-1.0.0.md` |
| Manual release | No |
| Game-version IDs | `9008`, `7498`, `9638`, `8326` |

The compatibility IDs map to Minecraft 1.18.2, Forge, Client, and Java 17.
Confirm them with the authenticated catalog check at upload time.

## Dependencies

| Project | Relation |
| --- | --- |
| Cloth Config API (348521) | **Optional dependency** (only the settings screen needs it; also a project default relation) |

Cloth Config is `compileOnly` and is not bundled in the jar. Sophisticated
Storage, Sophisticated Core, Embeddium, Rubidium, Oculus, ImmediatelyFast and
the other integrations are optional runtime integrations; they must not be
bundled or marked as required dependencies.

## Integrity

```text
File: vault_render_optimization.1.0.0.jar
Size: {{JAR_SIZE}} bytes
SHA-256: {{JAR_SHA256}}
Source state: v1.0.0 release commit and tag (reproducible build)
```

## Synchronized links

- GitHub Release:
  https://github.com/HoYin1600p/The-Vault-Render-Optimization/releases/tag/v1.0.0
- CurseForge project:
  https://www.curseforge.com/minecraft/mc-mods/vault-render-optimization
- Expected public file URL after upload:
  `https://www.curseforge.com/minecraft/mc-mods/vault-render-optimization/files/{fileId}`

## Update manifest plan

| Field | Value |
| --- | --- |
| Version | `1.0.0` |
| Critical | `false` |
| Message | `In-game GPU benchmark; GPU rendering is now opt-in` |
| Production state | latest/recommended set to `1.0.0` in the release commit (active when pushed) |

## Final safety checks

- Run the incremental public-identity scan and the agent-trace scan on the
  exact release commit, refs and jar before pushing.
- Verify the GitHub Release asset and the CurseForge upload are the same jar.
- Confirm Cloth Config is listed as an Optional dependency and nothing is Required.
- Confirm the jar contains `LICENSE` and the third-party notices, and no Cloth
  Config or other third-party classes.
- Update the project Summary and Description to `SUMMARY.txt` and
  `DESCRIPTION.md`: 1.0.0 makes GPU rendering opt-in and adds the GPU benchmark.
