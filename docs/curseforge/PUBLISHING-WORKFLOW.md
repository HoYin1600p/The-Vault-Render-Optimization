# VRO CurseForge publishing workflow

This is the repeatable procedure for publishing The Vault Render Optimization
updates to CurseForge.

## Project

- Project ID: `1637635`
- Project summary: paste the complete contents of `PROJECT-SUMMARY.txt` from the assembled kit
- Upload only the normal release JAR from `libs/`.
- Never upload a sources JAR, development JAR, upload kit, or ZIP as the main
  project file.

## Release readiness

Before opening CurseForge:

1. Confirm the intended version's source and documentation are committed and
   the release candidate has been built and tested. Do not publish the GitHub
   release or update the live manifest yet.
2. Read `.identity-scan/identity-scan-log.md`, validate its newest successful
   checkpoint, and run the required incremental public-identity scan. Fall
   back to a full scan when the checkpoint is absent or unsafe. Treat any
   finding as blocking and append the result to the local ignored log.
3. Run `scripts/verify-public-identity.ps1` as part of that scan and treat any
   match as blocking.
4. Run `scripts/build-pack-compatibility.ps1` against every supported Vault
   baseline.
5. Confirm the normal JAR name, embedded mod version, and SHA-256 checksum.
6. Run `scripts/assemble-curseforge-release.ps1 -Version X.Y.Z` and review the
   resulting local kit under `release/curseforge/`.
7. Confirm the CurseForge project Summary exactly matches `PROJECT-SUMMARY.txt`.
8. Keep the CurseForge changelog concise and user-visible. Do not substitute
   the full GitHub release notes.

## Supported file metadata

- Environment: **Client only**
- Mod loader: **Forge**
- Java: **Java 17**
- Minecraft: **1.18.2**
- Release type: **Release**
- Publication: **Publish automatically once approved**

Do not select the Server environment or advertise server support. The remote
server does not need VRO.

## Automated upload

The standard uploader uses CurseForge's supported Upload API. Keep the author
token only in the `CURSEFORGE_API_TOKEN` process environment; never place it in
Git, Gradle properties, a command line, an upload kit, or the release ledger.

First rehearse the exact artifact and metadata path without making a remote
request:

```powershell
.\scripts\publish-curseforge.ps1 `
  -Version X.Y.Z `
  -ChangelogFile docs/curseforge/CHANGELOG-X.Y.Z.md `
  -GitHubReleaseUrl https://github.com/HoYin1600p/The-Vault-Render-Optimization/releases/tag/vX.Y.Z `
  -DryRun
```

Review `build/mod-publish-rehearsal/curseforge-upload-dry-run.json`, then remove
`-DryRun` to submit once. The real path refuses a version that differs from
`gradle.properties`, selects only the exact production JAR under `build/libs`,
uses Markdown, Release, automatic publication, and the established 1.18.2
Forge metadata, and appends the full GitHub release link when needed.

After submission, record the returned file ID and verified artifact metadata in
`docs/release-ledger.json` as `awaiting_approval`. Dispatch **CurseForge approval
monitor** immediately; its scheduled checks continue every 15 minutes.

## Verification

After submission, return to the files list and verify:

- The expected JAR and display name are present.
- The file has a CurseForge file ID.
- Release type is `Release`.
- Environment is `Client` and does not include `Server`.
- Forge, Java 17, and Minecraft 1.18.2 are listed.
- Processing or moderation has begun.

The approval monitor queries the exact file ID through the official read API,
then verifies its public unauthenticated bytes, hash, filename, size, channel,
compatibility, relations, display name, and full GitHub changelog link. A 404 or
pending file cannot change `update.json`. Only a fully verified file advances
to `public_verified`; the monitor then activates the existing Forge JSON fields,
reads them back through the production raw URL, and finally records `activated`.
Every public push is narrow and identity-scanned. Repeated runs are safe.

The workflow needs the CurseForge for Studios read key in the repository secret
`CURSEFORGE_API_KEY`. The secret is not needed when the ledger has no active
release, so a no-pending manual dispatch remains a non-writing health check.

Do not delete, archive, replace, or alter older files unless explicitly
requested. Correct editable metadata on the existing file rather than
uploading a duplicate.
