# CurseForge upload sheet: The Vault Render Optimization 0.3.2

## Project fields

| Field | Value |
| --- | --- |
| Project name | `The Vault Render Optimization` |
| Summary | `Improves Vault Hunters frame consistency and memory use with render caching, terrain culling, dynamic-light controls, and client stability fixes.` |
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
| Upload file | `vault_render_optimization.0.3.2.jar` |
| Display name | `The Vault Render Optimization 0.3.2` |
| Release type | `Release` |
| Game version | `Minecraft 1.18.2` |
| Mod loader | `Forge` |
| Environment | `Client` |
| Changelog format | `Markdown` |
| Changelog | `CHANGELOG-0.3.2.md` |

Upload the runnable JAR itself. Do not upload a sources JAR or the documentation
bundle in place of the mod.

## Relationship notes

- Vault Hunters is a supported optional integration, not a hard dependency.
- Vault Integrations, Powah, Create Crafts & Additions, Embeddium/Rubidium, and
  Oculus are optional integrations.
- VRO yields overlapping paths to Entity Collision FPS Fix, BadOptimizations,
  Particle Core, Flerovium, Better Fps - Render Distance, and Dynamic Lights
  Reforged when present.

Only mark a CurseForge relationship when the exact project is available in the
relationship selector.

## Integrity

```text
vault_render_optimization.0.3.2.jar
SHA-256: 48155297C0EB31E1491A35901543F78AD400EF425849A5C181E3B44F80637C3E
Source state: tag the reviewed release commit as `v0.3.2`
```

## Final moderation check

- `scripts/verify-public-identity.ps1` passes immediately before push and upload.
- The runnable JAR, complete corresponding source, AGPL license, credits, and
  exact adapted-source notices are public.
- `THIRD_PARTY_NOTICES.md` embedded in the JAR matches the release source.
- The project page uses `PROJECT-SUMMARY.txt` and `PROJECT-DESCRIPTION.md` from
  the assembled kit.
- The page does not claim a guaranteed FPS percentage.
- The file is marked `Release`, `Client`, `Forge`, and `Minecraft 1.18.2`.
- One active JAR has been tested in a clean copy of the shipping baseline.
