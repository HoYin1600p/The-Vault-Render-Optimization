# Repository instructions

## Public identity safety

- This repository is public. Refer to the project owner only as `HoYin1600p`.
- `hoyin1600p@gmail.com` and
  `4504665+HoYin1600p@users.noreply.github.com` are approved public project
  email addresses. They are not private identifiers or release blockers.
- Never add a private legal name, private email address, home address, or other
  personal identifier to source, documentation, metadata, archives, jars,
  commits, branches, tags, release notes, or generated public artifacts.
- Before every push, run `./scripts/verify-public-identity.ps1`. Any finding is
  release-blocking.
- On a new clone, run `./scripts/install-public-identity-hook.ps1` before other
  release work so the committed pre-push protection is active in that working
  copy.
- The scanner must continue to cover tracked content and paths, binary jar
  payloads, all reachable history, commit identities and messages, and refs.
- Do not bypass or weaken `.githooks/pre-push` or the public identity CI check.
- Do not rewrite already published history without reporting the exact affected
  refs and commits and obtaining explicit approval.

## Release safety

- Source is authoritative. Never retain or publish a jar without committing the
  source and documentation that produced it.
- Run `./scripts/build-pack-compatibility.ps1` before retaining a release jar.
- Run `./scripts/assemble-curseforge-release.ps1 -Version X.Y.Z` before a
  CurseForge upload and review every file in the local kit.
- Keep `LICENSE`, `CREDITS.md`, and `THIRD_PARTY_NOTICES.md` current and embed
  the license and notices in the runnable jar.
- Use `HoYin1600p` in public authorship and attribution fields.
- Do not push, tag, publish, or modify a Prism instance unless the user
  explicitly requests that action.
