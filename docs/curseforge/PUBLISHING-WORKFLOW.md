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
2. Validate the configured external Codex shadow workspace marker, read its
   `identity-scan/identity-scan-log.md`, and run the required incremental
   public-identity scan. Fall back to a full scan when the checkpoint is absent
   or unsafe. Treat any finding as blocking. The scanner atomically appends its
   result to that external log.
3. Run `scripts/verify-public-identity.ps1` as part of that scan and treat any
   match as blocking.
4. Run `scripts/build-pack-compatibility.ps1` against every supported Vault
   baseline.
5. Confirm the normal JAR name, embedded mod version, and SHA-256 checksum.
6. Run `scripts/assemble-curseforge-release.ps1 -Version X.Y.Z` and review the
   resulting local kit under the external shadow workspace's
   `artifacts/curseforge/` directory.
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

## Upload

The project file is submitted through CurseForge's supported Upload API by the
maintainer's private release tooling, which is kept outside this repository.
The author token is never stored in Git, Gradle properties, a command line or
an upload kit. The upload uses the exact production JAR built from the release
tag, Markdown notes, the Release channel, automatic publication, and the
established 1.18.2 Forge client metadata, and it links the full GitHub release.

`update.json` is updated in the release commit, so the in-game update notice
switches to the new version as soon as the release is pushed. A version marked
critical carries the `[CRITICAL] ` prefix on its message.

## Verification

After submission, return to the files list and verify:

- The expected JAR and display name are present.
- The file has a CurseForge file ID.
- Release type is `Release`.
- Environment is `Client` and does not include `Server`.
- Forge, Java 17, and Minecraft 1.18.2 are listed.
- Processing or moderation has begun.

Once the file is public, download it and confirm its filename, size and SHA-256
match the GitHub release asset.

Do not delete, archive, replace, or alter older files unless explicitly
requested. Correct editable metadata on the existing file rather than
uploading a duplicate.
