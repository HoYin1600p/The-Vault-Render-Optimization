# The Vault Render Optimization 1.0.0

VRO's first full release. GPU rendering is now something you choose, and a new
in-game benchmark tells you whether it helps on your PC.

## Added

- **GPU benchmark**, at the bottom of the GPU rendering tab in the settings
  screen. It fills the view in front of you with a crowd only you can see (180
  mobs, half of them Vault Hunters mobs, plus armour stands, dropped items and
  particles), measures each GPU switch on and off, and recommends a change only
  when the difference is bigger than the measured noise. At the end it lists the
  recommended changes with their FPS gains and saves them only if you choose
  **Apply**.
  - Takes about 5 minutes (up to 7 with a shader pack). You cannot move or look
    around until it ends; opening a menu pauses it.
  - Stand somewhere safe, above a flat open area, facing empty space.
  - Works on servers where you are not an operator; nothing is spawned for other
    players. Also available as `/vro benchmark gpu start|cancel|status|result`.

## Changed

- GPU entity models, GPU items and GPU particles are now **off by default**.
  Whether they help depends on your CPU and graphics card. Updating from 0.5
  switches them off once; after that your choices are kept. Turn them on in the
  GPU rendering tab or with the benchmark. **Experimental** no longer turns on
  any GPU switch.

In testing on Vault Hunters 3rd Edition Remastered, the benchmark recommended
GPU entity models (about +35% FPS in its crowd scene) and left items and
particles off where their difference was within noise. Your results depend on
your hardware.

VRO remains client-only: stop Minecraft, replace the old VRO jar, and keep only
one version in your `mods` folder.

Full technical release details:
https://github.com/HoYin1600p/The-Vault-Render-Optimization/releases/tag/v1.0.0
