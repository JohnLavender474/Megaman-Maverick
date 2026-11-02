---
status: open
verified: question
area: core
gameplay-change: unknown
found-by: 118 research (2026-11-02)
related-items: [118]
---

# Question: the screen and the boss read `mini` and `end` from different sources

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/screens/levels/MegaLevelScreen.kt:699,701`: reads the map object, before the spawn.
- `core/src/main/kotlin/com/megaman/maverick/game/entities/bosses/WilyFinalBoss.kt:239`: forces `END=false` in `onSpawn`.
- `core/src/main/kotlin/com/megaman/maverick/game/entities/bosses/PenguinMiniBoss.kt:84`: forces `mini`. `MoonHead.kt:133` and `MechaDragon.kt:206` do too.

## What happens

The `WilyFinalBoss` map object has no `end`, so the screen reads `true`, and it checks `isLevelDefeated(WILY_STAGE_3)`. The boss reads `false`. The research infers that normal play cannot reach the check: the write at `MegaLevelScreen.kt:829` runs on `END_LEVEL`, and `Wily` ends with `GAME_COMPLETE`.

For a forced `mini`, the screen and the boss can also disagree. In all, 12 lines in 8 subclasses overwrite map props.

## What should happen

One source for each fight fact.

## How to reproduce

Read the code.

## Notes

The owner decided (2026-11-02) that `118a` removes the `wily_room` check as a named change, and that `FightPlans.resolve` becomes the one source. This issue closes with `118a`.
