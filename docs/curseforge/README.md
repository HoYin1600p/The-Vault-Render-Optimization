# CurseForge release materials

This directory contains the source-of-truth text and artwork for The Vault Render
Optimization CurseForge listing.

- `vro-icon.jpg`: square upload-ready project icon
- `SUMMARY.txt`: one-line project summary shown in CurseForge search and browse results
- `DESCRIPTION.md`: concise project-page Markdown
- `CHANGELOG-X.Y.Z.md`: concise, user-facing file changelog for a release
- `UPLOAD-X.Y.Z.md`: release-specific project fields and review checklist
- `PUBLISHING-WORKFLOW.md`: automated upload and approval-monitor procedure

Locally assembled upload bundles belong under the repository's external Codex
shadow workspace in `artifacts/curseforge/` and must never be committed. The
local Git setting `codex.shadowWorkspace` identifies that workspace. Upload the
verified runnable jar, not a sources jar or support bundle.

Assemble or refresh a local review kit with:

```powershell
.\scripts\assemble-curseforge-release.ps1 -Version X.Y.Z
```

The durable moderation handoff is `docs/release-ledger.json`. Stable, non-secret
publication settings are in `.codex/mod-publish.json`.
