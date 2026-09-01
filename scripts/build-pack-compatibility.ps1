param(
    [string]$PrismInstancesDirectory = "$env:APPDATA\PrismLauncher\instances",
    [switch]$RetainJar
)

$ErrorActionPreference = 'Stop'

$repositoryDirectory = Split-Path -Parent $PSScriptRoot
$gradleWrapper = Join-Path $repositoryDirectory 'gradlew.bat'
$propertiesPath = Join-Path $repositoryDirectory 'gradle.properties'

$profiles = @(
    @{ Name = 'VaultCrafters Bootstrap'; Folder = 'vaultcrafters-bootstrap-1.0.0'; Minecraft = '.minecraft' },
    @{ Name = 'Asgard-SMP'; Folder = 'Asgard-SMP'; Minecraft = 'minecraft' },
    @{ Name = 'Wolds Vaults 0.32.2'; Folder = 'Wolds-Vaults-0.32.2'; Minecraft = 'minecraft' },
    @{ Name = 'Wolds Vaults 0.33.0'; Folder = 'Wolds-Vaults-0.33.0'; Minecraft = 'minecraft' },
    @{ Name = 'Vault Hunters Third Edition'; Folder = 'Vault Hunters Third Edition'; Minecraft = 'minecraft' }
)

$targets = foreach ($profile in $profiles) {
    $modsDirectory = Join-Path $PrismInstancesDirectory "$($profile.Folder)\$($profile.Minecraft)\mods"
    $vaultJars = @(Get-ChildItem -LiteralPath $modsDirectory -File -Filter 'the_vault-*.jar')
    $renderJars = @(Get-ChildItem -LiteralPath $modsDirectory -File | Where-Object {
        $_.Name -match '^(embeddium|rubidium)-[0-9].*\.jar$'
    })

    if ($vaultJars.Count -ne 1) {
        throw "Expected exactly one active The Vault jar for $($profile.Name), found $($vaultJars.Count) in $modsDirectory"
    }
    if ($renderJars.Count -ne 1) {
        throw "Expected exactly one active Embeddium or Rubidium jar for $($profile.Name), found $($renderJars.Count) in $modsDirectory"
    }

    [PSCustomObject]@{
        Name = $profile.Name
        Jar = $vaultJars[0].FullName
        RenderJar = $renderJars[0].FullName
    }
}

Push-Location $repositoryDirectory
try {
    foreach ($target in $targets) {
        Write-Host "Compiling against $($target.Name): $($target.Jar), $($target.RenderJar)"
        & $gradleWrapper clean compileJava "-Pvault_mod_jar=$($target.Jar)" "-Prender_mod_jar=$($target.RenderJar)" --console=plain
        if ($LASTEXITCODE -ne 0) {
            throw "Compatibility compilation failed for $($target.Name)"
        }
    }

    $primaryTarget = $targets | Where-Object Name -eq 'Vault Hunters Third Edition' | Select-Object -First 1
    Write-Host "Building universal jar against the primary target: $($primaryTarget.Jar)"
    & $gradleWrapper clean build "-Pvault_mod_jar=$($primaryTarget.Jar)" "-Prender_mod_jar=$($primaryTarget.RenderJar)" --console=plain
    if ($LASTEXITCODE -ne 0) {
        throw 'Final universal jar build failed'
    }

    $modVersion = (Select-String -LiteralPath $propertiesPath -Pattern '^mod_version=(.+)$').Matches[0].Groups[1].Value
    $jarName = "vault_render_optimization.$modVersion.jar"
    $builtJar = Join-Path $repositoryDirectory "build\libs\$jarName"
    $hash = (Get-FileHash -LiteralPath $builtJar -Algorithm SHA256).Hash
    Write-Host "Compatibility build complete: $builtJar"
    Write-Host "SHA-256: $hash"
    if ($RetainJar) {
        $retainedJar = Join-Path $repositoryDirectory "libs\$jarName"
        Copy-Item -LiteralPath $builtJar -Destination $retainedJar -Force
        Write-Host "Retained compatibility jar: $retainedJar"
    } else {
        Write-Host 'Built jar was not copied into tracked libs; pass -RetainJar only for an authorized release build.'
    }
}
finally {
    Pop-Location
}
