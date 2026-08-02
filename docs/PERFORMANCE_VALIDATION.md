# Performance validation

Validation date: August 2, 2026

## Method

Four Vault Hunters clients each completed five VRO-enabled and five
VRO-disabled trials. The campaign enforced:

- one focused Minecraft process at a time;
- a prepared world and identical generated route within each client;
- spectator mode in the overworld;
- noon, disabled daylight cycling, and clear weather;
- render distance 32, VSync off, and uncapped frame rate;
- shaders and Distant Horizons rendering disabled;
- two preload routes and 200 warm-up ticks;
- balanced enabled/disabled ordering;
- continuous checks for focus, world, time, weather, render settings, live
  frame updates, benchmark progress, and result completeness;
- a post-run camera movement and frozen-frame assertion.

All 40 measured trials passed. Each comparison is within the same client;
different pack mod stacks are not treated as directly comparable hardware
benchmarks.

## Results

| Client | Average FPS gain | 1% low gain | 0.1% low gain | p99 improvement | Client CPU improvement |
| --- | ---: | ---: | ---: | ---: | ---: |
| Asgard | 9.43% | 38.43% | 14.86% | 26.74% | 8.83% |
| Remastered | 6.00% | 22.27% | 25.12% | 13.42% | 5.73% |
| Third Edition | 0.70% | 16.64% | 25.28% | 5.25% | 1.00% |
| Wolds | 5.27% | 45.30% | 75.00% | 10.33% | 2.38% |
| Unweighted mean | 5.35% | 30.66% | 35.07% | 13.94% | 4.49% |

Frames longer than 16.7 ms decreased in every client:

| Client | Baseline | VRO enabled |
| --- | ---: | ---: |
| Asgard | 180 | 66 |
| Remastered | 69 | 38 |
| Third Edition | 54 | 19 |
| Wolds | 87 | 15 |

Garbage-collection count and total collection time also decreased in every
client during these trials.

## Interpretation

VRO's strongest repeatable result is improved frame consistency and fewer long
frames. Average FPS rose in every aggregate, but the trial-level average-FPS
interval crossed zero for Third Edition and Wolds. The release therefore does
not promise a fixed average-FPS percentage on every machine or pack.

These results validate the complete 0.3.0 feature set as tested. They do not
prove that every individual optimization contributes equally, and they do not
replace compatibility testing for a user's exact resource packs, shaders, and
optional mods.
