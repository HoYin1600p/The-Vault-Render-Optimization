# CurseForge release materials

This directory contains the source-of-truth text and artwork for The Vault Render
Optimization CurseForge listing.

- `vro-icon.jpg`: square upload-ready project icon
- `SUMMARY.txt`: one-line project summary shown in CurseForge search and browse results
- `DESCRIPTION.md`: concise project-page Markdown
- `CHANGELOG-0.3.2.md`: concise current-release file changelog
- `UPLOAD-0.3.2.md`: current project fields and upload checklist
- `PUBLISHING-WORKFLOW.md`: repeatable browser publication procedure

Locally assembled upload bundles belong under `release/curseforge/` and should
not be committed. Upload the verified runnable jar, not a sources jar or support
bundle.

Assemble or refresh the local review kit with:

```powershell
.\scripts\assemble-curseforge-release.ps1 -Version 0.3.2
```
