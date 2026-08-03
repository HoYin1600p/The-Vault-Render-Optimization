## 0.3.2 - Release Candidate

**New since 0.3.0**

- Reduced retained model and block-state cache memory with two post-FerriteCore
  4.2.2 compaction improvements.
- Added vertical terrain-section culling, enabled by default at 12 sections
  above and below the camera. Optional horizontal culling remains off by default.
- Added an optional spatial dynamic-light engine for held and dropped items,
  luminous entities, and resource-defined block entities. It is off by default.
- Added `/vro culling` and `/vro lights` controls with saved client settings.

**Fixes**

- Prevented Vault elixir-orb number rendering from corrupting later particle
  colors.
- Preserved Powah world-cache cleanup without crashing during client exit.

**Compatibility**

- Terrain culling supports vanilla and Embeddium/Rubidium renderers without
  changing chunk loading or Distant Horizons storage.
- VRO yields terrain culling to Better Fps - Render Distance and dynamic
  lighting to Dynamic Lights Reforged when those mods are installed.
- One client-only JAR continues to support official, Remastered, Wolds, and
  selected custom Vault Hunters 1.18.2 baselines.

No server installation, world migration, or cache deletion is required.
