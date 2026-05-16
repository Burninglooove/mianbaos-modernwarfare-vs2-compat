# mianbao's ModernWarfare VS2 Compat

Minimal Forge 1.20.1 compatibility mod for `mianbaos_modernwarfare` and Valkyrien Skies 2.

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
