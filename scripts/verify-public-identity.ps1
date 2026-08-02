param(
    [switch]$Quiet
)

$ErrorActionPreference = 'Stop'

$repositoryDirectory = (& git rev-parse --show-toplevel 2>$null).Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($repositoryDirectory)) {
    throw 'This command must run inside a Git repository.'
}

# Construct the private token without storing it in the public repository.
$privateIdentity = -join @(69, 116, 104, 97, 110 | ForEach-Object { [char]$_ })
$findings = [System.Collections.Generic.List[string]]::new()
$archiveExtensions = @('.jar', '.zip')

function Add-Matches {
    param(
        [string]$Category,
        [object[]]$Lines
    )

    foreach ($line in @($Lines)) {
        if (-not [string]::IsNullOrWhiteSpace([string]$line)) {
            $findings.Add("$Category`: $line")
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
        throw "Git identity scan failed in $Category with exit code $exitCode`: $output"
    }
    if ($exitCode -eq 0) {
        Add-Matches -Category $Category -Lines $output
    }
}

function Test-ArchiveStream {
    param(
        [string]$Label,
        [System.IO.Stream]$Stream
    )

    $archive = [System.IO.Compression.ZipArchive]::new(
        $Stream,
        [System.IO.Compression.ZipArchiveMode]::Read,
        $true
    )
    try {
        foreach ($entry in $archive.Entries) {
            if ($entry.FullName.IndexOf($privateIdentity, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
                $findings.Add("archive path $Label`: $($entry.FullName)")
            }

            $entryStream = $entry.Open()
            try {
                $memory = [System.IO.MemoryStream]::new()
                try {
                    $entryStream.CopyTo($memory)
                    $text = [System.Text.Encoding]::Latin1.GetString($memory.ToArray())
                    if ($text.IndexOf($privateIdentity, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
                        $findings.Add("archive content $Label`: $($entry.FullName)")
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

function Get-GitBlobStream {
    param([string]$ObjectId)

    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = 'git'
    $startInfo.Arguments = "cat-file blob $ObjectId"
    $startInfo.WorkingDirectory = $repositoryDirectory
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.CreateNoWindow = $true

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    if (-not $process.Start()) {
        throw "Unable to read Git object $ObjectId."
    }

    $memory = [System.IO.MemoryStream]::new()
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

Invoke-GitSearch -Category 'current tracked content' -Arguments @(
    'grep', '-a', '-i', '-n', '-e', $privateIdentity, '--', '.'
)

$trackedPaths = & git -C $repositoryDirectory ls-files
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to enumerate tracked paths.'
}
Add-Matches -Category 'current tracked path' -Lines @(
    $trackedPaths | Select-String -SimpleMatch $privateIdentity -CaseSensitive:$false
)

$commits = @(& git -C $repositoryDirectory rev-list --all)
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to enumerate reachable commits.'
}

foreach ($commit in $commits) {
    Invoke-GitSearch -Category "history content $commit" -Arguments @(
        'grep', '-a', '-i', '-n', '-e', $privateIdentity, $commit, '--'
    )

    $paths = & git -C $repositoryDirectory ls-tree -r --name-only $commit
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to enumerate paths for commit $commit."
    }
    Add-Matches -Category "history path $commit" -Lines @(
        $paths | Select-String -SimpleMatch $privateIdentity -CaseSensitive:$false
    )
}

$metadata = & git -C $repositoryDirectory log --all --format='%H|%an|%ae|%cn|%ce|%s%n%b'
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to inspect commit metadata.'
}
Add-Matches -Category 'commit metadata' -Lines @(
    $metadata | Select-String -SimpleMatch $privateIdentity -CaseSensitive:$false
)

$refs = & git -C $repositoryDirectory for-each-ref --format='%(refname)'
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to inspect Git refs.'
}
Add-Matches -Category 'Git ref' -Lines @(
    $refs | Select-String -SimpleMatch $privateIdentity -CaseSensitive:$false
)

Add-Type -AssemblyName System.IO.Compression.FileSystem
$trackedArchives = @($trackedPaths | Where-Object {
    $archiveExtensions -contains [System.IO.Path]::GetExtension($_).ToLowerInvariant()
})
foreach ($relativeArchive in $trackedArchives) {
    $archivePath = Join-Path $repositoryDirectory $relativeArchive
    if (-not (Test-Path -LiteralPath $archivePath)) {
        continue
    }

    $stream = [System.IO.File]::OpenRead($archivePath)
    try {
        Test-ArchiveStream -Label $relativeArchive -Stream $stream
    }
    finally {
        $stream.Dispose()
    }
}

$scannedArchiveBlobs = [System.Collections.Generic.HashSet[string]]::new()
foreach ($commit in $commits) {
    $treeEntries = & git -C $repositoryDirectory ls-tree -r $commit
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to enumerate archive objects for commit $commit."
    }

    foreach ($treeEntry in $treeEntries) {
        if ($treeEntry -notmatch '^\d+\s+\w+\s+([0-9a-f]+)\t(.+)$') {
            continue
        }

        $objectId = $Matches[1]
        $path = $Matches[2]
        if ($archiveExtensions -notcontains [System.IO.Path]::GetExtension($path).ToLowerInvariant()) {
            continue
        }
        if (-not $scannedArchiveBlobs.Add($objectId)) {
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

if ($findings.Count -gt 0) {
    [Console]::Error.WriteLine("Public identity verification failed with $($findings.Count) finding(s).")
    $findings | Sort-Object -Unique | ForEach-Object {
        [Console]::Error.WriteLine($_)
    }
    exit 1
}

if (-not $Quiet) {
    Write-Host 'Public identity verification passed.'
    Write-Host "Scanned $($commits.Count) reachable commit(s), Git metadata, refs, tracked paths/content, $($trackedArchives.Count) current archive(s), and $($scannedArchiveBlobs.Count) unique historical archive(s)."
}
