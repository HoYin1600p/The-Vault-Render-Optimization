param(
    [Parameter(Mandatory = $true)][string]$ForgeMappedJar,
    [Parameter(Mandatory = $true)][string[]]$RendererJars
)

$ErrorActionPreference = 'Stop'

function Read-MethodBytecode {
    param([string]$Jar, [string]$ClassName, [string]$Method)
    if (-not (Test-Path -LiteralPath $Jar -PathType Leaf)) { throw "Missing input JAR: $Jar" }
    $lines = @(& javap -classpath $Jar -c -p $ClassName 2>&1)
    if ($LASTEXITCODE -ne 0) { throw "javap failed for $ClassName" }
    $start = -1
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ([string]$lines[$i] -match ('^  (private|public|protected).*\b' + [regex]::Escape($Method) + '\(')) {
            if ($start -ne -1) { throw "Ambiguous method: $ClassName.$Method" }
            $start = $i
        }
    }
    if ($start -lt 0) { throw "Missing method: $ClassName.$Method" }
    $end = $start + 1
    while ($end -lt $lines.Count -and [string]$lines[$end] -notmatch '^  (private|public|protected|static)') { $end++ }
    return ($lines[$start..($end - 1)] -join "`n")
}

$vanilla = Read-MethodBytecode $ForgeMappedJar 'net.minecraft.client.renderer.LevelRenderer' 'compileChunks'
if ([regex]::Matches($vanilla, 'getfield\s+.*Options\.prioritizeChunkUpdates:').Count -ne 2) {
    throw 'Vanilla compileChunks must have exactly two priority preference reads.'
}
foreach ($call in @('rebuildChunkSync:', 'rebuildChunkAsync:', 'setNotDirty:', 'uploadAllPendingUploads:')) {
    if (-not $vanilla.Contains($call)) { throw "Missing vanilla scheduling operation: $call" }
}
Write-Output 'PASS: vanilla priority reads and native sync/async/dirty/upload paths.'

foreach ($jar in $RendererJars) {
    $method = Read-MethodBytecode $jar 'me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager' 'submitRebuildTasks'
    $decisionMethod = $method
    if ([IO.Path]::GetFileName($jar) -match '^rubidium-0\.5\.6') {
        $decisionMethod = Read-MethodBytecode $jar 'me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager' 'scheduleRebuild'
    }
    if ([regex]::Matches($decisionMethod, 'getfield\s+.*alwaysDeferChunkUpdates:Z').Count -ne 1) {
        throw "Expected exactly one native deferral decision in $jar"
    }
    foreach ($call in @('isImportant:', 'schedule:', 'scheduleDeferred:')) {
        if (-not $method.Contains($call)) { throw "Missing native scheduling branch $call in $jar" }
    }
    Write-Output ("PASS: native deferral decision and both task branches in " + [IO.Path]::GetFileName($jar))
}
