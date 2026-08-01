param(
    [string]$PrismInstancesDirectory = "$env:APPDATA\PrismLauncher\instances"
)

$ErrorActionPreference = 'Stop'

$repositoryDirectory = Split-Path -Parent $PSScriptRoot
$gradleWrapper = Join-Path $repositoryDirectory 'gradlew.bat'
$propertiesPath = Join-Path $repositoryDirectory 'gradle.properties'

$profiles = @(
    @{ Name = 'VaultCrafters Bootstrap'; Folder = 'vaultcrafters-bootstrap-1.0.0'; Minecraft = '.minecraft' },
    @{ Name = 'Asgard-SMP'; Folder = 'Asgard-SMP'; Minecraft = 'minecraft' },
    @{ Name = 'Wolds Vaults'; Folder = 'Wolds-Vaults-0.32.2'; Minecraft = 'minecraft' },
    @{ Name = 'Vault Hunters Third Edition'; Folder = 'Vault Hunters Third Edition'; Minecraft = 'minecraft' }
)

$targets = foreach ($profile in $profiles) {
    $modsDirectory = Join-Path $PrismInstancesDirectory "$($profile.Folder)\$($profile.Minecraft)\mods"
    $vaultJars = @(Get-ChildItem -LiteralPath $modsDirectory -File -Filter 'the_vault-*.jar')

    if ($vaultJars.Count -ne 1) {
        throw "Expected exactly one active The Vault jar for $($profile.Name), found $($vaultJars.Count) in $modsDirectory"
    }

    [PSCustomObject]@{
        Name = $profile.Name
        Jar = $vaultJars[0].FullName
    }
}

Push-Location $repositoryDirectory
try {
    foreach ($target in $targets) {
        Write-Host "Compiling against $($target.Name): $($target.Jar)"
        & $gradleWrapper clean compileJava "-Pvault_mod_jar=$($target.Jar)" --console=plain
        if ($LASTEXITCODE -ne 0) {
            throw "Compatibility compilation failed for $($target.Name)"
        }
    }

    $primaryTarget = $targets | Where-Object Name -eq 'Vault Hunters Third Edition' | Select-Object -First 1
    Write-Host "Building universal jar against the primary target: $($primaryTarget.Jar)"
    & $gradleWrapper clean build "-Pvault_mod_jar=$($primaryTarget.Jar)" --console=plain
    if ($LASTEXITCODE -ne 0) {
        throw 'Final universal jar build failed'
    }

    $modVersion = (Select-String -LiteralPath $propertiesPath -Pattern '^mod_version=(.+)$').Matches[0].Groups[1].Value
    $jarName = "vault_render_optimization.$modVersion.jar"
    $builtJar = Join-Path $repositoryDirectory "build\libs\$jarName"
    $retainedJar = Join-Path $repositoryDirectory "libs\$jarName"

    Copy-Item -LiteralPath $builtJar -Destination $retainedJar -Force
    $hash = (Get-FileHash -LiteralPath $retainedJar -Algorithm SHA256).Hash

    Write-Host "Compatibility build complete: $retainedJar"
    Write-Host "SHA-256: $hash"
}
finally {
    Pop-Location
}
