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

## Browser upload

Use an authenticated browser session:

1. Open the project files page and select **Add File**.
2. Choose the exact non-sources release JAR from the assembled kit or `libs/`.
3. Set the display name to `The Vault Render Optimization X.Y.Z`.
4. Keep automatic publication selected.
5. Select only Forge, Java 17, Minecraft 1.18.2, Client, and Release.
6. Select Markdown for the changelog and use `FILE-CHANGELOG.md` from the kit.
7. Leave related projects unchanged unless the release specifically changes a
   relationship.
8. Review the entire form before submission, then submit once.

## Verification

After submission, return to the files list and verify:

- The expected JAR and display name are present.
- The file has a CurseForge file ID.
- Release type is `Release`.
- Environment is `Client` and does not include `Server`.
- Forge, Java 17, and Minecraft 1.18.2 are listed.
- Processing or moderation has begun.

After the CurseForge file is downloadable, publish and verify the matching
GitHub release. Update `update.json` for the released version, message, and
Minecraft promotions only after both downloads are live, then perform the
required final identity scan and push the manifest last. This prevents clients
from advertising a version that cannot yet be downloaded.

Do not delete, archive, replace, or alter older files unless explicitly
requested. Correct editable metadata on the existing file rather than
uploading a duplicate.
