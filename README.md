# The Vault Render Optimization

Small client-side Forge 1.18.2 optimization mod for Vault Hunters.

## Current Optimizations

This client-side mod targets render and chunk rebuild hotspots seen in Spark/JFR profiles from a Vault Hunters 1.18.2 client.

### Vault Gear HUD Cache

Targets:

`ItemHudModule.renderModule -> ItemStack.getMaxDamage -> VaultArmorItem.getMaxDamage -> VaultGearData.read(ItemStack)`

Vault armor max durability is cached per `ItemStack`. The cache refreshes when either:

- the stack's `vaultGearData` long array changes, or
- the cached value is more than one second old.

This keeps HUD/render paths from fully deserializing Vault gear data every frame while still responding quickly to actual gear NBT changes.

Armor damageability follows the newer Asgard Vault jar behavior by querying Vault's own `GearDataCache` for the gear state instead of deserializing full gear data directly from `VaultArmorItem.isDamageable`.

### Vault Client Render Event Dispatch Cache

Selected high-frequency Vault client render event dispatchers are also optimized with cached, priority-ordered listener snapshots. This avoids rebuilding and copying the listener tree for every `BiomeColorsEvent`, `DimensionEffectEvent`, `AmbientLightEvent`, and `RenderLevelLastEvent` invocation during chunk rebuild block/fluid tint, lighting, and end-of-level render hooks. Other Vault events keep their original dispatch behavior.

### Chunk Rebuild Diagnostics

Temporary Embeddium chunk rebuild diagnostics are enabled by default. Every few seconds the client log receives a `[VRO chunk diagnostics]` summary covering rebuild schedules, render-thread `updateChunks`, region uploads, upload batch setup, stolen chunk rebuild tasks, rebuild task timings, and sampled schedule callers. Launch with `-Dvault_render_optimization.chunkDiagnostics=false` to disable the diagnostic logging without removing the jar.

## Build

```powershell
.\gradlew.bat clean build
```

Output:

`build/libs/vault_render_optimization.0.1.jar`

By default the build looks for The Vault and Embeddium in the Prism Launcher bootstrap instance under the current Windows user profile. If either jar lives somewhere else, pass overrides:

```powershell
.\gradlew.bat clean build -Pvault_mod_jar="C:\path\to\the_vault.jar" -Pembeddium_mod_jar="C:\path\to\embeddium.jar"
```
