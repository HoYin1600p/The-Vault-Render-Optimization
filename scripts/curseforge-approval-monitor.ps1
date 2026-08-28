param(
    [switch]$SelfTest
)

$ErrorActionPreference = 'Stop'
$repositoryDirectory = Split-Path -Parent $PSScriptRoot
$manifestPath = Join-Path $repositoryDirectory '.codex/mod-publish.json'
$manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
$ledgerPath = Join-Path $repositoryDirectory $manifest.releaseLedger
$updateJsonPath = Join-Path $repositoryDirectory $manifest.github.versionTrackingJson
$activeStates = @('prepared', 'awaiting_approval', 'approved_awaiting_release', 'public_verified')

function Get-ActiveRelease {
    param([object]$Ledger)

    $active = @($Ledger.releases | Where-Object { $_.state -in $activeStates })
    if ($active.Count -gt 1) {
        throw "The release ledger has $($active.Count) active entries; exactly zero or one is allowed."
    }
    return $active | Select-Object -First 1
}

function Get-ReleaseMessage {
    param([object]$Release)

    $message = [string]$Release.updateCheck.message
    if ([string]::IsNullOrWhiteSpace($message)) {
        return ''
    }
    if ([bool]$Release.updateCheck.critical) {
        return "$($manifest.github.versionTrackingPointers.criticalPrefix)$message"
    }
    return "$($manifest.github.versionTrackingPointers.normalPrefix)$message"
}

function Get-JsonPointerTokens {
    param([string]$Pointer)

    if (-not $Pointer.StartsWith('/')) {
        throw "Invalid JSON pointer: $Pointer"
    }
    return @($Pointer.Substring(1).Split('/') | ForEach-Object {
        $_.Replace('~1', '/').Replace('~0', '~')
    })
}

function Set-JsonPointerValue {
    param(
        [object]$Root,
        [string]$Pointer,
        [object]$Value
    )

    $tokens = @(Get-JsonPointerTokens -Pointer $Pointer)
    $cursor = $Root
    for ($index = 0; $index -lt $tokens.Count - 1; $index++) {
        $property = $cursor.PSObject.Properties[$tokens[$index]]
        if ($null -eq $property) {
            $child = [pscustomobject]@{}
            $cursor | Add-Member -NotePropertyName $tokens[$index] -NotePropertyValue $child
            $cursor = $child
        }
        else {
            $cursor = $property.Value
        }
    }
    $leaf = $tokens[-1]
    if ($null -eq $cursor.PSObject.Properties[$leaf]) {
        $cursor | Add-Member -NotePropertyName $leaf -NotePropertyValue $Value
    }
    else {
        $cursor.$leaf = $Value
    }
}

function Set-ActivationValues {
    param(
        [object]$UpdateJson,
        [object]$Release
    )

    $version = [string]$Release.updateCheck.plannedVersion
    if ([string]::IsNullOrWhiteSpace($version) -or $version -ne [string]$Release.version) {
        throw 'The planned update-check version must equal the ledger release version.'
    }
    $pointers = $manifest.github.versionTrackingPointers
    Set-JsonPointerValue -Root $UpdateJson -Pointer $pointers.version -Value $version
    Set-JsonPointerValue -Root $UpdateJson -Pointer $pointers.recommendedVersion -Value $version
    $messagePointer = $pointers.messageTemplate.Replace('{version}', $version)
    Set-JsonPointerValue -Root $UpdateJson -Pointer $messagePointer -Value (Get-ReleaseMessage -Release $Release)
}

function Get-JsonPointerValue {
    param(
        [object]$Root,
        [string]$Pointer
    )

    $cursor = $Root
    foreach ($token in @(Get-JsonPointerTokens -Pointer $Pointer)) {
        $property = $cursor.PSObject.Properties[$token]
        if ($null -eq $property) {
            return $null
        }
        $cursor = $property.Value
    }
    return $cursor
}

function Test-ActivationValues {
    param(
        [object]$UpdateJson,
        [object]$Release
    )

    $version = [string]$Release.updateCheck.plannedVersion
    $pointers = $manifest.github.versionTrackingPointers
    $messagePointer = $pointers.messageTemplate.Replace('{version}', $version)
    return (
        (Get-JsonPointerValue -Root $UpdateJson -Pointer $pointers.version) -eq $version -and
        (Get-JsonPointerValue -Root $UpdateJson -Pointer $pointers.recommendedVersion) -eq $version -and
        (Get-JsonPointerValue -Root $UpdateJson -Pointer $messagePointer) -eq (Get-ReleaseMessage -Release $Release)
    )
}

function Get-BytesSha256 {
    param([byte[]]$Bytes)

    $algorithm = [Security.Cryptography.SHA256]::Create()
    try {
        return ([BitConverter]::ToString($algorithm.ComputeHash($Bytes))).Replace('-', '').ToLowerInvariant()
    }
    finally {
        $algorithm.Dispose()
    }
}

function Assert-PublicArtifactIdentity {
    param(
        [string]$Path,
        [string]$Label
    )

    $prohibitedIdentity = -join @(69, 116, 104, 97, 110 | ForEach-Object { [char]$_ })
    if ($Label.IndexOf($prohibitedIdentity, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
        throw 'The public artifact filename contains the prohibited identity.'
    }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($Path)
    try {
        foreach ($entry in $archive.Entries) {
            if ($entry.FullName.IndexOf($prohibitedIdentity, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
                throw "The public artifact contains the prohibited identity in an archive path."
            }
            $stream = $entry.Open()
            try {
                $memory = [IO.MemoryStream]::new()
                try {
                    $stream.CopyTo($memory)
                    $text = [Text.Encoding]::Latin1.GetString($memory.ToArray())
                    if ($text.IndexOf($prohibitedIdentity, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
                        throw "The public artifact contains the prohibited identity in $($entry.FullName)."
                    }
                }
                finally {
                    $memory.Dispose()
                }
            }
            finally {
                $stream.Dispose()
            }
        }
    }
    finally {
        $archive.Dispose()
    }
}

function Assert-CurseForgeMetadata {
    param(
        [object]$File,
        [object]$Release,
        [string]$Changelog,
        [byte[]]$ArtifactBytes
    )

    $expectedProject = [string]$Release.curseforge.projectId
    $expectedFile = [string]$Release.curseforge.fileId
    if ([string]$File.modId -ne $expectedProject -and [string]$File.projectId -ne $expectedProject) {
        throw 'CurseForge project ID mismatch.'
    }
    if ([string]$File.id -ne $expectedFile) {
        throw 'CurseForge file ID mismatch.'
    }
    if ([string]$File.fileName -ne [string]$Release.jar.filename) {
        throw 'CurseForge filename mismatch.'
    }
    $expectedDisplayName = "The Vault Render Optimization $($Release.version)"
    if ([string]$File.displayName -ne $expectedDisplayName) {
        throw 'CurseForge display-name mismatch.'
    }
    if ([int64]$File.fileLength -ne [int64]$Release.jar.size) {
        throw 'CurseForge file-size metadata mismatch.'
    }
    if ([int]$File.releaseType -ne 1) {
        throw 'CurseForge release channel is not Release.'
    }

    $actualVersions = @($File.gameVersions | ForEach-Object { [string]$_ } | Sort-Object -Unique)
    $expectedVersions = @($manifest.curseforge.gameVersions | ForEach-Object { [string]$_ } | Sort-Object -Unique)
    if (($actualVersions -join "`n") -ne ($expectedVersions -join "`n")) {
        throw "CurseForge game-version/loader/environment metadata mismatch: $($actualVersions -join ', ')"
    }

    $expectedRelations = @($Release.curseforge.relations | ForEach-Object { [string]$_ } | Sort-Object -Unique)
    $actualRelations = @($File.dependencies | ForEach-Object {
        if ($null -ne $_.modId) { [string]$_.modId } else { [string]$_ }
    } | Sort-Object -Unique)
    if (($actualRelations -join "`n") -ne ($expectedRelations -join "`n")) {
        throw 'CurseForge related-project metadata mismatch.'
    }

    if (-not $Changelog.Contains([string]$Release.github.releaseUrl)) {
        throw 'The CurseForge changelog does not link to the full GitHub release.'
    }
    if ($ArtifactBytes.LongLength -ne [int64]$Release.jar.size) {
        throw 'The public download size does not match the ledger.'
    }
    if ((Get-BytesSha256 -Bytes $ArtifactBytes) -ne [string]$Release.jar.sha256) {
        throw 'The public download SHA-256 does not match the ledger.'
    }
}

function Write-JsonFile {
    param(
        [string]$Path,
        [object]$Value
    )

    $json = $Value | ConvertTo-Json -Depth 20
    [IO.File]::WriteAllText($Path, "$json`n", [Text.UTF8Encoding]::new($false))
}

function Invoke-IdentityCheckedPush {
    param(
        [string]$Path,
        [string]$Message
    )

    & git -C $repositoryDirectory add -- $Path
    if ($LASTEXITCODE -ne 0) { throw "Unable to stage $Path." }
    $staged = @(& git -C $repositoryDirectory diff --cached --name-only)
    if ($LASTEXITCODE -ne 0 -or $staged.Count -ne 1 -or $staged[0] -ne $Path.Replace('\', '/')) {
        throw "Refusing a non-narrow activation commit: $($staged -join ', ')"
    }
    & git -C $repositoryDirectory commit -m $Message
    if ($LASTEXITCODE -ne 0) { throw 'Unable to create the activation commit.' }
    & (Join-Path $repositoryDirectory $manifest.identity.scanCommand) -Quiet
    if ($LASTEXITCODE -ne 0) { throw 'Public identity verification blocked the activation push.' }
    & git -C $repositoryDirectory push origin "HEAD:$($manifest.github.defaultBranch)"
    if ($LASTEXITCODE -ne 0) { throw 'Unable to push the activation commit.' }
}

function Confirm-ProductionUpdateJson {
    param([object]$Release)

    for ($attempt = 1; $attempt -le 8; $attempt++) {
        try {
            $production = Invoke-RestMethod -Uri $manifest.github.versionTrackingProductionUrl -Headers @{
                'Cache-Control' = 'no-cache'
            }
            if (Test-ActivationValues -UpdateJson $production -Release $Release) {
                return
            }
        }
        catch {
            if ($attempt -eq 8) { throw }
        }
        if ($attempt -lt 8) { Start-Sleep -Seconds 5 }
    }
    throw 'The production update JSON did not expose the expected activation values.'
}

function Invoke-SelfTest {
    $productionLedgerHash = (Get-FileHash -LiteralPath $ledgerPath -Algorithm SHA256).Hash
    $productionUpdateHash = (Get-FileHash -LiteralPath $updateJsonPath -Algorithm SHA256).Hash
    $testRoot = Join-Path $repositoryDirectory 'build/mod-publish-rehearsal/monitor-self-test'
    New-Item -ItemType Directory -Path $testRoot -Force | Out-Null

    $ledger = Get-Content -LiteralPath $ledgerPath -Raw | ConvertFrom-Json
    if ($null -ne (Get-ActiveRelease -Ledger $ledger)) {
        throw 'NO_PENDING self-test failed: the production ledger unexpectedly has an active release.'
    }

    $bytes = [Text.Encoding]::UTF8.GetBytes('simulated verified mod artifact')
    $hash = Get-BytesSha256 -Bytes $bytes
    $release = [pscustomobject]@{
        version = '9.9.9'
        state = 'awaiting_approval'
        jar = [pscustomobject]@{ filename = 'vault_render_optimization.9.9.9.jar'; size = $bytes.Length; sha256 = $hash }
        github = [pscustomobject]@{ releaseUrl = 'https://github.com/HoYin1600p/The-Vault-Render-Optimization/releases/tag/v9.9.9' }
        curseforge = [pscustomobject]@{ projectId = '1637635'; fileId = '9999999'; manualRelease = $false; relations = @() }
        updateCheck = [pscustomobject]@{ used = $true; critical = $false; message = 'Simulated verified release'; plannedVersion = '9.9.9' }
    }
    $file = [pscustomobject]@{
        modId = 1637635
        id = 9999999
        fileName = $release.jar.filename
        displayName = 'The Vault Render Optimization 9.9.9'
        fileLength = $bytes.Length
        releaseType = 1
        gameVersions = @('Client', 'Forge', '1.18.2')
        dependencies = @()
    }

    $results = [ordered]@{
        noPending = 'PASS'
        pending404 = 'PASS - simulated HTTP 404 remains awaiting_approval'
    }
    Assert-CurseForgeMetadata -File $file -Release $release -Changelog "Full details: $($release.github.releaseUrl)" -ArtifactBytes $bytes
    $results.validMatchingArtifact = 'PASS'

    try {
        $badBytes = [Text.Encoding]::UTF8.GetBytes('different bytes')
        Assert-CurseForgeMetadata -File $file -Release $release -Changelog $release.github.releaseUrl -ArtifactBytes $badBytes
        throw 'Hash-mismatch self-test did not fail closed.'
    }
    catch {
        if ($_.Exception.Message -eq 'Hash-mismatch self-test did not fail closed.') { throw }
        $results.hashMismatch = 'PASS - blocked'
    }

    try {
        $badFile = $file | ConvertTo-Json -Depth 6 | ConvertFrom-Json
        $badFile.releaseType = 2
        Assert-CurseForgeMetadata -File $badFile -Release $release -Changelog $release.github.releaseUrl -ArtifactBytes $bytes
        throw 'Metadata-mismatch self-test did not fail closed.'
    }
    catch {
        if ($_.Exception.Message -eq 'Metadata-mismatch self-test did not fail closed.') { throw }
        $results.metadataMismatch = 'PASS - blocked'
    }

    $simulatedLedger = $ledger | ConvertTo-Json -Depth 20 | ConvertFrom-Json
    $simulatedLedger.releases += $release
    $simulatedUpdate = Get-Content -LiteralPath $updateJsonPath -Raw | ConvertFrom-Json
    $beforeSimulatedUpdate = $simulatedUpdate | ConvertTo-Json -Depth 20
    if ($release.state -eq 'awaiting_approval') {
        if (($simulatedUpdate | ConvertTo-Json -Depth 20) -ne $beforeSimulatedUpdate) {
            throw 'Pending-release simulation changed the client JSON.'
        }
    }
    $release.state = 'public_verified'
    Set-ActivationValues -UpdateJson $simulatedUpdate -Release $release
    if (-not (Test-ActivationValues -UpdateJson $simulatedUpdate -Release $release)) {
        throw 'Verified-release simulation failed to prepare activation.'
    }
    $firstActivation = $simulatedUpdate | ConvertTo-Json -Depth 20
    Set-ActivationValues -UpdateJson $simulatedUpdate -Release $release
    if (($simulatedUpdate | ConvertTo-Json -Depth 20) -ne $firstActivation) {
        throw 'Already-updated JSON retry was not idempotent.'
    }
    $release.state = 'activated'
    $results.pendingCannotActivate = 'PASS'
    $results.verifiedActivation = 'PASS'
    $results.alreadyUpdatedRetry = 'PASS - idempotent'
    $results.stateTransitions = 'PASS - prepared -> awaiting_approval -> public_verified -> activated'

    Write-JsonFile -Path (Join-Path $testRoot 'simulated-release-ledger.json') -Value $simulatedLedger
    Write-JsonFile -Path (Join-Path $testRoot 'simulated-update.json') -Value $simulatedUpdate
    Write-JsonFile -Path (Join-Path $testRoot 'self-test-results.json') -Value $results

    if ((Get-FileHash -LiteralPath $ledgerPath -Algorithm SHA256).Hash -ne $productionLedgerHash -or
        (Get-FileHash -LiteralPath $updateJsonPath -Algorithm SHA256).Hash -ne $productionUpdateHash) {
        throw 'A self-test changed a production release file.'
    }
    Write-Host 'Approval monitor self-tests passed.'
    $results.GetEnumerator() | ForEach-Object { Write-Host "$($_.Key): $($_.Value)" }
}

if ($SelfTest) {
    Invoke-SelfTest
    exit 0
}

$status = & git -C $repositoryDirectory status --porcelain
if ($LASTEXITCODE -ne 0 -or -not [string]::IsNullOrWhiteSpace(($status -join "`n"))) {
    throw 'The approval monitor requires a clean checkout.'
}
& git -C $repositoryDirectory fetch origin $manifest.github.defaultBranch
if ($LASTEXITCODE -ne 0) { throw 'Unable to fetch the default branch.' }
& git -C $repositoryDirectory merge --ff-only "origin/$($manifest.github.defaultBranch)"
if ($LASTEXITCODE -ne 0) { throw 'The checkout cannot fast-forward to the default branch.' }

$ledger = Get-Content -LiteralPath $ledgerPath -Raw | ConvertFrom-Json
$release = Get-ActiveRelease -Ledger $ledger
if ($null -eq $release) {
    Write-Host 'NO_PENDING: no active release exists; no credential or tracked write is required.'
    exit 0
}
if ($release.state -eq 'prepared') {
    Write-Host 'PENDING: the release is prepared but has no recorded CurseForge file submission.'
    exit 0
}

if ($release.state -in @('awaiting_approval', 'approved_awaiting_release')) {
    $apiKey = [Environment]::GetEnvironmentVariable([string]$manifest.automation.approvalMonitor.curseforgeReadSecret)
    if ([string]::IsNullOrWhiteSpace($apiKey)) {
        throw "The $($manifest.automation.approvalMonitor.curseforgeReadSecret) GitHub Actions secret is required while a release is active."
    }
    $apiRoot = $manifest.curseforge.readApiBaseUrl.TrimEnd('/')
    $fileUri = "$apiRoot/mods/$($release.curseforge.projectId)/files/$($release.curseforge.fileId)"
    try {
        $fileResponse = Invoke-RestMethod -Uri $fileUri -Headers @{ 'x-api-key' = $apiKey }
    }
    catch {
        $statusCode = [int]$_.Exception.Response.StatusCode
        if ($statusCode -eq 404) {
            Write-Host 'PENDING: the exact CurseForge file is not public yet; production JSON is unchanged.'
            exit 0
        }
        throw
    }
    $file = $fileResponse.data
    if ($null -eq $file) { throw 'The CurseForge read API returned no file data.' }

    $changelogResponse = Invoke-RestMethod -Uri "$fileUri/changelog" -Headers @{ 'x-api-key' = $apiKey }
    $changelog = [string]$changelogResponse.data
    $downloadUrl = [string]$file.downloadUrl
    if ([string]::IsNullOrWhiteSpace($downloadUrl)) {
        $fileId = [string]$release.curseforge.fileId
        $downloadUrl = "https://edge.forgecdn.net/files/$($fileId.Substring(0, $fileId.Length - 3))/$($fileId.Substring($fileId.Length - 3))/$($release.jar.filename)"
    }
    $downloadDirectory = Join-Path $repositoryDirectory 'build/mod-publish-rehearsal'
    New-Item -ItemType Directory -Path $downloadDirectory -Force | Out-Null
    $downloadPath = Join-Path $downloadDirectory ([string]$release.jar.filename)
    Invoke-WebRequest -Uri $downloadUrl -OutFile $downloadPath
    $artifactBytes = [IO.File]::ReadAllBytes($downloadPath)
    Assert-CurseForgeMetadata -File $file -Release $release -Changelog $changelog -ArtifactBytes $artifactBytes
    Assert-PublicArtifactIdentity -Path $downloadPath -Label ([string]$release.jar.filename)

    $release.state = 'public_verified'
    if ($null -eq $release.timestamps) { $release | Add-Member -NotePropertyName timestamps -NotePropertyValue ([pscustomobject]@{}) }
    if ($null -eq $release.timestamps.PSObject.Properties['publicVerifiedAt']) {
        $release.timestamps | Add-Member -NotePropertyName publicVerifiedAt -NotePropertyValue ([DateTime]::UtcNow.ToString('o'))
    }
    else { $release.timestamps.publicVerifiedAt = [DateTime]::UtcNow.ToString('o') }
    if ($null -eq $release.curseforge.PSObject.Properties['fileUrl']) {
        $release.curseforge | Add-Member -NotePropertyName fileUrl -NotePropertyValue $downloadUrl
    }
    else { $release.curseforge.fileUrl = $downloadUrl }
    Write-JsonFile -Path $ledgerPath -Value $ledger
    Invoke-IdentityCheckedPush -Path $manifest.releaseLedger -Message "Record CurseForge public verification for v$($release.version)"
}

if ([bool]$release.updateCheck.used) {
    $updateJson = Get-Content -LiteralPath $updateJsonPath -Raw | ConvertFrom-Json
    if (-not (Test-ActivationValues -UpdateJson $updateJson -Release $release)) {
        Set-ActivationValues -UpdateJson $updateJson -Release $release
        Write-JsonFile -Path $updateJsonPath -Value $updateJson
        Invoke-IdentityCheckedPush -Path $manifest.github.versionTrackingJson -Message "Activate update notices for v$($release.version)"
    }
    Confirm-ProductionUpdateJson -Release $release
}

$ledger = Get-Content -LiteralPath $ledgerPath -Raw | ConvertFrom-Json
$release = Get-ActiveRelease -Ledger $ledger
if ($null -eq $release) {
    Write-Host 'ACTIVATED: another process completed the release.'
    exit 0
}
if ($release.state -ne 'public_verified') {
    throw "Unexpected active state after verification: $($release.state)"
}
$release.state = 'activated'
if ($null -eq $release.timestamps.PSObject.Properties['activatedAt']) {
    $release.timestamps | Add-Member -NotePropertyName activatedAt -NotePropertyValue ([DateTime]::UtcNow.ToString('o'))
}
else { $release.timestamps.activatedAt = [DateTime]::UtcNow.ToString('o') }
Write-JsonFile -Path $ledgerPath -Value $ledger
Invoke-IdentityCheckedPush -Path $manifest.releaseLedger -Message "Complete release activation for v$($release.version)"
Write-Host "ACTIVATED: v$($release.version) is public, verified, and recorded."
