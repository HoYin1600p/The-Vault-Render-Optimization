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

1. Confirm the intended version has been built, tested, committed, tagged,
   pushed, and released on GitHub.
2. Run `scripts/verify-public-identity.ps1` and treat any match as blocking.
3. Run `scripts/build-pack-compatibility.ps1` against every supported Vault
   baseline.
4. Confirm the normal JAR name, embedded mod version, and SHA-256 checksum.
5. Run `scripts/assemble-curseforge-release.ps1 -Version X.Y.Z` and review the
   resulting local kit under `release/curseforge/`.
6. Confirm the CurseForge project Summary exactly matches `PROJECT-SUMMARY.txt`.
7. Keep the CurseForge changelog concise and user-visible. Do not substitute
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

Do not delete, archive, replace, or alter older files unless explicitly
requested. Correct editable metadata on the existing file rather than
uploading a duplicate.
