param(
    [string]$Version = '0.4.1',
    [switch]$Force
)

$ErrorActionPreference = 'Stop'

$repositoryDirectory = Split-Path -Parent $PSScriptRoot
$releaseRoot = Join-Path $repositoryDirectory 'release/curseforge'
$bundleName = "The-Vault-Render-Optimization-$Version"
$bundleDirectory = Join-Path $releaseRoot $bundleName
$zipPath = Join-Path $releaseRoot "$bundleName-CurseForge-Upload-Kit.zip"

$sources = [ordered]@{
    'FILE-CHANGELOG.md' = Join-Path $repositoryDirectory "docs/curseforge/CHANGELOG-$Version.md"
    'PROJECT-SUMMARY.txt' = Join-Path $repositoryDirectory 'docs/curseforge/SUMMARY.txt'
    'PROJECT-DESCRIPTION.md' = Join-Path $repositoryDirectory 'docs/curseforge/DESCRIPTION.md'
    'UPLOAD-CHECKLIST.md' = Join-Path $repositoryDirectory "docs/curseforge/UPLOAD-$Version.md"
    'LICENSE.txt' = Join-Path $repositoryDirectory 'LICENSE'
    'CREDITS.md' = Join-Path $repositoryDirectory 'CREDITS.md'
    'THIRD-PARTY-NOTICES.md' = Join-Path $repositoryDirectory 'THIRD_PARTY_NOTICES.md'
    'SOURCE-PROVENANCE-AUDIT.md' = Join-Path $repositoryDirectory 'docs/SOURCE_PROVENANCE_AUDIT.md'
    'vro-icon.jpg' = Join-Path $repositoryDirectory 'docs/curseforge/vro-icon.jpg'
    "vault_render_optimization.$Version.jar" = Join-Path $repositoryDirectory "libs/vault_render_optimization.$Version.jar"
}

foreach ($source in $sources.Values) {
    if (-not (Test-Path -LiteralPath $source -PathType Leaf)) {
        throw "Required CurseForge release input is missing: $source"
    }
}

$resolvedReleaseRoot = [IO.Path]::GetFullPath($releaseRoot)
$resolvedBundle = [IO.Path]::GetFullPath($bundleDirectory)
$resolvedZip = [IO.Path]::GetFullPath($zipPath)
if (-not $resolvedBundle.StartsWith($resolvedReleaseRoot + [IO.Path]::DirectorySeparatorChar,
        [StringComparison]::OrdinalIgnoreCase)) {
    throw "Unsafe bundle path: $resolvedBundle"
}
if (-not $resolvedZip.StartsWith($resolvedReleaseRoot + [IO.Path]::DirectorySeparatorChar,
        [StringComparison]::OrdinalIgnoreCase)) {
    throw "Unsafe ZIP path: $resolvedZip"
}

if ((Test-Path -LiteralPath $bundleDirectory) -or (Test-Path -LiteralPath $zipPath)) {
    if (-not $Force) {
        throw 'The local CurseForge kit already exists. Re-run with -Force to replace it.'
    }
    if (Test-Path -LiteralPath $bundleDirectory) {
        Remove-Item -LiteralPath $bundleDirectory -Recurse -Force
    }
    if (Test-Path -LiteralPath $zipPath) {
        Remove-Item -LiteralPath $zipPath -Force
    }
}

& (Join-Path $repositoryDirectory 'scripts/verify-public-identity.ps1') -Quiet
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

New-Item -ItemType Directory -Path $bundleDirectory -Force | Out-Null
foreach ($entry in $sources.GetEnumerator()) {
    Copy-Item -LiteralPath $entry.Value -Destination (Join-Path $bundleDirectory $entry.Key)
}
$jarName = "vault_render_optimization.$Version.jar"
$sourceJar = $sources[$jarName]
$sourceJarItem = Get-Item -LiteralPath $sourceJar
$sourceHash = (Get-FileHash -LiteralPath $sourceJar -Algorithm SHA256).Hash
$uploadSheetPath = Join-Path $bundleDirectory 'UPLOAD-CHECKLIST.md'
$uploadSheet = Get-Content -LiteralPath $uploadSheetPath -Raw
if (-not $uploadSheet.Contains('{{JAR_SIZE}}') -or
    -not $uploadSheet.Contains('{{JAR_SHA256}}')) {
    throw 'The tracked upload sheet is missing its JAR integrity placeholders.'
}
$uploadSheet = $uploadSheet.Replace('{{JAR_SIZE}}', [string]$sourceJarItem.Length).
    Replace('{{JAR_SHA256}}', $sourceHash)
Set-Content -LiteralPath $uploadSheetPath -Value $uploadSheet -Encoding utf8
$workflowDestination = Join-Path $releaseRoot 'CURSEFORGE-PUBLISHING-WORKFLOW.md'
Copy-Item -LiteralPath (Join-Path $repositoryDirectory 'docs/curseforge/PUBLISHING-WORKFLOW.md') `
    -Destination $workflowDestination -Force

$privateIdentity = -join @(69, 116, 104, 97, 110 | ForEach-Object { [char]$_ })
foreach ($publicFile in @(
        Get-ChildItem -LiteralPath $bundleDirectory -File -Recurse
    ) + @(Get-Item -LiteralPath $workflowDestination)) {
    if ($publicFile.FullName.IndexOf($privateIdentity,
            [StringComparison]::OrdinalIgnoreCase) -ge 0) {
        throw "Private identity found in release path: $($publicFile.FullName)"
    }
    $bytes = [IO.File]::ReadAllBytes($publicFile.FullName)
    $content = [Text.Encoding]::Latin1.GetString($bytes)
    if ($content.IndexOf($privateIdentity,
            [StringComparison]::OrdinalIgnoreCase) -ge 0) {
        throw "Private identity found in release file: $($publicFile.FullName)"
    }
}

$bundleJar = Join-Path $bundleDirectory $jarName
$bundleHash = (Get-FileHash -LiteralPath $bundleJar -Algorithm SHA256).Hash
if ($sourceHash -ne $bundleHash) {
    throw 'The assembled CurseForge JAR does not match the retained release JAR.'
}

$uploadSheet = Get-Content -LiteralPath $uploadSheetPath -Raw
if (-not $uploadSheet.Contains($sourceHash)) {
    throw 'The upload sheet does not contain the assembled JAR checksum.'
}

$projectSummary = (Get-Content -LiteralPath (Join-Path $bundleDirectory 'PROJECT-SUMMARY.txt') -Raw).Trim()
if ([string]::IsNullOrWhiteSpace($projectSummary) -or $projectSummary.Contains("`n") -or $projectSummary.Contains("`r")) {
    throw 'The CurseForge project summary must be one non-empty line.'
}
if (-not $uploadSheet.Contains($projectSummary)) {
    throw 'The upload sheet does not contain the packaged project summary.'
}

$logo = Get-Item -LiteralPath (Join-Path $bundleDirectory 'vro-icon.jpg')
if ($logo.Length -ge 100000) {
    throw "The CurseForge logo exceeds 100,000 bytes: $($logo.Length)"
}
Add-Type -AssemblyName System.Drawing
$logoImage = [Drawing.Image]::FromFile($logo.FullName)
try {
    if ($logoImage.Width -ne $logoImage.Height -or $logoImage.Width -lt 400) {
        throw "The CurseForge logo must be square and at least 400 pixels: $($logoImage.Width)x$($logoImage.Height)"
    }
    $logoDimensions = "$($logoImage.Width)x$($logoImage.Height)"
}
finally {
    $logoImage.Dispose()
}

Compress-Archive -LiteralPath $bundleDirectory -DestinationPath $zipPath -CompressionLevel Optimal

Write-Host "CurseForge review kit: $bundleDirectory"
Write-Host "CurseForge support ZIP: $zipPath"
Write-Host "Release JAR SHA-256: $sourceHash"
Write-Host "Logo: $logoDimensions, $($logo.Length) bytes"
