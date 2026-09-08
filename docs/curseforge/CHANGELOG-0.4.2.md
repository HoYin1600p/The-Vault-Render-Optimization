# The Vault Render Optimization 0.4.2

This update makes busy Sophisticated Storage barrel walls considerably cheaper
to render without hiding anything you should be able to see.

## What's new

- Hidden and covered barrel fronts no longer spend time drawing items,
  quantities, fill levels, or upgrades.
- Repeated quantity labels are reused instead of rebuilt for every barrel.
- Fill-level bars use a lighter rendering path.
- Count- and fill-only updates avoid unnecessary chunk-model rebuilds.
- Tier badges are deliberately unchanged.
- New hot `/vro storage` controls and optional diagnostics make the behavior
  easy to inspect or compare in game.

The new path is enabled by default for the validated Vault Hunters Remastered
Sophisticated Storage/Core versions. Unknown versions safely retain their
original rendering.

## Fix

Limited-barrel fill bars now use the correct texture coordinates, including the
small multi-slot fill column found during release testing.

VRO remains client-only. Stop Minecraft, replace the old VRO jar, and keep only
one version in your `mods` folder. No server update, world migration, or config
reset is required.

Full technical release details:
https://github.com/HoYin1600p/The-Vault-Render-Optimization/releases/tag/v0.4.2
