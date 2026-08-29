param(
    [switch]$SelfTest,
    [switch]$LivePublicCheck
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

function Get-PublicFilePageUrl {
    param([object]$Release)

    $projectSlug = [string]$manifest.curseforge.projectSlug
    $fileId = [string]$Release.curseforge.fileId
    if ([string]::IsNullOrWhiteSpace($projectSlug) -or $projectSlug -notmatch '^[a-z0-9-]+$') {
        throw 'The configured CurseForge project slug is missing or invalid.'
    }
    if ([string]::IsNullOrWhiteSpace($fileId) -or $fileId -notmatch '^\d+$') {
        throw 'The active ledger entry has no valid upload-returned CurseForge file ID.'
    }
    return ([string]$manifest.automation.approvalMonitor.publicFileUrlTemplate).
        Replace('{projectSlug}', $projectSlug).
        Replace('{fileId}', $fileId)
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

function Get-ResponseDisposition {
    param([int]$StatusCode)

    if ($StatusCode -ge 200 -and $StatusCode -lt 300) { return 'SUCCESS' }
    if ($StatusCode -eq 404) { return 'PENDING' }
    if ($StatusCode -in @(403, 429, 500, 502, 503, 504)) { return 'RETRYABLE' }
    return 'ATTENTION_REQUIRED'
}

function Invoke-PublicGet {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Uri,
        [switch]$NotFoundIsPending,
        [int]$MaximumAttempts = 3
    )

    Add-Type -AssemblyName System.Net.Http
    $handler = [Net.Http.HttpClientHandler]::new()
    $handler.AllowAutoRedirect = $true
    $handler.MaxAutomaticRedirections = 8
    $handler.UseCookies = $false
    $handler.AutomaticDecompression = [Net.DecompressionMethods]::GZip -bor [Net.DecompressionMethods]::Deflate
    $client = [Net.Http.HttpClient]::new($handler)
    $client.Timeout = [TimeSpan]::FromSeconds(45)
    $client.DefaultRequestHeaders.UserAgent.ParseAdd('VRO-Approval-Monitor/1.0')
    $client.DefaultRequestHeaders.Accept.ParseAdd('text/html,application/java-archive,application/octet-stream;q=0.9,*/*;q=0.8')
    $client.DefaultRequestHeaders.CacheControl = [Net.Http.Headers.CacheControlHeaderValue]::new()
    $client.DefaultRequestHeaders.CacheControl.NoCache = $true

    try {
        for ($attempt = 1; $attempt -le $MaximumAttempts; $attempt++) {
            $response = $null
            try {
                $response = $client.GetAsync($Uri).GetAwaiter().GetResult()
                $statusCode = [int]$response.StatusCode
                $disposition = Get-ResponseDisposition -StatusCode $statusCode
                if ($disposition -eq 'SUCCESS') {
                    $bytes = $response.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult()
                    return [pscustomobject]@{
                        State = 'SUCCESS'
                        StatusCode = $statusCode
                        Bytes = [byte[]]$bytes
                        FinalUri = [string]$response.RequestMessage.RequestUri.AbsoluteUri
                        ContentType = [string]$response.Content.Headers.ContentType.MediaType
                        ContentDisposition = [string]$response.Content.Headers.ContentDisposition
                    }
                }
                if ($disposition -eq 'PENDING' -and $NotFoundIsPending) {
                    return [pscustomobject]@{
                        State = 'PENDING'
                        StatusCode = $statusCode
                        Bytes = [byte[]]@()
                        FinalUri = $Uri
                        ContentType = ''
                        ContentDisposition = ''
                    }
                }
                if ($disposition -eq 'RETRYABLE' -and $attempt -lt $MaximumAttempts) {
                    Start-Sleep -Seconds $attempt
                    continue
                }
                throw "ATTENTION_REQUIRED: public CurseForge request returned HTTP $statusCode after $attempt attempt(s): $Uri"
            }
            catch {
                if ($_.Exception.Message.StartsWith('ATTENTION_REQUIRED:')) { throw }
                if ($attempt -ge $MaximumAttempts) {
                    throw "ATTENTION_REQUIRED: public CurseForge request failed after $attempt attempt(s): $Uri. $($_.Exception.Message)"
                }
                Start-Sleep -Seconds $attempt
            }
            finally {
                if ($null -ne $response) { $response.Dispose() }
            }
        }
    }
    finally {
        $client.Dispose()
        $handler.Dispose()
    }
}

function ConvertFrom-NextFlightText {
    param([string]$Html)

    $pattern = 'self\.__next_f\.push\(\[1,"(?<payload>(?:\\.|[^"\\])*)"\]\)'
    $segments = [Collections.Generic.List[string]]::new()
    foreach ($match in [regex]::Matches($Html, $pattern)) {
        try {
            $decoded = ConvertFrom-Json -InputObject ('"' + $match.Groups['payload'].Value + '"')
            [void]$segments.Add([string]$decoded)
        }
        catch {
            throw 'ATTENTION_REQUIRED: the public CurseForge page contains unreadable embedded data.'
        }
    }
    if ($segments.Count -eq 0) {
        throw 'ATTENTION_REQUIRED: the public CurseForge page has no recognizable embedded data.'
    }
    return $segments -join "`n"
}

function Get-EmbeddedJsonObject {
    param(
        [string]$Text,
        [string]$PropertyName
    )

    $needle = '"' + $PropertyName + '":'
    $searchIndex = 0
    while ($searchIndex -lt $Text.Length) {
        $propertyIndex = $Text.IndexOf($needle, $searchIndex, [StringComparison]::Ordinal)
        if ($propertyIndex -lt 0) { break }
        $valueIndex = $propertyIndex + $needle.Length
        while ($valueIndex -lt $Text.Length -and [char]::IsWhiteSpace($Text[$valueIndex])) { $valueIndex++ }
        if ($valueIndex -ge $Text.Length -or $Text[$valueIndex] -ne '{') {
            $searchIndex = $propertyIndex + $needle.Length
            continue
        }

        $depth = 0
        $insideString = $false
        $escaped = $false
        for ($index = $valueIndex; $index -lt $Text.Length; $index++) {
            $character = $Text[$index]
            if ($insideString) {
                if ($escaped) { $escaped = $false; continue }
                if ($character -eq [char]92) { $escaped = $true; continue }
                if ($character -eq [char]34) { $insideString = $false }
                continue
            }
            if ($character -eq [char]34) { $insideString = $true; continue }
            if ($character -eq '{') { $depth++ }
            elseif ($character -eq '}') {
                $depth--
                if ($depth -eq 0) {
                    $json = $Text.Substring($valueIndex, $index - $valueIndex + 1)
                    return $json | ConvertFrom-Json
                }
            }
        }
        throw "ATTENTION_REQUIRED: the public CurseForge page has an incomplete $PropertyName record."
    }
    throw "ATTENTION_REQUIRED: the public CurseForge page does not expose its $PropertyName record."
}

function ConvertFrom-PublicFilePage {
    param(
        [string]$Html,
        [object]$Release,
        [string]$ExpectedPageUrl
    )

    $flightText = ConvertFrom-NextFlightText -Html $Html
    $file = Get-EmbeddedJsonObject -Text $flightText -PropertyName 'file'
    $project = Get-EmbeddedJsonObject -Text $flightText -PropertyName 'project'
    $canonicalPattern = '<link\b(?=[^>]*\brel=["'']canonical["''])(?=[^>]*\bhref=["''](?<url>[^"'']+)["''])[^>]*>'
    $canonicalMatch = [regex]::Match($Html, $canonicalPattern, [Text.RegularExpressions.RegexOptions]::IgnoreCase)
    if ($canonicalMatch.Success) {
        $canonicalUrl = [Net.WebUtility]::HtmlDecode($canonicalMatch.Groups['url'].Value)
    }
    else {
        $canonicalMatch = [regex]::Match($flightText, '"canonical":"(?<url>[^"]+)"')
        if (-not $canonicalMatch.Success) {
            throw 'ATTENTION_REQUIRED: the public CurseForge page has no canonical URL.'
        }
        $canonicalUrl = $canonicalMatch.Groups['url'].Value
    }
    if ($canonicalUrl.TrimEnd('/') -ne $ExpectedPageUrl.TrimEnd('/')) {
        throw "ATTENTION_REQUIRED: the public CurseForge page canonical URL does not match the exact ledger file: $canonicalUrl"
    }

    $expectedDownloadPath = "/minecraft/mc-mods/$($manifest.curseforge.projectSlug)/download/$($Release.curseforge.fileId)"
    if ($Html.IndexOf($expectedDownloadPath, [StringComparison]::Ordinal) -lt 0 -and
        $flightText.IndexOf($expectedDownloadPath, [StringComparison]::Ordinal) -lt 0) {
        throw 'ATTENTION_REQUIRED: the exact public CurseForge page has no matching download action.'
    }

    return [pscustomobject]@{
        PageUrl = $ExpectedPageUrl
        CanonicalUrl = $canonicalUrl
        DownloadPageUrl = "https://www.curseforge.com$expectedDownloadPath"
        File = $file
        Project = $project
        SearchableText = "$Html`n$flightText"
    }
}

function Get-PublicDownload {
    param(
        [object]$Page,
        [object]$Release
    )

    $downloadPage = Invoke-PublicGet -Uri $Page.DownloadPageUrl
    if ($downloadPage.ContentType -in @('application/java-archive', 'application/octet-stream')) {
        $download = $downloadPage
    }
    else {
        $downloadPageHtml = [Text.Encoding]::UTF8.GetString($downloadPage.Bytes)
        $expectedEndpoint = "https://www.curseforge.com/api/v1/mods/$($Release.curseforge.projectId)/files/$($Release.curseforge.fileId)/download"
        if ($downloadPageHtml.IndexOf($expectedEndpoint, [StringComparison]::Ordinal) -lt 0) {
            throw 'ATTENTION_REQUIRED: the public download page does not expose the expected exact-file download action.'
        }
        $download = Invoke-PublicGet -Uri $expectedEndpoint
    }

    $finalUri = [Uri]$download.FinalUri
    $fileName = [Uri]::UnescapeDataString([IO.Path]::GetFileName($finalUri.AbsolutePath))
    if ([string]::IsNullOrWhiteSpace($fileName) -and -not [string]::IsNullOrWhiteSpace($download.ContentDisposition)) {
        $fileName = ([string]$download.ContentDisposition -replace '^.*filename\*?=(?:UTF-8''|)"?', '' -replace '".*$', '').Trim()
    }
    return [pscustomobject]@{
        Bytes = [byte[]]$download.Bytes
        FinalUri = $download.FinalUri
        FileName = $fileName
        ContentType = $download.ContentType
    }
}

function Assert-CurseForgePageAndDownload {
    param(
        [object]$Page,
        [object]$Download,
        [object]$Release,
        [switch]$SkipChangelogLink
    )

    if ([string]$Page.Project.id -ne [string]$Release.curseforge.projectId) {
        throw 'ATTENTION_REQUIRED: CurseForge project ID mismatch.'
    }
    if ([string]$Page.Project.slug -ne [string]$manifest.curseforge.projectSlug) {
        throw 'ATTENTION_REQUIRED: CurseForge project slug mismatch.'
    }
    if ([int]$Page.Project.status -ne 4 -or [int]$Page.Project.downloadAvailability -ne 1) {
        throw 'ATTENTION_REQUIRED: the CurseForge project/file does not report public download availability.'
    }
    if ([string]$Page.File.id -ne [string]$Release.curseforge.fileId) {
        throw 'ATTENTION_REQUIRED: CurseForge file ID mismatch.'
    }
    if ([string]$Page.File.fileName -ne [string]$Release.jar.filename) {
        throw 'ATTENTION_REQUIRED: CurseForge filename mismatch.'
    }
    $expectedDisplayName = "The Vault Render Optimization $($Release.version)"
    if ([string]$Page.File.displayName -ne $expectedDisplayName) {
        throw 'ATTENTION_REQUIRED: CurseForge display-name mismatch.'
    }
    if ([int64]$Page.File.fileLength -ne [int64]$Release.jar.size) {
        throw 'ATTENTION_REQUIRED: CurseForge file-size metadata mismatch.'
    }
    if ([int]$Page.File.releaseType -ne 1) {
        throw 'ATTENTION_REQUIRED: CurseForge release channel is not Release.'
    }

    $actualVersions = [Collections.Generic.List[string]]::new()
    foreach ($value in @($Page.File.gameVersions)) { [void]$actualVersions.Add([string]$value) }
    foreach ($flavor in @($Page.File.flavors)) { [void]$actualVersions.Add([string]$flavor.name) }
    if ([bool]$Page.File.isClientCompatible) { [void]$actualVersions.Add('Client') }
    $actualVersionSet = @($actualVersions | Sort-Object -Unique)
    $expectedVersionSet = @($manifest.curseforge.gameVersions | ForEach-Object { [string]$_ } | Sort-Object -Unique)
    if (($actualVersionSet -join "`n") -ne ($expectedVersionSet -join "`n")) {
        throw "ATTENTION_REQUIRED: CurseForge game-version/loader/environment metadata mismatch: $($actualVersionSet -join ', ')"
    }

    $expectedRelations = @($Release.curseforge.relations | ForEach-Object { [string]$_ } | Sort-Object -Unique)
    $actualRelations = @($Page.File.relatedProjects | ForEach-Object {
        if ($null -ne $_.id) { [string]$_.id }
        elseif ($null -ne $_.projectId) { [string]$_.projectId }
        else { [string]$_ }
    } | Sort-Object -Unique)
    if (($actualRelations -join "`n") -ne ($expectedRelations -join "`n")) {
        throw 'ATTENTION_REQUIRED: CurseForge related-project metadata mismatch.'
    }

    if (-not $SkipChangelogLink -and
        $Page.SearchableText.IndexOf([string]$Release.github.releaseUrl, [StringComparison]::OrdinalIgnoreCase) -lt 0) {
        throw 'ATTENTION_REQUIRED: the CurseForge changelog does not link to the full GitHub release.'
    }
    if ([string]$Download.FileName -ne [string]$Release.jar.filename) {
        throw "ATTENTION_REQUIRED: the public download filename does not match the ledger: $($Download.FileName)"
    }
    if ($Download.Bytes.LongLength -ne [int64]$Release.jar.size) {
        throw 'ATTENTION_REQUIRED: the public download size does not match the ledger.'
    }
    if ((Get-BytesSha256 -Bytes $Download.Bytes) -ne [string]$Release.jar.sha256) {
        throw 'ATTENTION_REQUIRED: the public download SHA-256 does not match the ledger.'
    }
}

function Assert-PublicArtifactIdentity {
    param(
        [byte[]]$Bytes,
        [string]$Label
    )

    $prohibitedIdentity = -join @(69, 116, 104, 97, 110 | ForEach-Object { [char]$_ })
    if ($Label.IndexOf($prohibitedIdentity, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
        throw 'ATTENTION_REQUIRED: the public artifact filename contains the prohibited identity.'
    }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $memory = [IO.MemoryStream]::new($Bytes, $false)
    $archive = [IO.Compression.ZipArchive]::new($memory, [IO.Compression.ZipArchiveMode]::Read, $false)
    try {
        foreach ($entry in $archive.Entries) {
            if ($entry.FullName.IndexOf($prohibitedIdentity, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
                throw 'ATTENTION_REQUIRED: the public artifact contains the prohibited identity in an archive path.'
            }
            $entryStream = $entry.Open()
            try {
                $entryMemory = [IO.MemoryStream]::new()
                try {
                    $entryStream.CopyTo($entryMemory)
                    $text = [Text.Encoding]::Latin1.GetString($entryMemory.ToArray())
                    if ($text.IndexOf($prohibitedIdentity, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
                        throw "ATTENTION_REQUIRED: the public artifact contains the prohibited identity in $($entry.FullName)."
                    }
                }
                finally { $entryMemory.Dispose() }
            }
            finally { $entryStream.Dispose() }
        }
    }
    finally {
        $archive.Dispose()
        $memory.Dispose()
    }
}

function Invoke-PublicFileVerification {
    param(
        [object]$Release,
        [switch]$SkipChangelogLink
    )

    $pageUrl = Get-PublicFilePageUrl -Release $Release
    $pageResponse = Invoke-PublicGet -Uri $pageUrl -NotFoundIsPending
    if ($pageResponse.State -eq 'PENDING') {
        return [pscustomobject]@{ State = 'PENDING'; PageUrl = $pageUrl }
    }
    if ($pageResponse.ContentType -ne 'text/html') {
        throw "ATTENTION_REQUIRED: the exact CurseForge file page returned unexpected content type $($pageResponse.ContentType)."
    }
    $html = [Text.Encoding]::UTF8.GetString($pageResponse.Bytes)
    $page = ConvertFrom-PublicFilePage -Html $html -Release $Release -ExpectedPageUrl $pageUrl
    $download = Get-PublicDownload -Page $page -Release $Release
    Assert-CurseForgePageAndDownload -Page $page -Download $download -Release $Release -SkipChangelogLink:$SkipChangelogLink
    Assert-PublicArtifactIdentity -Bytes $download.Bytes -Label ([string]$Release.jar.filename)
    return [pscustomobject]@{
        State = 'PUBLIC_VERIFIED'
        PageUrl = $pageUrl
        DownloadUrl = $download.FinalUri
        Sha256 = Get-BytesSha256 -Bytes $download.Bytes
        Page = $page
        Download = $download
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
            if (Test-ActivationValues -UpdateJson $production -Release $Release) { return }
        }
        catch {
            if ($attempt -eq 8) { throw }
        }
        if ($attempt -lt 8) { Start-Sleep -Seconds 5 }
    }
    throw 'ATTENTION_REQUIRED: the production update JSON did not expose the expected activation values.'
}

function Get-SimulatedSequenceOutcome {
    param([int[]]$StatusCodes)

    foreach ($statusCode in $StatusCodes) {
        $disposition = Get-ResponseDisposition -StatusCode $statusCode
        if ($disposition -eq 'RETRYABLE') { continue }
        return $disposition
    }
    return 'ATTENTION_REQUIRED'
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
    $results = [ordered]@{
        noPending = 'PASS'
        pending404 = if ((Get-ResponseDisposition -StatusCode 404) -eq 'PENDING') { 'PASS' } else { throw '404 was not pending.' }
        temporary403 = if ((Get-SimulatedSequenceOutcome -StatusCodes @(403, 200)) -eq 'SUCCESS') { 'PASS - retried' } else { throw '403 retry failed.' }
        temporary429 = if ((Get-SimulatedSequenceOutcome -StatusCodes @(429, 200)) -eq 'SUCCESS') { 'PASS - retried' } else { throw '429 retry failed.' }
        temporary5xx = if ((Get-SimulatedSequenceOutcome -StatusCodes @(503, 200)) -eq 'SUCCESS') { 'PASS - retried' } else { throw '5xx retry failed.' }
    }

    $bytes = [Text.Encoding]::UTF8.GetBytes('simulated verified mod artifact')
    $hash = Get-BytesSha256 -Bytes $bytes
    $release = [pscustomobject]@{
        version = '9.9.9'
        state = 'prepared'
        jar = [pscustomobject]@{ filename = 'vault_render_optimization.9.9.9.jar'; size = $bytes.Length; sha256 = $hash }
        github = [pscustomobject]@{ releaseUrl = 'https://github.com/HoYin1600p/The-Vault-Render-Optimization/releases/tag/v9.9.9' }
        curseforge = [pscustomobject]@{ projectId = '1637635'; fileId = '9999999'; manualRelease = $false; relations = @() }
        updateCheck = [pscustomobject]@{ used = $true; critical = $false; message = 'Simulated verified release'; plannedVersion = '9.9.9' }
    }
    $file = [pscustomobject]@{
        id = 9999999
        fileName = $release.jar.filename
        displayName = 'The Vault Render Optimization 9.9.9'
        fileLength = $bytes.Length
        releaseType = 1
        gameVersions = @('1.18.2')
        flavors = @([pscustomobject]@{ id = 1; name = 'Forge' })
        relatedProjects = @()
        isClientCompatible = $true
    }
    $project = [pscustomobject]@{
        id = 1637635
        slug = 'vault-render-optimization'
        status = 4
        downloadAvailability = 1
    }
    $pageUrl = Get-PublicFilePageUrl -Release $release
    $flight = '23:{"file":' + ($file | ConvertTo-Json -Depth 8 -Compress) +
        ',"project":' + ($project | ConvertTo-Json -Depth 8 -Compress) + '}'
    $encodedFlight = ConvertTo-Json -InputObject $flight -Compress
    $downloadPath = "/minecraft/mc-mods/vault-render-optimization/download/$($release.curseforge.fileId)"
    $fixtureHtml = "<html><head><link rel=`"canonical`" href=`"$pageUrl`"><script>self.__next_f.push([1,$encodedFlight])</script></head><body><a href=`"$downloadPath`">Download</a><a href=`"$($release.github.releaseUrl)`">Full changelog</a></body></html>"
    $page = ConvertFrom-PublicFilePage -Html $fixtureHtml -Release $release -ExpectedPageUrl $pageUrl
    $download = [pscustomobject]@{
        Bytes = $bytes
        FinalUri = "https://mediafilez.forgecdn.net/files/9999/999/$($release.jar.filename)"
        FileName = $release.jar.filename
        ContentType = 'application/java-archive'
    }
    $release.state = 'awaiting_approval'
    Assert-CurseForgePageAndDownload -Page $page -Download $download -Release $release
    $results.validPublicPageAndArtifact = 'PASS'

    try {
        $badDownload = $download | ConvertTo-Json -Depth 6 | ConvertFrom-Json
        $badDownload.Bytes = [Text.Encoding]::UTF8.GetBytes('different bytes')
        Assert-CurseForgePageAndDownload -Page $page -Download $badDownload -Release $release
        throw 'Hash-mismatch self-test did not fail closed.'
    }
    catch {
        if ($_.Exception.Message -eq 'Hash-mismatch self-test did not fail closed.') { throw }
        $results.hashMismatch = 'PASS - ATTENTION_REQUIRED'
    }

    try {
        $badPage = $page | ConvertTo-Json -Depth 12 | ConvertFrom-Json
        $badPage.File.releaseType = 2
        Assert-CurseForgePageAndDownload -Page $badPage -Download $download -Release $release
        throw 'Metadata-mismatch self-test did not fail closed.'
    }
    catch {
        if ($_.Exception.Message -eq 'Metadata-mismatch self-test did not fail closed.') { throw }
        $results.metadataMismatch = 'PASS - ATTENTION_REQUIRED'
    }

    try {
        ConvertFrom-PublicFilePage -Html '<html><body>changed page</body></html>' -Release $release -ExpectedPageUrl $pageUrl | Out-Null
        throw 'Page-format drift self-test did not fail closed.'
    }
    catch {
        if ($_.Exception.Message -eq 'Page-format drift self-test did not fail closed.') { throw }
        $results.pageFormatDrift = 'PASS - ATTENTION_REQUIRED'
    }

    $simulatedLedger = $ledger | ConvertTo-Json -Depth 20 | ConvertFrom-Json
    $simulatedLedger.releases += $release
    $simulatedUpdate = Get-Content -LiteralPath $updateJsonPath -Raw | ConvertFrom-Json
    $beforeActivation = $simulatedUpdate | ConvertTo-Json -Depth 20
    if (($simulatedUpdate | ConvertTo-Json -Depth 20) -ne $beforeActivation) {
        throw 'Pending-release simulation changed the client JSON.'
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

function Invoke-LivePublicCheck {
    $ledger = Get-Content -LiteralPath $ledgerPath -Raw | ConvertFrom-Json
    $knownRelease = $ledger.releases |
        Where-Object { $_.state -eq 'activated' -and $null -ne $_.curseforge.fileId } |
        Select-Object -Last 1
    if ($null -eq $knownRelease) {
        throw 'ATTENTION_REQUIRED: the ledger has no known public CurseForge file for a live route check.'
    }
    $verification = Invoke-PublicFileVerification -Release $knownRelease -SkipChangelogLink
    if ($verification.State -ne 'PUBLIC_VERIFIED') {
        throw 'ATTENTION_REQUIRED: the known public CurseForge file did not verify.'
    }
    Write-Host "PUBLIC_VERIFIED: $($verification.PageUrl)"
    Write-Host "Public download: $($verification.DownloadUrl)"
    Write-Host "SHA-256: $($verification.Sha256)"
}

if ($SelfTest) {
    Invoke-SelfTest
    exit 0
}
if ($LivePublicCheck) {
    Invoke-LivePublicCheck
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
    Write-Host 'NO_PENDING: no active release exists; no tracked write is required.'
    exit 0
}
if ($release.state -eq 'prepared') {
    Write-Host 'PENDING: the release is prepared but has no recorded CurseForge file submission.'
    exit 0
}

if ($release.state -in @('awaiting_approval', 'approved_awaiting_release')) {
    $verification = Invoke-PublicFileVerification -Release $release
    if ($verification.State -eq 'PENDING') {
        Write-Host 'PENDING: the exact public CurseForge file page is not available; production JSON is unchanged.'
        exit 0
    }
    Write-Host "PUBLIC_VERIFIED: $($verification.PageUrl)"
    $release.state = 'public_verified'
    if ($null -eq $release.timestamps) {
        $release | Add-Member -NotePropertyName timestamps -NotePropertyValue ([pscustomobject]@{})
    }
    if ($null -eq $release.timestamps.PSObject.Properties['publicVerifiedAt']) {
        $release.timestamps | Add-Member -NotePropertyName publicVerifiedAt -NotePropertyValue ([DateTime]::UtcNow.ToString('o'))
    }
    else { $release.timestamps.publicVerifiedAt = [DateTime]::UtcNow.ToString('o') }
    if ($null -eq $release.curseforge.PSObject.Properties['fileUrl']) {
        $release.curseforge | Add-Member -NotePropertyName fileUrl -NotePropertyValue $verification.PageUrl
    }
    else { $release.curseforge.fileUrl = $verification.PageUrl }
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
    throw "ATTENTION_REQUIRED: unexpected active state after verification: $($release.state)"
}
$release.state = 'activated'
if ($null -eq $release.timestamps.PSObject.Properties['activatedAt']) {
    $release.timestamps | Add-Member -NotePropertyName activatedAt -NotePropertyValue ([DateTime]::UtcNow.ToString('o'))
}
else { $release.timestamps.activatedAt = [DateTime]::UtcNow.ToString('o') }
Write-JsonFile -Path $ledgerPath -Value $ledger
Invoke-IdentityCheckedPush -Path $manifest.releaseLedger -Message "Complete release activation for v$($release.version)"
Write-Host "ACTIVATED: v$($release.version) is public, verified, and recorded."
