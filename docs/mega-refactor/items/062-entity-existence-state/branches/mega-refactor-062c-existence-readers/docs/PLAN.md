# Plan: mega-refactor-062c-existence-readers

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-10-27

## New API and first callers

| API | First caller |
|---|---|
| `EntityExistenceState.liveNow` | `ReactorMan.kt:412` (step 4) |
| `EntityExistenceState.liveAfterUpdate` | `CannoHoney.kt:221` (step 5) |

No other new API. The temporary suite and its frozen copy are test code.

## Rewrite rules

The scripts in steps 3 to 8 apply these rules. Each rule keeps the value, including for `null`:

| Old | New |
|---|---|
| `x.spawned` | `x.existenceState.liveNow` |
| `!x.dead` | `x.existenceState.liveAfterUpdate` |
| `x.dead` | `!x.existenceState.liveAfterUpdate` |
| bare `dead` / `!dead` (a member read, for example in `Megaman` and its behaviors) | `!existenceState.liveAfterUpdate` / `existenceState.liveAfterUpdate` |
| `x?.dead == true` | `x?.existenceState?.liveAfterUpdate == false` |
| `x?.spawned == true` | `x?.existenceState?.liveNow == true` |
| `x!!.dead == true` | `!x!!.existenceState.liveAfterUpdate` |
| `(x as MegaGameEntity).dead` | `!x.existenceState.liveAfterUpdate`, if `x` is already an `IGameEntity`; else keep the cast |

`x` is any receiver (a name, `x!!`, `it`, a call). A read that matches no rule is changed by hand and written in the
process log. `Spawner.spawned`, the `MegamanWeaponsHandler.spawned` map and comments are not touched.

## Steps

### 1. Baseline

- Changes: none. A clean build (`./gradlew clean engine:test core:test lwjgl3:jar`); write the test counts and the
  set of unique compiler warnings to scratchpad `062c-warnings-baseline.txt`.
- Check: the build passes; the counts and the warnings are in the process log.
- Done: yes

### 2. The enum table and its tests

- Changes: `EntityExistenceState` gets the constructor values `liveNow` and `liveAfterUpdate` (the table in
  `docs/SCOPE.md`). Engine test sources: the temporary `TempOldExistenceFlags` (the old `spawned` and `dead` formulas,
  copied as they are now) and `TempOldExistenceFlagsTest` (for each state, `liveNow` = old `spawned`,
  `liveAfterUpdate` = old `!dead`); a permanent test of both values for each state in `EntityExistenceStateTest`.
- Check: `./gradlew engine:test` passes; the temporary suite covers all five states.
- Done: yes

### 3. The rewrite and check scripts

- Changes: scratchpad scripts only. `062c-rewrite.py` applies the rewrite rules to a list of files.
  `062c-check-substitutions.py` reads `git diff` for `main` code, undoes the rules on each new line, compares it with
  the old line, and lists every line it cannot match (plus the enum, the removed getters and `toString` as expected
  changes).
- Check: the check script, run on a hand-made diff with one correct and one wrong substitution (a lost `!`), passes
  the first and reports the second.
- Done: yes

### 4. Batch 1: bosses

- Changes: `entities/bosses/` (for example `ReactorMan`, `WilyFinalBoss`, `GutsTank`, `PreciousWoman`, `SigmaRat`).
- Check: `./gradlew core:compileKotlin`; the check script reports no unmatched line (or each one is reviewed and
  logged); `./gradlew core:test` passes.
- Done: yes

### 5. Batch 2: enemies

- Changes: `entities/enemies/` and `entities/contracts/` (for example `CannoHoney`, `AbstractEnemy`,
  `AbstractHealthEntity`).
- Check: as in step 4.
- Done: yes

### 6. Batch 3: special, blocks, hazards, projectiles, sensors and utils

- Changes: `entities/special/`, `blocks/`, `hazards/`, `projectiles/`, `sensors/`, `utils/` (for example
  `CapsuleTeleporter`, `FreezableEntityHandler`, `LaserBeamer`, `PlayerDeath`). The two places that mix the
  questions keep them (`FreezableEntityHandler.kt:59,64`).
- Check: as in step 4.
- Done: yes

### 7. Batch 4: Megaman

- Changes: `entities/megaman/` (`Megaman`, the behaviors, `MegamanJetpackHandler`, `MegamanWeaponsHandler`).
- Check: as in step 4.
- Done: yes

### 8. Batch 5: screens, the game, the engine and the tests

- Changes: `MegaLevelScreen`, `MegamanMaverickGame.kt:931`, `GameEngine.reset()`/`dispose()`, and the tests that read
  the flags (`EntityExistenceStateTest`, `EntityExistenceHooksTest`, `PlayersTest`, `EntityExistenceTest`,
  `SpawnerReleaseTest`, `MegaPlayersTest`).
- Check: `./gradlew engine:test core:test` passes; the check script reports no unmatched line; a grep finds no read of
  the entity `spawned` or `dead` left.
- Done: yes

### 9. Remove the old getters

- Changes: remove `IGameEntity.spawned` and `MegaGameEntity.dead`; `MegaGameEntity.toString` prints
  `existenceState` in place of `dead`. Remove `EntityExistenceState` imports that are no longer used.
- Check: the build compiles (the proof that no reader is left); all tests pass; the check script on the full branch
  diff reports only the expected changes.
- Done: yes

### 10. Contract checks and the owner's play test

- Changes: none.
- Check: all automated checks in `docs/CONTRACT.md` on a clean build (warnings compared with the baseline); then the
  owner plays the manual checks. The results go in the process log.
- Done: yes

### 11. Delete the temporary suite

- Changes: delete `TempOldExistenceFlags` and `TempOldExistenceFlagsTest`, in the last commit before the merge.
- Check: the clean build and all tests pass again.
- Done: yes
