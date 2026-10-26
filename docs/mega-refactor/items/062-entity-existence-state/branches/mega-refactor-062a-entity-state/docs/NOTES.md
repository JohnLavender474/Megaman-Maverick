# Notes: mega-refactor-062a-entity-state

## Decisions

## Overlap with other branches

None: no other branch is active.

## Evidence for `062b`: the odd spawn and destroy paths in play (2026-10-26)

The owner's play test ran the six manual checks of `docs/CONTRACT.md` with `--logTags GameEngine` and `--logLevels
debug,log,error` (the alpha script), and copied `assets/logs.txt` after each run.

| Run | Odd-path lines (`GameEngine` tag) |
|---|---|
| `REFACTOR_TEST_029 --room enemy_aim` (freeze, thaw, freeze again, kill, respawn) | 0 |
| `REFACTOR_TEST_029 --room pickups` | 0 |
| `REFACTOR_TEST_029 --room player_death` (death and respawn) | 0 |
| `REFACTOR_TEST_029 --room player_death` (game over, save screen, a second level start) | 0 |
| `INTRO_STAGE --room room12` (boss fight, beam-out, the next level start) | 0 |
| `REFACTOR_TEST_029 --room movement` | 0 |

- The log file works in these runs: the boss run wrote an `ERROR` line of `GetWeaponScreen` ("show(): failed to load
  weapons attained"; the intro stage gives no weapon, so the screen has nothing to show; not related to this branch).
  So the zero is a real zero, not a missing log.
- Meaning for `062b`: none of the three odd paths (a spawn of a live entity, a spawn while a destroy is queued, a
  destroy of an unspawned entity) happened in these rooms. That is evidence that the common paths do not depend on
  them, but not that no caller does: the runs cover a small part of the game. `062b` needs a wider search (a longer
  play with the tag on, or a static search of the `spawn`/`destroy` callers that do not guard with `dead` or
  `spawned`).
- The engine tests already prove what each odd path does today (`EntityExistenceStateTest`, "the odd paths keep the
  engine's actions").
