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
- speed is sampled uniformly from 0.125 to 0.2 blocks/tick, or 2.5 to 4
  blocks/second;
- diameter shrinks continuously from 0.30 blocks to 10 percent of that size;
- physics, gravity, collision, fluid handling, and entity interaction stay off;
- ordinary depth testing and Rubidium/Forge frustum culling stay on;
- particles are full-bright for the first visual test;
- remaining orbs finish their trip after the trap disappears, but no new orbs
  are replenished.

The first-test colors are approximately `#D7F5FF` outside and `#091F57`
inside, with a 46-percent inner diameter. These are prototype artistic values,
not a compatibility contract.

The module counts live particle objects rather than rendered objects. A
culled orb therefore continues to move and cannot cause a compensating spawn
when the camera turns. Target populations are 80 on All, 52 on Decreased, and
20 on Minimal. A maximum of eight deficit particles is added per tick so the
initial population ramps in instead of appearing in one frame. Excess objects
expire naturally after a particle-quality reduction.

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
| `mana_stealer_visual_prototype.minimum_speed` | `0.125` | Minimum blocks/tick |
| `mana_stealer_visual_prototype.maximum_speed` | `0.2` | Maximum blocks/tick |
| `mana_stealer_visual_prototype.outer_diameter` | `0.3` | Starting outer diameter in blocks |
| `mana_stealer_visual_prototype.inner_diameter_ratio` | `0.46` | Inner/outer diameter ratio |

`/vro mana_stealer on|off` and `/vro mana_stealer sigil on|off` save and
apply immediately. Existing composite orbs finish their current trips after
the module is disabled.

## Removal boundary

The module can be removed by deleting its `client.compat.manastealer` and
`mixin.manastealer` packages, its dedicated mixin JSON, particle JSON/texture,
two focused tests, one config registration call, one command-tree entry, and
the corresponding MixinGradle config line. No general VRO particle or render
optimization depends on it.

## Required in-game validation

Before this prototype is considered suitable for any release, verify the real
trap with All, Decreased, and Minimal particles; Rubidium culling on/off;
shaders on/off; one and several simultaneous traps; cameras at the center,
edge, above, and behind terrain; trap expiration and world unload; Compare Mode
transitions; and the production reobfuscated JAR. Check for mixin failures,
premature center/frustum disappearance, render-state leakage, and any change to
the server's spherical drain behavior.
