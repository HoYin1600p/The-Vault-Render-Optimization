# The Vault Render Optimization

Small client-side Forge 1.18.2 optimization mod for Vault Hunters.

Vault Hunters Third Edition is the shipping baseline. The same jar is also compiled against the custom VaultCrafters Bootstrap, Asgard-SMP, and Wolds Vaults instances as compatibility targets; mods found only in those private instances are not assumed to be part of the shipped pack. The Vault, Vault Integrations, Powah, Create Addition, and Entity Collision FPS Fix integrations are selected only when the corresponding mod is present. There are no hard mod dependencies beyond Minecraft and Forge.

Prospective optimization sources, licenses, overlap checks, and validation requirements are tracked in [`docs/PERFORMANCE_BACKPORT_RESEARCH.md`](docs/PERFORMANCE_BACKPORT_RESEARCH.md).

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

### World Map Key Compatibility

The Vault's open-map key binding is active only while a client vault is active. This prevents its default `M` binding from consuming the same Forge key click used by Xaero's World Map in overworld dimensions, while preserving the Vault map inside vaults.

### Client Crash Guards

Known stale client state is repaired where recovery is deterministic. Vault Integrations altar conduits initialize a missing placement position before ticking. Powah cable registration replaces a stale cable at the same position and continues its normal adjacent-network refresh instead of terminating the client after the replacement has already happened. Both guards are client-only and leave server behavior unchanged.

### Client Entity Collision Checks

Client-side suffocation checks and living-entity push calculations are skipped. The server remains authoritative for collisions and movement, while crowded mob and item-processing areas avoid repeating collision work that cannot change the server result.

This behavior is adapted from the CC0-licensed Entity Collision FPS Fix mod. If `entitycollisionfpsfix` is still installed, these mixins disable themselves automatically and leave the standalone mod in control. See `THIRD_PARTY_NOTICES.md` for source and license details.

### Unloaded World Cleanup

When a world or dimension unloads, retained Create Addition energy-network and Powah cable-network entries for that exact world are removed. This prevents old integrated-server and client-level objects from remaining reachable across dimension changes, world reconnects, and long play sessions.

Create and Flywheel are not patched here because the installed versions already invalidate their world-attached caches during the Forge world-unload event.

## Build

```powershell
.\gradlew.bat clean build
```

Output:

`build/libs/vault_render_optimization.0.3-dev.jar`

By default the build looks for The Vault in the supported Prism Launcher instances under the current Windows user profile. If the jar lives somewhere else, pass an override:

```powershell
.\gradlew.bat clean build -Pvault_mod_jar="C:\path\to\the_vault.jar"
```

The normal build discovers the active The Vault jar in the known Prism instances and uses VaultCrafters Bootstrap as its primary target. Embeddium is not a compile-time or runtime dependency.

Before retaining a release or test jar, run the complete compatibility matrix:

```powershell
.\scripts\build-pack-compatibility.ps1
```

This compiles the complete source against each installed The Vault jar, performs a final build against the primary target, and copies the resulting universal jar into `libs`.

Validated pack targets:

| Prism instance | The Vault jar |
| --- | --- |
| VaultCrafters Bootstrap | `the_vault-1.18.2-20.0.3-remastered.jar` |
| Asgard-SMP | `the_vault-1.18.2-3.21.62.jar` |
| Wolds Vaults | `the_vault-1.18.2-3.21.5.6573.jar` |
| Vault Hunters Third Edition | `the_vault-1.18.2-3.21.6.6884.jar` |
