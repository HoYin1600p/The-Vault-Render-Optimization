# Create Shader Instancing Compatibility

## Purpose

Flywheel 0.6.11 disables its GPU instancing and batching backends whenever
Oculus reports that a shader pack is active. Large moving Create contraptions
then use Create's CPU-transformed fallback renderer, which can reduce frame
rate dramatically.

VRO can keep Flywheel's configured GPU backend available while Oculus shaders
are active. It merges Flywheel's generated vertex logic into the active
shader-pack block or shadow program. The feature is client-only and does not
change Create simulation or server behavior.

## Supported Stack

- Minecraft 1.18.2
- Forge 40.3.11 or newer in the Forge 40 line
- Create 0.5.1.i
- Flywheel 0.6.11-107
- Oculus 1.6.x, compiled and verified against public Oculus 1.6.4
- Rubidium 0.5.6 or compatible Embeddium releases

The optional mixins are not applied unless Create, Flywheel, Oculus, and a
Rubidium/Embeddium renderer are all present. Untested Oculus or Flywheel
version lines are rejected instead of attempting uncertain injections.

## Included Compatibility

- Instanced and batched Flywheel shader-program compilation
- Create lighting-volume texture reservation
- Extended block vertex attributes and `mc_Entity` block IDs
- Normals and tangents required by normal mapping and PBR shader paths
- Solid, cutout-mipped, and cutout render-layer ordering
- Shader shadow-pass rendering
- Shader reload and pipeline cache cleanup
- Automatic fallback to Create's standard renderer after a compile failure

The implementation is adapted from the MIT-licensed Iris & Oculus Flywheel
Compat project. See `CREDITS.md`, `THIRD_PARTY_NOTICES.md`, and
`docs/licenses/iris-flw-compat-MIT.txt` for exact provenance and terms.

## Controls And Recovery

The feature defaults on. It can be changed and saved while a world is loaded:

```text
/vro create shader_compat on
/vro create shader_compat off
/vro create shader_compat status
```

Changing the setting refreshes Flywheel and rebuilds Create world renderers.
VRO Compare Mode also disables this optimization for controlled comparisons.

For early startup recovery, disable all compatibility mixins with this JVM
argument:

```text
-Dvault_render_optimization.flywheelShaderCompat=false
```

## Expected Logs

With the supported stack installed, VRO reports that it is loading Create
shader instancing compatibility. After a compatible shader program is built,
it reports successful integration. If compilation fails, VRO records the
affected Flywheel program and refreshes Create onto its standard shader-safe
renderer on the next frame.

## Manual Acceptance Test

1. Confirm the same scene and camera position before each comparison.
2. Confirm Create's Flywheel backend is set to instancing.
3. Start with shaders disabled and verify moving contraptions render normally.
4. Enable the target shader and run `/vro create shader_compat status`.
5. Verify moving contraptions retain textures, block colors, lighting, and motion.
6. Compare frame rate while the same large contraption group is moving.
7. Toggle shaders off and on at least five times.
8. Reload the shader pack, resize the window, and change dimensions.
9. Stop and restart moving contraptions after each transition.
10. Verify translucent and cutout Create parts, belts, shafts, cogs, and fluids.
11. Verify no stale frames, black geometry, missing contraptions, or GL errors.
12. Disable compatibility in-game and confirm Create returns to its standard path.
13. Repeat once with the startup recovery argument to verify a clean no-mixin launch.
