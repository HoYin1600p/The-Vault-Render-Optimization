# CurseForge upload sheet: The Vault Render Optimization 0.5.0

## Project metadata

| Field | Value |
| --- | --- |
| Project ID | `1637635` |
| Project slug | `vault-render-optimization` |
| Summary | `Client-side Vault Hunters performance: faster Create rendering, particles, chunks, models, Sophisticated Storage displays, shaders, and stability.` |
| Environment | Client only |
| Minecraft | `1.18.2` |
| Forge | `40.3.11+` in the Forge 40.x line |
| Java | 17 |

## File upload

| Field | Value |
| --- | --- |
| Upload file | `vault_render_optimization.0.5.0.jar` |
| Display name | `The Vault Render Optimization 0.5.0` |
| Release channel | Release |
| Changelog type | Markdown |
| Changelog | `CHANGELOG-0.5.0.md` |
| Manual release | No |
| Game-version IDs | `9008`, `7498`, `9638`, `8326` |

The compatibility IDs map to Minecraft 1.18.2, Forge, Client, and Java 17.
Confirm the IDs against the CurseForge file form at upload time.

## Dependencies

| Project | Relation |
| --- | --- |
| Cloth Config (Forge) | **Optional dependency** (new in 0.5.0; only the settings screen needs it) |

Cloth Config is `compileOnly` and is not bundled in the jar. Sophisticated
Storage, Sophisticated Core, Embeddium, Rubidium, Oculus, ImmediatelyFast and
the other integrations are optional runtime integrations; they must not be
bundled or marked as required dependencies. Do not add a required dependency.

## Integrity

```text
File: vault_render_optimization.0.5.0.jar
Size: {{JAR_SIZE}} bytes
SHA-256: {{JAR_SHA256}}
Source state: v0.5.0 release commit and tag
```

## Synchronized links

- GitHub Release:
  https://github.com/HoYin1600p/The-Vault-Render-Optimization/releases/tag/v0.5.0
- CurseForge project:
  https://www.curseforge.com/minecraft/mc-mods/vault-render-optimization
- Expected public file URL after upload:
  `https://www.curseforge.com/minecraft/mc-mods/vault-render-optimization/files/{fileId}`

## Update manifest plan

| Field | Value |
| --- | --- |
| Version | `0.5.0` |
| Critical | `true` |
| Message | `[CRITICAL] GPU entity, particle and item rendering, settings screen, built-in ImmediatelyFast` |
| Production state | latest/recommended set to `0.5.0` at release (activated immediately) |

The approval monitor may promote `0.5.0` only after the exact CurseForge file
page is public, its public download succeeds, and the downloaded jar SHA-256
matches the release ledger. Pending or mismatched files must not change the
production update JSON.

## Final safety checks

- Run the required incremental public-identity scan immediately before every
  public push and artifact upload.
- Verify the GitHub Release asset and CurseForge upload are the exact same jar.
- Keep the production update JSON on 0.4.2 until public CurseForge verification.
- Confirm Cloth Config is listed as an Optional dependency and nothing is Required.
- Confirm `vault_render_optimization.0.5.0.jar` contains `LICENSE` and the
  third-party notices, and no Cloth Config or other third-party classes.
- Confirm the project Summary and Description match the reviewed
  `SUMMARY.txt` and `DESCRIPTION.md` (see the 0.5.0 description proposal).
- Do not push, tag, upload or publish until the owner explicitly requests it.
- Use the full GitHub notes and this shorter user-facing CurseForge changelog.
