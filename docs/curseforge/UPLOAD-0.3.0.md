# CurseForge upload sheet: The Vault Render Optimization 0.3.0

## Project fields

| Field | Value |
| --- | --- |
| Project name | `The Vault Render Optimization` |
| Summary | `Improves Vault Hunters client frame consistency by reducing repeated gear, HUD, collision, particle, and renderer work.` |
| Class | `Mods` |
| License | `GNU Affero General Public License v3.0 or later` |
| Description format | `Markdown` |
| Description | `DESCRIPTION.md` |
| Project logo | `vro-icon.jpg` |
| Source | `https://github.com/HoYin1600p/The-Vault-Render-Optimization` |
| Issues | `https://github.com/HoYin1600p/The-Vault-Render-Optimization/issues` |

The optimized JPEG logo is 512 by 512 pixels, is under 100 KB, and is original
AI-assisted project artwork. It exceeds CurseForge's 400 by 400 pixel minimum.

## First public file

| Field | Value |
| --- | --- |
| Upload file | `vault_render_optimization.0.3.0.jar` |
| Display name | `The Vault Render Optimization 0.3.0` |
| Release type | `Release` |
| Game version | `Minecraft 1.18.2` |
| Mod loader | `Forge` |
| Environment | `Client` |
| Changelog format | `Markdown` |
| Changelog | `CHANGELOG-0.3.0.md` |

Upload the runnable jar itself. Do not upload a sources jar or documentation
bundle in place of the mod. The assembled support bundle is for author review.

## Relationship notes

- Vault Hunters is a supported optional integration, not a hard dependency.
- Vault Integrations, Powah, and Create Crafts & Additions are optional.
- Entity Collision FPS Fix, BadOptimizations, Particle Core, and Flerovium are
  supported overlap owners; VRO disables equivalent mixins when present.

Only mark a CurseForge relationship when the exact project is available in the
relationship selector.

## Integrity

Fill this after the final clean compatibility build:

```text
vault_render_optimization.0.3.0.jar
SHA-256: 7EE4F265C9670F0923E95ED4F20010D982AF198580C84DDF3CB2B537B6033AF0
Source state: tag the reviewed release commit as `v0.3.0`
```

## Final moderation check

- `scripts/verify-public-identity.ps1` passes immediately before the push and
  upload.
- The project description states concrete functionality and client-only scope.
- The runnable jar, source repository, AGPL license, credits, and exact adapted
  sources are public.
- The page does not claim a guaranteed FPS percentage.
- The logo is an original square graphic, and the AI-assisted artwork
  disclosure appears at the bottom of the description.
- The file is marked `Release`, matching VH Accelerator's public release type.
- The changelog is concise and the GitHub release notes retain full detail.
- One active jar has been tested in a clean copy of the shipping baseline.
