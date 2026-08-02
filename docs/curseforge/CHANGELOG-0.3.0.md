## 0.3.0 - Initial Public Release

**Performance**

- Reduced repeated Vault gear, armor, tool, ability HUD, damage-number, and
  render-event work.
- Reduced client collision work in crowded entity areas.
- Added conservative particle, empty-render, tutorial, debug, and renderer
  lookup fast paths.
- Improved average 1% lows by 30.66% and p99 frame time by 13.94% across a
  controlled four-client, 40-trial campaign.

**Stability and compatibility**

- Releases selected Create Addition and Powah world state after unloads.
- Repairs known stale Vault Integrations altar and Powah cable client states.
- Restores Xaero's World Map `M` binding outside Vault dimensions.
- Automatically avoids overlapping work with Entity Collision FPS Fix,
  BadOptimizations, Particle Core, and Flerovium.

**Testing**

- Added immediate `/vro compare on|off` client commands for paired benchmarks.

No server installation or configuration reset is required.
