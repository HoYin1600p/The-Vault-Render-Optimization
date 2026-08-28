## VRO 0.4.0

VRO can now let you know when a newer release is available—without automatic
downloads, forced updates, or any server requirement.

### Update notices

- Added a small update row to the main menu when an allowed VRO update is
  available.
- Added occasional in-game reminders with a clickable link to VRO's official
  CurseForge page.
- Checks run in the background with short timeouts and safely disappear when
  GitHub, the network, or the manifest is unavailable.
- Critical notices are enabled by default. Normal update notices remain hidden
  unless you explicitly select all update types.
- Rejoining worlds, changing dimensions, or transferring servers will not
  repeat the reminder during the same Minecraft launch.

### Controls

- `/vro updates` or `/vro updates status` shows the current settings.
- `/vro updates on|off` enables or disables update checks immediately.
- `/vro updates critical|all` chooses critical-only or all update notices.
- The same settings are saved in
  `config/vault_render_optimization-client.toml`.

No server installation, world migration, cache deletion, or settings reset is
required. Stop Minecraft, replace the older VRO jar, and keep only one active
VRO jar in the `mods` folder.
