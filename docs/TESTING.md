# Testing and benchmarking

## Quick comparison

1. Enter a stable location with the workload to test.
2. Keep render distance, shaders, weather, time, camera path, and other mod
   settings unchanged.
3. Run `/vro compare off` for the optimized condition.
4. Allow the scene to settle, then record several runs.
5. Run `/vro compare on` for the baseline condition.
6. Repeat the same route and workload.
7. Alternate conditions rather than measuring every enabled run first.

Compare Mode applies immediately. Restarting between conditions is not
required for VRO's runtime rendering paths, but a restart is useful when testing
startup-time ownership or optional-mod presence.

## Recommended controlled benchmark

- Use the same client, world, player position, route, and game mode.
- Fix time to noon with `doDaylightCycle=false`.
- Clear weather and disable weather cycling for the test period.
- Keep the client focused.
- Use the same 32-chunk render distance and uncapped frame rate.
- Keep VSync disabled.
- Keep shaders and Distant Horizons in the same state for every paired run.
- Preload the route before recording so chunk generation is not measured.
- Perform at least two unmeasured warm-up routes.
- Record at least five runs per condition in a balanced order.
- Measure average FPS, 1% lows, 0.1% lows, p99/p999 frame times, long-frame
  counts, CPU time, and garbage collection where available.

Average FPS alone can hide short stalls. VRO should be judged primarily by
paired frame-time distributions and long-frame counts.

## Acceptance checks

An optimization build is not release-ready until it has exercised:

- particle-heavy mob deaths and item processing;
- dense entities and block entities;
- Vault gear HUD, armor, tools, abilities, and damage numbers;
- Vault elixir-orb number particles followed by other colored particles;
- animated text, inventories, maps, and resource reloads;
- sustained Xaero map discovery followed by teleports, dimension changes, and
  vault transitions without stale cache-write crashes or lost future saves;
- model-heavy resource reloads with FerriteCore 4.2.2 present;
- vertical section culling in tall rooms, mountains, caves, the Nether, and
  spectator flight, plus opt-in horizontal culling at its distance boundary;
- dynamic lights off by default, then enabled with held, dropped, entity, and
  resource-defined block-entity sources, including source removal and movement;
- disconnect/reconnect, death/respawn, and dimension changes;
- world unload, normal client exit with Powah present, and a multi-hour session;
- shaders and Distant Horizons in a separate compatibility client;
- window resizing, fullscreen changes, and repeated shader toggles.

Terrain-culling tests must confirm that chunk loading and Distant Horizons
storage remain unchanged. Dynamic-light shader tests must confirm the default
pause policy and the explicit opt-in path separately.

Default optimizations must not suppress visible content or change lighting,
animation timing, transparency, model state, loot, movement, or server rules.
