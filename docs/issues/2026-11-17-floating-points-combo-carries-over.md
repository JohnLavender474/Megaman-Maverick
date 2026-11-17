---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: mega-refactor-047i-transition-path, LevelState review (2026-11-17)
related-items: [169, 013]
---

# The floating-points combo carries over into the next level

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/contracts/AbstractEnemy.kt:315-336` (`spawnFloatingPoints`)
- `core/src/main/kotlin/com/megaman/maverick/game/screens/levels/MegaLevelScreen.kt:522-524` (level start)
- `LevelState.timeSinceLastFloatingPoints` and `LevelState.lastFloatingPointsType`

## What happens

The level start sets `timeSinceLastFloatingPoints = 0f`, but it keeps `lastFloatingPointsType` from the last level.
`0f` is less than `ConstVals.AGGREGATE_FLOATING_POINTS_DUR` (`1f`), so the first enemy kill within 1 s of the level
start counts as part of a combo. It gives the next type after the last level's last type, not `POINTS100`.

## What should happen

The first kill of a level starts a new combo at `POINTS100`.

## How to reproduce

1. In a level, kill enemies quickly to step the combo up (for example to `POINTS400`).
2. Finish or leave the level, and start the next level.
3. Kill an enemy in the first second of the level. The points step up from the last level's type.

## Notes

- Found by reading the code; the reset at `MegaLevelScreen.kt:522-524` writes only the timer.
- A one-line fix is to also set `lastFloatingPointsType = null` at the level start. Item `169` proposes a level-run
  object with one `reset()` for coins, points and the combo, which also fixes this.
