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

### Vault Biome Color Event Dispatch Cache

Vault's biome color event dispatcher is also optimized with a cached, priority-ordered listener snapshot. This avoids rebuilding and copying the listener tree for every `BiomeColorsEvent` invocation during chunk rebuild block and fluid tint lookups. Other Vault events keep their original dispatch behavior.

## Build

```powershell
.\gradlew.bat clean build
```

Output:

`build/libs/vault_render_optimization.0.1.jar`

By default the build looks for The Vault in the Prism Launcher bootstrap instance under the current Windows user profile. If the jar lives somewhere else, pass an override:

```powershell
.\gradlew.bat clean build -Pvault_mod_jar="C:\path\to\the_vault.jar"
```
