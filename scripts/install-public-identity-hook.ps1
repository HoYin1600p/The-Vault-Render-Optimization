$ErrorActionPreference = 'Stop'

$repositoryDirectory = (& git rev-parse --show-toplevel 2>$null).Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($repositoryDirectory)) {
    throw 'This command must run inside a Git repository.'
}

& git -C $repositoryDirectory config core.hooksPath .githooks
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to configure the repository hook path.'
}

& (Join-Path $repositoryDirectory 'scripts/verify-public-identity.ps1')
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host 'Public identity pre-push protection is active for this working copy.'
