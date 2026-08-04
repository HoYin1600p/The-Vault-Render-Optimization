## VRO 0.3.3

This is a recommended stability update for everyone using VRO 0.3.2.

### Fixed

- Fixed a client crash that could happen while block entities were being added
  or removed from the world.
- Fixed VRO checking block entities even while its optional dynamic-light
  feature was turned off.
- Made block-entity dynamic lighting safe when users deliberately enable it.

The crash appeared as a `ConcurrentModificationException` and could occur with
VRO dynamic lights disabled. Dynamic lighting supplied by another mod, such as
Embeddium++, is unaffected.

No settings reset, cache deletion, world migration, or server installation is
required. Stop Minecraft, replace the older VRO JAR, and keep only one active
VRO JAR in the `mods` folder.
