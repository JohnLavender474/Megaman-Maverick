# Notes: mega-refactor-062b-spawn-order-guards

## Decisions

## Overlap with other branches

- `019b` (`mega-refactor-019b-destroy-notification`, active since 2026-10-27, planning only; its engine change waits
  until `062b` merges, owner's decision in its `docs/NOTES.md`). Its scan names `GameEngine.onSpawnQueued` and
  `onDestroyQueued` as the place for a queue-time destroy notification; `062b` removes both, and the same logic is now
  in `spawn()` and `destroy()`. `062b` adds `RESPAWN_QUEUED` (a spawn after a destroy in the same tick respawns the
  entity after the destroy phase; `dead` is `false` while it waits) and refuses a destroy of an unspawned entity, so a
  spawner can be told its entity is destroyed and then see it spawn again in the same tick. No code overlap: `019b`
  has no code commits. Checked on 2026-10-27 before the `062b` merge.

## The owner's play test with `--logTags GameEngine` (2026-10-27)

Eight contract runs (`REFACTOR_TEST_029` `enemy_aim`, `pickups`, `player_death`; `TIMBER_WOMAN --room room5`;
`DESERT_MAN --room room3`; `RODENT_MAN --room room13` and `room16`; `INTRO_STAGE --room room12`): no `GameEngine` line,
so no call was refused, no respawn was queued and no respawn was cancelled in these runs. The log files work (the
`DESERT_MAN` map errors and the intro stage's `GetWeaponScreen` error are in them). Together with `062a`'s six runs:
no odd call in 14 runs; the static scan of `docs/SCOPE.md` found one caller of path B (the ice shield) and the
same-frame window of path C.
