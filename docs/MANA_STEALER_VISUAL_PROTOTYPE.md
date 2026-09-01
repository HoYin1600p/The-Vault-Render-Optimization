# Mana Stealer visual prototype

This development-only module replaces the client presentation of Vault
Hunters' Mana Stealer chest trap. It is intentionally isolated so it can move
to another mod or be removed from VRO without touching the trap's gameplay.

The server remains authoritative. VRO does not inject into
`ManaStealerTrapEffect.apply(...)`, the server branch of
`ManaStealerEntity.tick()`, mana values, player selection, trap duration,
sounds, packets, or entity registration.

## First-test presentation

While the synchronized `the_vault:mana_stealer` entity exists, VRO maintains a
bounded population of composite camera-facing orbs:

- one particle object draws both a pale-blue outer ball and navy inner ball;
- starts are uniformly distributed on the configured spherical radius;
- every orb travels in a straight line to the exact entity center;
- speed is sampled uniformly from 0.1625 to 0.26 blocks/tick, or 3.25 to 5.2
  blocks/second;
- with the default 0.30-block reference diameter, the outer layer shrinks
  continuously from 0.75 blocks to 0.0375 blocks;
- physics, gravity, collision, fluid handling, and entity interaction stay off;
- ordinary depth testing and Rubidium/Forge frustum culling stay on;
- particles are full-bright for the first visual test;
- remaining orbs finish their trip after the trap disappears, but no new orbs
  are replenished.
- orbs begin at 250 percent of the configured reference diameter at the outer
  edge of the sphere, then shrink continuously to a 12.5-percent
  diameter at the trap center.

The revised first-test colors are approximately `#64C7FF` outside and
`#0A2B80` inside, with a 46-percent inner diameter. The stronger saturation
keeps both layers visibly blue at the intended viewing distance instead of
letting full-bright blending read as white or lead-gray. These are prototype
artistic values, not a compatibility contract. The navy billboard is biased
toward the camera by only 1/4096 of a block (approximately 0.244 mm) so its
depth ordering remains stable without a perceptible gap from the outer layer.

The module counts live particle objects rather than rendered objects. A
culled orb therefore continues to move and cannot cause a compensating spawn
when the camera turns. Target populations are 80 on All, 52 on Decreased, and
20 on Minimal. A maximum of eight deficit particles is added per tick so the
initial population ramps in instead of appearing in one frame. Excess objects
expire naturally after a particle-quality reduction.

## Affected-player drain stream

While a player is inside the synchronized trap radius, VRO also draws a steady
stream of the same pale-blue/navy composite orbs from the player's upper torso
to the trap center. The real-trap path mirrors Vault's spherical
`player.distanceToSqr(trap) <= radius * radius` boundary and excludes spectator
and creative players, matching the server's eligibility checks. Leaving the
radius, unloading either entity, disabling the stream, or ending the trap
removes the stream immediately.

The stream is a direct batched render rather than a collection of ParticleEngine
objects. Both endpoints are resampled while rendering so the path remains
attached as the player moves. Orbs travel player-to-trap at 0.32 blocks/tick,
use a shallow deterministic spiral with a maximum 0.18-block spread, retain
ordinary depth testing, and reuse the chest effect's sprite, colors, opacity,
full-bright treatment, and 46-percent navy core.

Vault's original Arcane ray emits about 18 particles per tick across six blocks
and keeps them for five ticks, or approximately 90 simultaneously visible
particles. The stream's default six-block target is 45 composite orbs on All,
29 on Decreased, and 11 on Minimal. Each composite orb is still one logical
visual with two aligned billboard layers.

This is a geometric client prediction, not an authoritative mana transaction
notification. It intentionally shows when an eligible player is inside an
active drain radius; it cannot prove that a nonzero amount of mana was removed
on that exact server tick.

## Narrow mixin ownership

The dedicated `mixins.vault_render_optimization.mana_stealer.json` contains
only:

- `ManaStealerEntityParticlesMixin`, which conditionally cancels the private
  `spawnClientParticles()V` dust loop; and
- `ManaStealerRendererMixin`, which conditionally cancels the typed
  `ManaStealerRenderer.render(...)` ground-sigil method.

A module-specific mixin plugin applies them only on a physical client with
`the_vault` installed. If the particle sprite/factory is unavailable, neither
legacy visual is cancelled. Compare Mode also restores both Vault visuals.

## Configuration and hot controls

The isolated configuration file is:

```text
config/vault_render_optimization-mana-stealer-client.toml
```

| Key | Default | Purpose |
| --- | ---: | --- |
| `mana_stealer_visual_prototype.enabled` | `true` | Enables the experimental dust replacement |
| `mana_stealer_visual_prototype.replace_ground_sigil` | `true` | Hides the legacy flat line sigil while active |
| `mana_stealer_visual_prototype.all_population` | `80` | All-particle live target |
| `mana_stealer_visual_prototype.decreased_population` | `52` | Decreased-particle live target |
| `mana_stealer_visual_prototype.minimal_population` | `20` | Minimal-particle live target |
| `mana_stealer_visual_prototype.max_spawns_per_tick` | `8` | Initial/refill ramp limit |
| `mana_stealer_visual_prototype.minimum_speed` | `0.1625` | Minimum blocks/tick |
| `mana_stealer_visual_prototype.maximum_speed` | `0.26` | Maximum blocks/tick |
| `mana_stealer_visual_prototype.outer_diameter` | `0.3` | Reference outer diameter in blocks |
| `mana_stealer_visual_prototype.inner_diameter_ratio` | `0.46` | Inner/outer diameter ratio |
| `mana_stealer_visual_prototype.drain_stream_enabled` | `true` | Shows the affected-player stream |
| `mana_stealer_visual_prototype.drain_stream_density` | `6.5` | All-particle visible orbs per block |
| `mana_stealer_visual_prototype.drain_stream_minimum_orbs` | `6` | Minimum stream population |
| `mana_stealer_visual_prototype.drain_stream_maximum_orbs` | `48` | Per-player/per-trap All-particle cap |
| `mana_stealer_visual_prototype.drain_stream_speed` | `0.32` | Player-to-trap blocks/tick |
| `mana_stealer_visual_prototype.drain_stream_orb_diameter` | `0.22` | Stream-orb reference diameter |
| `mana_stealer_visual_prototype.drain_stream_spread` | `0.18` | Maximum center-line spread in blocks |

`/vro mana_stealer on|off`, `/vro mana_stealer sigil on|off`, and
`/vro mana_stealer stream on|off` save and apply immediately. Existing chest
orbs finish their current trips after the module is disabled; drain streams
stop immediately.

The client-only preview command simulates the complete replacement visual and
its beacon activation/ambient audio without creating a Vault entity, draining
mana, or changing the world:

```text
/vro mana_stealer preview <ticks>
/vro mana_stealer preview <ticks> <x> <y> <z>
/vro mana_stealer preview stop
```

Without coordinates, the center is one block above the center of the targeted
block, matching the real chest trap. If no block is targeted, it is four blocks
ahead of the player's eyes. Explicit coordinates are the exact sphere center
and support normal Minecraft relative-coordinate syntax such as `~ ~2 ~`.
Preview duration is caller-selected, while its radius remains the real 6.0
blocks. The preview intentionally works even when replacement ownership is off
so it can be used for visual testing; it still honors particle quality and the
prototype's population, speed, size, and refill configuration. Starting a new
preview replaces the active replenishment source. Stopping or reaching the
requested duration lets already-live orbs finish naturally.

The preview also draws the local player's drain stream whenever the player is
inside its radius. Preview mode intentionally permits creative and spectator
players so the visual can be rehearsed safely in a disposable test world.

## Removal boundary

The module can be removed by deleting its `client.compat.manastealer` and
`mixin.manastealer` packages, its dedicated mixin JSON, particle JSON/texture,
its focused tests, one config registration call, one command-tree entry, and
the corresponding MixinGradle config line. No general VRO particle or render
optimization depends on it.

## Required in-game validation

Before this prototype is considered suitable for any release, verify the real
trap with All, Decreased, and Minimal particles; Rubidium culling on/off;
shaders on/off; one and several simultaneous traps; cameras at the center,
edge, above, and behind terrain; trap expiration and world unload; Compare Mode
transitions; creative/spectator exclusion; moving players crossing the exact
radius boundary; first- and third-person stream attachment; multiple players
and traps; and the production reobfuscated JAR. Check for mixin failures,
premature center/frustum disappearance, render-state leakage, and any change to
the server's spherical drain behavior.
