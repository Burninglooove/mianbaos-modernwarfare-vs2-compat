# ModernWarfare VS2 Bridge

Minimal Forge 1.20.1 bridge for `mianbaos_modernwarfare` and Valkyrien Skies 2.

## Features

- Adds `VS2` as an integrated mode in the native ModernWarfare radar GUI.
- Uses the native radar buttons:
  - `MODE`: cycles `NONE -> ENTITY -> PLAYER -> VS2 -> NONE`.
  - `<` / `>`: selects VS2 ship targets while in `VS2` mode.
  - `lock`: binds the selected VS2 ship to the radar channel.
  - `RESET`: clears the VS2 target.
- Caches VS2 ships once per dimension every 5 ticks.
- Excludes the ship carrying the radar, so ship-mounted radars do not lock themselves.
- Guides ModernWarfare projectiles by continuously feeding coordinate target NBT.

## Usage

1. Install this jar with Forge 1.20.1, Valkyrien Skies 2, and mianbao's ModernWarfare.
2. Place and open a ModernWarfare radar.
3. Press `MODE` until the mode text shows `VS2`.
4. Use `<` / `>` to choose a VS2 ship target.
5. Press `lock`.
6. Fire a guided ModernWarfare missile using the same radar channel.

## Notes

- The bridge aims at the target ship center.
- It does not replace ModernWarfare missile flight code.
- Control-Craft is not required for this first version.
- Channel-to-ship bindings are in memory and should be relocked after a server restart.
