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

VRO also turns on Flywheel's `INSTANCING` backend for the session when a pack
ships with Flywheel set to `OFF`, without changing Flywheel's config file.
Turning `create_rendering.auto_enable_flywheel_instancing` off (default on)
returns to the pack's setting immediately. It does not bypass
Flywheel's GPU capability checks, and shader integration failures still switch
to the standard renderer.

## Supported Stack

- Minecraft 1.18.2
- Forge 40.3.11 or newer in the Forge 40 line
- Create 0.5.1.i
- Flywheel 0.6.11-107
- Oculus 1.6.x: public Oculus 1.6.4 and HoYin1600p's `dh-compat` builds
  (1.6.5, 1.6.7 and 1.6.8 were checked)
- Rubidium 0.5.6 or compatible Embeddium releases

The optional mixins are not applied unless Create, Flywheel, Oculus, and a
Rubidium/Embeddium renderer are all present. Untested Oculus or Flywheel
version lines are rejected instead of attempting uncertain injections.

Within the 1.6.x line, several different Oculus builds exist (some share a
version string), so VRO also checks the installed Oculus jar before any
compatibility mixin applies (`OculusCompatContract`). Every Oculus class,
method, field, injection target, invoker and accessor used by VRO's
compatibility code must exist. If one is missing, the compatibility is disabled
with a log message naming it and Create uses its standard renderer, instead of
failing an injection at startup. `OculusCompatContractTest` runs the same check
against every Oculus jar the build finds in the known instances, plus any given
with `-Poculus_contract_jars=path1;path2`.

## Oculus Flywheel Compat (irisflw)

VRO's implementation is a newer adaptation of Oculus Flywheel Compat and patches
the same Flywheel and Oculus methods, so the two must not both run. When VRO's
compatibility is available and `irisflw` is installed, VRO removes irisflw's
mixins before they are applied and logs a warning that the
`oculus-flywheel-compat` jar can be removed. irisflw's mod class only logs, so
nothing else of it runs. If the replacement cannot be done safely (for example on
an unexpected Mixin version), VRO disables its own compatibility instead and
says so, leaving irisflw alone.

## Included Compatibility

- Instanced and batched Flywheel shader-program compilation
- Create lighting-volume texture reservation
- Extended block vertex attributes and `mc_Entity` block IDs
- Normals and tangents required by normal mapping and PBR shader paths
- Solid, cutout-mipped, and cutout render-layer ordering
- Shader shadow-pass rendering
- Shader reload and pipeline cache cleanup
- Buffer-local format selection and deferred instanced model ticks during an
  Oculus pipeline gap, including a DH-ready reload before login
- Automatic fallback to Create's standard renderer after a compile failure

The implementation is adapted from the MIT-licensed Iris & Oculus Flywheel
Compat project. See `CREDITS.md`, `THIRD_PARTY_NOTICES.md`, and
`docs/licenses/iris-flw-compat-MIT.txt` for exact provenance and terms.

## Dedicated Shader-Pack Programs

Shader packs may provide `gbuffers_flw.vsh/.fsh` for normal scene rendering
and `shadow_flw.vsh/.fsh` for shadow-map rendering. VRO prefers a valid
dedicated program, then injects the Flywheel 0.6 vertex layout required by the
specific Create material being compiled. This allows a shader author to tune
fragment, geometry, blending, and buffer behavior for Create separately from
ordinary block rendering.

When either dedicated program is absent, invalid, or fails to compile, VRO
retries that pass with its generated compatibility program. A malformed
optional program therefore does not remove the existing fallback. Use
`/vro create status` to see whether the scene and shadow passes selected
`DEDICATED`, `GENERATED FALLBACK`, or
`GENERATED FALLBACK AFTER DEDICATED FAILURE`.

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
14. With DH and shaders enabled, restart at a base with moving contraptions;
    verify login and the first rendered frames, then repeat shader reloads.
    See `FLYWHEEL_STARTUP_FORMAT_FIX.md` for the targeted regression evidence.
