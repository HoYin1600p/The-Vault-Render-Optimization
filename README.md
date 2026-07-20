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

Armor texture paths are cached alongside durability, and armor model rendering uses Vault's built-in gear-model cache and per-entity armor-layer cache. This avoids repeatedly parsing gear data and rebuilding the same armor layer for every rendered frame.

### Vault Tool Model Cache

Resolved model locations for identified Vault tools are cached per `ItemStack`. Unidentified tools continue through Vault's original animated preview path.

### Ability HUD Cache

The learned-ability list used by Vault HUD and overlay rendering is retained until the client receives an ability-tree update. This avoids walking the complete ability tree in several render paths.

### Damage Number Allocation Fix

Floored damage numbers reuse Vault's existing render-thread formatter instead of constructing a new `DecimalFormat` for every visible damage number on every frame.

### Vault Client Render Event Dispatch Cache

Selected high-frequency Vault client render event dispatchers are also optimized with cached, priority-ordered listener snapshots. This avoids rebuilding and copying the listener tree for every `BiomeColorsEvent`, `DimensionEffectEvent`, `AmbientLightEvent`, and `RenderLevelLastEvent` invocation during chunk rebuild block/fluid tint, lighting, and end-of-level render hooks. Other Vault events keep their original dispatch behavior.

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
