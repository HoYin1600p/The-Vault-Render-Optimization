[CmdletBinding()]
param(
    [switch]$Quiet,
    [string]$LogPath,
    [string]$TargetRef = 'HEAD',
    [string[]]$ArtifactPath = @()
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repositoryDirectory = (& git rev-parse --show-toplevel 2>$null).Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($repositoryDirectory)) {
    throw 'This command must run inside a Git repository.'
}
$repositoryDirectory = [IO.Path]::GetFullPath($repositoryDirectory).TrimEnd(
    [IO.Path]::DirectorySeparatorChar,
    [IO.Path]::AltDirectorySeparatorChar
)

$originRemote = (& git -C $repositoryDirectory remote get-url origin 2>$null).Trim()
if ($LASTEXITCODE -ne 0) {
    $originRemote = ''
}
$canonicalRepositoryName = if ([string]::IsNullOrWhiteSpace($originRemote)) {
    Split-Path -Leaf $repositoryDirectory
}
else {
    $remotePath = ($originRemote -replace '\\', '/').TrimEnd('/')
    [IO.Path]::GetFileNameWithoutExtension(($remotePath -split '/')[-1])
}

function Resolve-ScanLogPath {
    if (-not [string]::IsNullOrWhiteSpace($LogPath)) {
        return [IO.Path]::GetFullPath($LogPath)
    }

    $shadowWorkspace = (& git -C $repositoryDirectory config --local --get codex.shadowWorkspace 2>$null)
    if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($shadowWorkspace)) {
        $shadowWorkspace = [IO.Path]::GetFullPath($shadowWorkspace.Trim())
        $markerPath = Join-Path $shadowWorkspace 'workspace-identity.json'
        if (-not (Test-Path -LiteralPath $markerPath -PathType Leaf)) {
            throw 'The configured Codex shadow workspace has no identity marker.'
        }
        $marker = Get-Content -LiteralPath $markerPath -Raw | ConvertFrom-Json
        if ([string]$marker.canonical_repository_name -ne $canonicalRepositoryName -or
            [string]$marker.remote_identity -ne $originRemote) {
            throw 'The configured Codex shadow workspace belongs to another repository.'
        }
        return Join-Path $shadowWorkspace 'identity-scan/identity-scan-log.md'
    }

    if ($env:GITHUB_ACTIONS -eq 'true' -and -not [string]::IsNullOrWhiteSpace($env:RUNNER_TEMP)) {
        return Join-Path $env:RUNNER_TEMP 'identity-scan/identity-scan-log.md'
    }

    throw 'Configure git codex.shadowWorkspace or provide -LogPath before scanning.'
}

$resolvedLogPath = Resolve-ScanLogPath
$logDirectory = Split-Path -Parent $resolvedLogPath
New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
$existingLog = if (Test-Path -LiteralPath $resolvedLogPath -PathType Leaf) {
    Get-Content -LiteralPath $resolvedLogPath -Raw
}
else {
    ''
}

$targetCommit = (& git -C $repositoryDirectory rev-parse "$TargetRef^{commit}").Trim()
if ($LASTEXITCODE -ne 0 -or $targetCommit -notmatch '^[0-9a-f]{40}$') {
    throw "Unable to resolve target ref $TargetRef."
}

function Get-ValidCheckpoint {
    if ([string]::IsNullOrWhiteSpace($existingLog)) {
        return $null
    }
    if (-not [string]::IsNullOrWhiteSpace($originRemote) -and
        $existingLog.IndexOf($originRemote, [StringComparison]::OrdinalIgnoreCase) -lt 0) {
        return $null
    }

    $matches = [regex]::Matches(
        $existingLog,
        '(?im)^-?\s*New scanned frontier:\s*`?([0-9a-f]{40})`?'
    )
    for ($index = $matches.Count - 1; $index -ge 0; $index--) {
        $candidate = $matches[$index].Groups[1].Value.ToLowerInvariant()
        & git -C $repositoryDirectory cat-file -e "$candidate^{commit}" 2>$null
        if ($LASTEXITCODE -ne 0) {
            continue
        }
        & git -C $repositoryDirectory merge-base --is-ancestor $candidate $targetCommit
        if ($LASTEXITCODE -eq 0) {
            return $candidate
        }
    }
    return $null
}

$priorCheckpoint = Get-ValidCheckpoint
$commitRange = if ($null -eq $priorCheckpoint) {
    $targetCommit
}
else {
    "$priorCheckpoint..$targetCommit"
}
$commits = @(& git -C $repositoryDirectory rev-list $commitRange)
if ($LASTEXITCODE -ne 0) {
    throw "Unable to enumerate commit range $commitRange."
}

# Construct the prohibited public identity without storing it in the repository.
$privateIdentity = -join @(69, 116, 104, 97, 110 | ForEach-Object { [char]$_ })
$findings = [System.Collections.Generic.List[string]]::new()
$archiveExtensions = @('.jar', '.zip')
$approvedPublicEmails = [System.Collections.Generic.HashSet[string]]::new(
    [StringComparer]::OrdinalIgnoreCase
)
[void]$approvedPublicEmails.Add('hoyin1600p@gmail.com')
[void]$approvedPublicEmails.Add('4504665+HoYin1600p@users.noreply.github.com')
[void]$approvedPublicEmails.Add('HoYin1600p@users.noreply.github.com')
$artifactFingerprints = [ordered]@{}
$refsExamined = @()

function Redact-Finding([string]$Text) {
    return [regex]::Replace(
        $Text,
        [regex]::Escape($privateIdentity),
        '[PROHIBITED_IDENTITY]',
        [Text.RegularExpressions.RegexOptions]::IgnoreCase
    )
}

function Add-Matches {
    param([string]$Category, [object[]]$Lines)
    foreach ($line in @($Lines)) {
        if (-not [string]::IsNullOrWhiteSpace([string]$line)) {
            $findings.Add((Redact-Finding "$Category`: $line"))
        }
    }
}

function Invoke-GitSearch {
    param(
        [string]$Category,
        [string[]]$Arguments,
        [int[]]$AllowedExitCodes = @(0, 1)
    )
    $output = & git -C $repositoryDirectory @Arguments 2>&1
    $exitCode = $LASTEXITCODE
    if ($exitCode -notin $AllowedExitCodes) {
        throw "Git identity scan failed in $Category with exit code $exitCode."
    }
    if ($exitCode -eq 0) {
        Add-Matches -Category $Category -Lines $output
    }
}

function Test-TextBytes {
    param([string]$Label, [byte[]]$Bytes)
    foreach ($encoding in @(
        [Text.Encoding]::UTF8,
        [Text.Encoding]::Unicode,
        [Text.Encoding]::BigEndianUnicode,
        [Text.Encoding]::Latin1
    )) {
        $text = $encoding.GetString($Bytes)
        if ($text.IndexOf($privateIdentity, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
            $findings.Add("$Label contains the prohibited identity")
            return
        }
    }
}

function Test-ArchiveStream {
    param(
        [string]$Label,
        [IO.Stream]$Stream,
        [int]$Depth = 0
    )
    $archive = [IO.Compression.ZipArchive]::new(
        $Stream,
        [IO.Compression.ZipArchiveMode]::Read,
        $true
    )
    try {
        foreach ($entry in $archive.Entries) {
            if ($entry.FullName.IndexOf($privateIdentity, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
                $findings.Add("archive path $Label contains the prohibited identity")
            }
            $entryStream = $entry.Open()
            try {
                $memory = [IO.MemoryStream]::new()
                try {
                    $entryStream.CopyTo($memory)
                    $bytes = $memory.ToArray()
                    Test-TextBytes -Label "archive content $Label`:$($entry.FullName)" -Bytes $bytes
                    if ($Depth -lt 3 -and
                        $archiveExtensions -contains [IO.Path]::GetExtension($entry.FullName).ToLowerInvariant()) {
                        $nested = [IO.MemoryStream]::new($bytes, $false)
                        try {
                            Test-ArchiveStream -Label "$Label`:$($entry.FullName)" -Stream $nested -Depth ($Depth + 1)
                        }
                        catch [IO.InvalidDataException] {
                            $findings.Add("invalid nested archive $Label`:$($entry.FullName)")
                        }
                        finally {
                            $nested.Dispose()
                        }
                    }
                }
                finally {
                    $memory.Dispose()
                }
            }
            finally {
                $entryStream.Dispose()
            }
        }
    }
    finally {
        $archive.Dispose()
    }
}

function Test-ArchiveFile {
    param([string]$Label, [string]$Path)
    $stream = [IO.File]::OpenRead($Path)
    try {
        Test-ArchiveStream -Label $Label -Stream $stream
    }
    catch [IO.InvalidDataException] {
        $findings.Add("invalid archive $Label")
    }
    finally {
        $stream.Dispose()
    }
}

function Get-GitBlobStream {
    param([string]$ObjectId)
    $startInfo = [Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = 'git'
    $startInfo.Arguments = "cat-file blob $ObjectId"
    $startInfo.WorkingDirectory = $repositoryDirectory
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.CreateNoWindow = $true
    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    if (-not $process.Start()) {
        throw "Unable to read Git object $ObjectId."
    }
    $memory = [IO.MemoryStream]::new()
    $process.StandardOutput.BaseStream.CopyTo($memory)
    $errorOutput = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) {
        $memory.Dispose()
        throw "Unable to read Git object $ObjectId`: $errorOutput"
    }
    $memory.Position = 0
    return $memory
}

function Get-ArtifactLabel([string]$Path) {
    $fullPath = [IO.Path]::GetFullPath($Path)
    if ($fullPath.StartsWith(
            $repositoryDirectory + [IO.Path]::DirectorySeparatorChar,
            [StringComparison]::OrdinalIgnoreCase
        )) {
        return [IO.Path]::GetRelativePath($repositoryDirectory, $fullPath).Replace([IO.Path]::DirectorySeparatorChar, '/')
    }
    $shadowWorkspace = (& git -C $repositoryDirectory config --local --get codex.shadowWorkspace 2>$null)
    if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($shadowWorkspace)) {
        $shadowWorkspace = [IO.Path]::GetFullPath($shadowWorkspace.Trim()).TrimEnd('\\', '/')
        if ($fullPath.StartsWith(
                $shadowWorkspace + [IO.Path]::DirectorySeparatorChar,
                [StringComparison]::OrdinalIgnoreCase
            )) {
            $relative = [IO.Path]::GetRelativePath($shadowWorkspace, $fullPath).Replace([IO.Path]::DirectorySeparatorChar, '/')
            return "shadow-workspace/$relative"
        }
    }
    return [IO.Path]::GetFileName($fullPath)
}

function Test-ExternalArtifact([string]$Path) {
    $fullPath = if ([IO.Path]::IsPathRooted($Path)) {
        [IO.Path]::GetFullPath($Path)
    }
    else {
        [IO.Path]::GetFullPath((Join-Path $repositoryDirectory $Path))
    }
    if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) {
        throw "Artifact does not exist: $Path"
    }
    $label = Get-ArtifactLabel $fullPath
    if ($label.IndexOf($privateIdentity, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
        $findings.Add("artifact path $label contains the prohibited identity")
    }
    $artifactFingerprints[$label] = (Get-FileHash -LiteralPath $fullPath -Algorithm SHA256).Hash
    if ($archiveExtensions -contains [IO.Path]::GetExtension($fullPath).ToLowerInvariant()) {
        Test-ArchiveFile -Label $label -Path $fullPath
    }
    else {
        Test-TextBytes -Label "artifact $label" -Bytes ([IO.File]::ReadAllBytes($fullPath))
    }
}

function Append-ScanLog {
    param([ValidateSet('PASS', 'FAIL', 'INCOMPLETE')][string]$Result, [string]$Detail)
    $timestamp = [DateTime]::UtcNow.ToString('o')
    $priorText = if ($null -eq $priorCheckpoint) { 'none; full baseline required' } else { $priorCheckpoint }
    $rangeText = if ($null -eq $priorCheckpoint) { "full history through $targetCommit" } else { $commitRange }
    $frontierText = if ($Result -eq 'PASS') { $targetCommit } else { 'none' }
    $lines = [Collections.Generic.List[string]]::new()
    $lines.Add('')
    $lines.Add("## $timestamp — $Result")
    $lines.Add('')
    $lines.Add("- Repository: $canonicalRepositoryName")
    $lines.Add("- Public remote: $originRemote")
    $lines.Add("- Prior checkpoint/frontier: $priorText")
    $lines.Add("- Exact commit scope: $rangeText ($($commits.Count) new commit(s))")
    $lines.Add("- Refs examined: $($refsExamined -join ', ')")
    $lines.Add('- Working-tree scope: current tracked paths/content plus untracked non-ignored files')
    if ($artifactFingerprints.Count -gt 0) {
        $lines.Add('- Artifact paths and SHA-256 fingerprints:')
        foreach ($item in $artifactFingerprints.GetEnumerator()) {
            $lines.Add("  - ``$($item.Key)`` — ``$($item.Value)``")
        }
    }
    else {
        $lines.Add('- Artifact scope: no separate non-Git artifacts supplied or discovered')
    }
    $lines.Add("- Result: $Result")
    $lines.Add("- Findings: $Detail")
    $lines.Add("- New scanned frontier: $frontierText")
    $entry = ($lines -join [Environment]::NewLine) + [Environment]::NewLine
    $header = if ([string]::IsNullOrWhiteSpace($existingLog) -and
        (-not (Test-Path -LiteralPath $resolvedLogPath) -or
            (Get-Item -LiteralPath $resolvedLogPath).Length -eq 0)) {
        "# Incremental public-identity scan log$([Environment]::NewLine)$([Environment]::NewLine)" +
        "Repository: $canonicalRepositoryName$([Environment]::NewLine)$([Environment]::NewLine)" +
        "Public remote: $originRemote$([Environment]::NewLine)"
    }
    else {
        ''
    }
    $bytes = [Text.UTF8Encoding]::new($false).GetBytes($header + $entry)
    $stream = [IO.FileStream]::new(
        $resolvedLogPath,
        [IO.FileMode]::OpenOrCreate,
        [IO.FileAccess]::Write,
        [IO.FileShare]::Read
    )
    try {
        $stream.Lock(0, [long]::MaxValue)
        try {
            $null = $stream.Seek(0, [IO.SeekOrigin]::End)
            $stream.Write($bytes, 0, $bytes.Length)
            $stream.Flush($true)
        }
        finally {
            $stream.Unlock(0, [long]::MaxValue)
        }
    }
    finally {
        $stream.Dispose()
    }
}

try {
    Invoke-GitSearch -Category 'current tracked content' -Arguments @(
        'grep', '-a', '-i', '-n', '-e', $privateIdentity, '--', '.'
    )
    $trackedPaths = @(& git -C $repositoryDirectory ls-files)
    if ($LASTEXITCODE -ne 0) {
        throw 'Unable to enumerate tracked paths.'
    }
    Add-Matches -Category 'current tracked path' -Lines @(
        $trackedPaths | Select-String -SimpleMatch $privateIdentity -CaseSensitive:$false
    )

    foreach ($commit in $commits) {
        Invoke-GitSearch -Category "new history content $commit" -Arguments @(
            'grep', '-a', '-i', '-n', '-e', $privateIdentity, $commit, '--'
        )
        $paths = @(& git -C $repositoryDirectory ls-tree -r --name-only $commit)
        if ($LASTEXITCODE -ne 0) {
            throw "Unable to enumerate paths for commit $commit."
        }
        Add-Matches -Category "new history path $commit" -Lines @(
            $paths | Select-String -SimpleMatch $privateIdentity -CaseSensitive:$false
        )

        $metadata = (& git -C $repositoryDirectory show -s --format='%H|%an|%ae|%cn|%ce|%s%n%b' $commit)
        Add-Matches -Category "new commit metadata $commit" -Lines @(
            $metadata | Select-String -SimpleMatch $privateIdentity -CaseSensitive:$false
        )
        $identityFields = ((& git -C $repositoryDirectory show -s --format='%H|%ae|%ce' $commit) -split '\|', 3)
        if ($identityFields.Count -ne 3) {
            throw "Unable to parse commit email metadata for $commit."
        }
        if (-not $approvedPublicEmails.Contains($identityFields[1])) {
            $findings.Add("unapproved author email $commit`: [UNAPPROVED_EMAIL]")
        }
        if (-not $approvedPublicEmails.Contains($identityFields[2])) {
            $findings.Add("unapproved committer email $commit`: [UNAPPROVED_EMAIL]")
        }
    }

    $tagMetadata = @(& git -C $repositoryDirectory for-each-ref refs/tags `
        --format='%(refname)|%(objecttype)|%(taggeremail)')
    if ($LASTEXITCODE -ne 0) {
        throw 'Unable to inspect tag metadata.'
    }
    foreach ($tagLine in $tagMetadata) {
        $tagFields = $tagLine -split '\|', 3
        if ($tagFields.Count -ne 3) {
            throw "Unable to parse tag metadata: $tagLine"
        }
        if ($tagFields[1] -eq 'tag') {
            $taggerEmail = $tagFields[2].Trim('<', '>')
            if (-not $approvedPublicEmails.Contains($taggerEmail)) {
                $findings.Add("unapproved tagger email $($tagFields[0])`: [UNAPPROVED_EMAIL]")
            }
        }
    }

    $refsExamined = @(& git -C $repositoryDirectory for-each-ref --format='%(refname)')
    if ($LASTEXITCODE -ne 0) {
        throw 'Unable to inspect Git refs.'
    }
    Add-Matches -Category 'Git ref' -Lines @(
        $refsExamined | Select-String -SimpleMatch $privateIdentity -CaseSensitive:$false
    )

    foreach ($configValue in @(
        (& git -C $repositoryDirectory config user.name 2>$null),
        (& git -C $repositoryDirectory config user.email 2>$null)
    )) {
        if (-not [string]::IsNullOrWhiteSpace($configValue) -and
            $configValue.IndexOf($privateIdentity, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
            $findings.Add('configured Git identity contains the prohibited identity')
        }
    }
    $configuredEmail = (& git -C $repositoryDirectory config user.email 2>$null)
    if (-not [string]::IsNullOrWhiteSpace($configuredEmail) -and
        -not $approvedPublicEmails.Contains($configuredEmail.Trim())) {
        $findings.Add('configured Git identity uses [UNAPPROVED_EMAIL]')
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $trackedArchives = @($trackedPaths | Where-Object {
        $archiveExtensions -contains [IO.Path]::GetExtension($_).ToLowerInvariant()
    })
    foreach ($relativeArchive in $trackedArchives) {
        $archivePath = Join-Path $repositoryDirectory $relativeArchive
        if (Test-Path -LiteralPath $archivePath -PathType Leaf) {
            Test-ExternalArtifact -Path $archivePath
        }
    }

    $scannedArchiveBlobs = [Collections.Generic.HashSet[string]]::new()
    foreach ($commit in $commits) {
        $treeEntries = @(& git -C $repositoryDirectory ls-tree -r $commit)
        if ($LASTEXITCODE -ne 0) {
            throw "Unable to enumerate archive objects for commit $commit."
        }
        foreach ($treeEntry in $treeEntries) {
            if ($treeEntry -notmatch '^\d+\s+\w+\s+([0-9a-f]+)\t(.+)$') {
                continue
            }
            $objectId = $Matches[1]
            $path = $Matches[2]
            if ($archiveExtensions -notcontains [IO.Path]::GetExtension($path).ToLowerInvariant() -or
                -not $scannedArchiveBlobs.Add($objectId)) {
                continue
            }
            $stream = Get-GitBlobStream -ObjectId $objectId
            try {
                Test-ArchiveStream -Label "$commit`:$path" -Stream $stream
            }
            finally {
                $stream.Dispose()
            }
        }
    }

    $untrackedPaths = @(& git -C $repositoryDirectory ls-files --others --exclude-standard)
    if ($LASTEXITCODE -ne 0) {
        throw 'Unable to enumerate untracked files.'
    }
    foreach ($relativePath in $untrackedPaths) {
        $fullPath = Join-Path $repositoryDirectory $relativePath
        if (Test-Path -LiteralPath $fullPath -PathType Leaf) {
            Test-ExternalArtifact -Path $fullPath
        }
    }
    foreach ($path in $ArtifactPath) {
        Test-ExternalArtifact -Path $path
    }

    if ($findings.Count -gt 0) {
        Append-ScanLog -Result FAIL -Detail "$($findings.Count) release-blocking finding(s); values are redacted"
        [Console]::Error.WriteLine("Public identity verification failed with $($findings.Count) finding(s).")
        $findings | Sort-Object -Unique | ForEach-Object { [Console]::Error.WriteLine($_) }
        exit 1
    }

    Append-ScanLog -Result PASS -Detail 'none; approved public project identity only'
    if (-not $Quiet) {
        Write-Host 'Public identity verification passed and the external scan log was updated.'
        Write-Host "Scanned $($commits.Count) new reachable commit(s), current refs/tree, and $($artifactFingerprints.Count) artifact(s)."
    }
}
catch {
    try {
        Append-ScanLog -Result INCOMPLETE -Detail 'scan execution did not complete; no checkpoint created'
    }
    catch {
        [Console]::Error.WriteLine('Identity scan failed and its INCOMPLETE result could not be logged.')
    }
    throw
}
