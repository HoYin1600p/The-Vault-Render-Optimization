# CurseForge upload sheet: The Vault Render Optimization 0.4.1

## Project fields

| Field | Value |
| --- | --- |
| Project name | `The Vault Render Optimization` |
| Summary | `Client-side Vault Hunters and Create performance: faster particles, models, chunk rendering, contraption culling, shader support, and stability fixes.` |
| Class | `Mods` |
| License | `GNU Affero General Public License v3.0 or later` |
| Description format | `Markdown` |
| Description | `DESCRIPTION.md` |
| Project logo | `vro-icon.jpg` |
| Source | `https://github.com/HoYin1600p/The-Vault-Render-Optimization` |
| Issues | `https://github.com/HoYin1600p/The-Vault-Render-Optimization/issues` |

## Release file

| Field | Value |
| --- | --- |
| Upload file | `vault_render_optimization.0.4.1.jar` |
| Display name | `The Vault Render Optimization 0.4.1` |
| Release type | `Release` |
| Game version | `Minecraft 1.18.2` |
| Mod loader | `Forge` |
| Environment | `Client` |
| Changelog format | `Markdown` |
| Changelog | `CHANGELOG-0.4.1.md` |

Upload the runnable JAR itself. Do not upload a sources JAR, a development JAR,
or the documentation bundle in place of the mod.

## Relationship notes

- Vault Hunters, Create, Flywheel, Oculus, Rubidium/Embeddium,
  CodeChickenLib, CTM, Vault Integrations, Powah, Create Crafts & Additions,
  Xaero's World Map, iSpawner, and Vault Loot Beams are optional integrations.
- ModernFix is optional; VRO yields only the exact overlapping enabled feature.
- VRO also yields overlapping paths to Entity Collision FPS Fix,
  BadOptimizations, Particle Core, Flerovium, Better Fps - Render Distance, and
  Dynamic Lights Reforged.

Only mark a CurseForge relationship when the exact project is available in the
relationship selector.

## Integrity

```text
vault_render_optimization.0.4.1.jar
SHA-256: 5AA5A6CE04DD37F1EBC72A097DFA2C3EC03876C794771F606904BF75F56DCD08
Source state: prepared v0.4.1 release commit
```

Prepared release artifact: 506,491 bytes, SHA-256
`5AA5A6CE04DD37F1EBC72A097DFA2C3EC03876C794771F606904BF75F56DCD08`.
The publication build must reproduce this value from the final release commit.

## Update-manifest activation

| Field | Planned value |
| --- | --- |
| Version | `0.4.1` |
| Critical release | Decide at real publication |
| Client-facing message | Decide at real publication |
| Production state before CurseForge verification | Keep `0.4.0` latest/recommended |

Do not add the 0.4.1 message or change either promotion until the exact public
CurseForge download has passed filename, size, metadata, and SHA-256 checks.

## Final moderation check

- Rebuild from the exact release commit and tag.
- Run the required identity scan immediately before every push and upload.
- Confirm the JAR embeds AGPL, credits, third-party notices, ModernFix and
  Flerovium LGPL texts, and the exact published version.
- Synchronize `SUMMARY.txt` and `DESCRIPTION.md` only if their live values
  differ after format-aware comparison.
- Mark the file `Release`, `Client`, `Forge`, and `Minecraft 1.18.2`.
- Verify the CurseForge changelog contains the final GitHub release URL.
- Leave `update.json` at 0.4.0 until the public CurseForge download matches the
  uploaded JAR's filename, size, and SHA-256.
