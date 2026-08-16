## VRO 0.3.5

This update contains everything added since the previous CurseForge release,
VRO 0.3.3.

### Create and shader performance

- Large Create contraptions can now skip rendering sections that are outside
  the camera view, without reducing model detail.
- Added support for Flywheel GPU instancing while Oculus shaders are active on
  supported Rubidium and Embeddium setups. This targets the large performance
  drop that moving Create contraptions could cause with shaders enabled.
- VRO now uses dedicated Flywheel scene and shadow programs when a shader pack
  provides them. Packs without them continue to use VRO's generated
  compatibility path.
- Missing or broken optional shader programs fall back automatically instead
  of disabling the shader pack.
- Added `/vro create status` and `/vro create shader_compat on|off|status` for
  immediate diagnostics and control.

### Performance and memory

- Reduced repeated rendering work for iSpawner displays.
- Vault Loot Beams tooltip data is now created only when it is needed and is
  cleared when the world unloads.
- Prevented Minecraft's shared empty item stack from retaining an old dropped
  item.

### Stability

- Fixed a Powah cable replacement race that could interrupt chunk updates or
  crash the client.
- Fixed a Xaero's World Map cache race that could crash the client during map
  writes.

No server installation, world migration, cache deletion, or settings reset is
required. Stop Minecraft, replace the older VRO jar, and keep only one active
VRO jar in the `mods` folder.

Shader packs do not need dedicated Flywheel programs to keep working. Those
programs are an optional improvement, and VRO retains its existing fallback.
