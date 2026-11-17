# Notes: 166 Desert Man's stage rework (epic)

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

The older research of the merged items stays in their folders: `075`, `078` and `165` `NOTES.md`.

## 2026-11-17, `master` `fca7dba9d`

- Live map: `LevelDefinition.DESERT_MAN` (`DesertMan_16x14_v4.tmx`).
- Object counts in the live map: 63 `Lantern` (the darkness cave), 30 `Bat`, 13 `QuickSand`, 11 `TorikoPlundge`,
  10 `Cactus`, 9 `LampeonBandito`, 8 `Jetto`, 7 `ScooperPete`, 7 `Met`, 5 `DarkSerket`, 4 `CartinJoe`, 4 `Arigock`,
  3 `Cokeyro`, 3 `CarriCarry`, 1 `Water`.
- `Jetto` (`entities/enemies/Jetto.kt`): states `FLY`, `FALL`, `FROZEN`; animation `fly` (3 frames); atlas regions
  `Jetto/<color>/<state>`. In `FLY` it drops a `DeathBomb` each time `dropBombDelay` (`DROP_BOMB_DELAY = 0.2f`)
  ends, at `BOMB_DROP_Y = -8f`.
- No sandstorm or tornado hazard exists yet.
