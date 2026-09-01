param(
    [Parameter(Mandatory = $true)]
    [string]$EmbeddiumJar,
    [Parameter(Mandatory = $true)]
    [string]$RubidiumJar
)

$ErrorActionPreference = 'Stop'

function Read-ClassLayout {
    param([string]$Jar, [string]$ClassName)
    $text = & javap -classpath $Jar -p -s $ClassName 2>&1 | Out-String
    if ($LASTEXITCODE -ne 0) {
        throw "javap failed for $ClassName in $Jar"
    }
    return $text
}

function Require-Layout {
    param([string]$Text, [string]$Pattern, [string]$Description)
    if ($Text -notmatch $Pattern) {
        throw "Missing validated renderer layout: $Description"
    }
}

foreach ($jar in @($EmbeddiumJar, $RubidiumJar)) {
    if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) {
        throw "Renderer jar does not exist: $jar"
    }

    $occlusion = Read-ClassLayout $jar 'me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache'
    Require-Layout $occlusion 'shouldDrawSide\(' 'BlockOcclusionCache.shouldDrawSide'

    $vertex = Read-ClassLayout $jar 'me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferBuilder'
    Require-Layout $vertex 'private void grow\(int\)' 'VertexBufferBuilder.grow'
    Require-Layout $vertex 'public void start\(\)' 'VertexBufferBuilder.start'

    $arena = Read-ClassLayout $jar 'me.jellysquid.mods.sodium.client.gl.arena.AsyncBufferArena'
    Require-Layout $arena 'ensureCapacity\(' 'AsyncBufferArena.ensureCapacity'

    $fluid = Read-ClassLayout $jar 'me.jellysquid.mods.sodium.client.render.pipeline.FluidRenderer'
    Require-Layout $fluid 'boolean render\(' 'FluidRenderer.render'
    Require-Layout $fluid 'calculateQuadColors\(' 'FluidRenderer.calculateQuadColors'

    $worldRenderer = Read-ClassLayout $jar 'me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer'
    Require-Layout $worldRenderer 'drawChunkLayer\(' 'SodiumWorldRenderer.drawChunkLayer'

    $manager = Read-ClassLayout $jar 'me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager'
    Require-Layout $manager 'submitRebuildTasks\(' 'RenderSectionManager.submitRebuildTasks'
    Require-Layout $manager 'scheduleRebuild\(int, int, int, boolean\)' 'RenderSectionManager.scheduleRebuild'
}

$embSection = Read-ClassLayout $EmbeddiumJar 'me.jellysquid.mods.sodium.client.render.chunk.RenderSection'
Require-Layout $embSection 'WeakReference<.*WrappedTask>.*rebuildTask' 'Embeddium weak wrapped-task field'
$embTask = Read-ClassLayout $EmbeddiumJar 'me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder$WrappedTask'
Require-Layout $embTask 'CompletableFuture<.*> getFuture\(\)' 'Embeddium wrapped-task future accessor'

$rubSection = Read-ClassLayout $RubidiumJar 'me.jellysquid.mods.sodium.client.render.chunk.RenderSection'
Require-Layout $rubSection 'CompletableFuture<\?> rebuildTask' 'Rubidium direct future field'

$ccl = Read-ClassLayout $EmbeddiumJar 'org.embeddedt.embeddium.compat.ccl.CCLCompat'
Require-Layout $ccl 'lambda\$onClientSetup\$2\(' 'validated Embeddium CCL populator lambda'

Write-Output 'PASS: validated Embeddium 0.3.18 and Rubidium 0.5.6 renderer-transfer layouts.'
