# Process log: mega-refactor-118e-batch-4

## Status

- State: active
- Goal: the mini bosses on the fight flow with `miniBossPlan` (5 live fights)
- Done: plan steps 1 to 6
- Next: owner merge approval, then the merge (plan step 7)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-05

- No branch exists yet. Scan of `master` at `ca1510f0e` (an agent did the TMX reads; I checked the `Gate` key logic, the `MechaDragon` spawn block and `ReactorMonkeyBall`). 5 live fights: `MoonHead` 1, `MechaDragon` 1, `ReactorMonkey` 3. `Bospider` and `PenguinMiniBoss` have none. The owner chose the map `mini` as the one source, accepted the 1-frame earlier controller, and chose to comment out `Bospider`. Draft `SCOPE.md`.
- The owner approved `SCOPE.md`. Draft `CONTRACT.md`, with 3 experimental checks and 1 control check.
- The owner approved `CONTRACT.md`. Draft `PLAN.md`.
- The owner approved `PLAN.md`. Branch start on `master`, and the worktree from `master`.
- Step 1: worktree from `master` at `d438a13e5`. Base: engine 636, core 854, 0 failures. Overlap unchanged (`docs/NOTES.md`).
- Step 2: the new tests ran on the base code, with an empty `EndBossSpawn` so that they compile. Core 858, and only the 6 new or changed tests fail.
- Step 3 (`bf92067fa`): `EndBossSpawn`, `miniBossPlan`, the `mini` read and the 4 bosses in `find`; the forced `mini` lines removed from `MoonHead`, `MechaDragon` and `PenguinMiniBoss`. `engine:test core:test core:smokeTest lwjgl3:jar` passes: engine 636, core 858, 0 failures. Mutation checks, each confirmed in `git diff` and run with the full `core:test`:
  - remove `MoonHead`, `MechaDragon`, `ReactorMonkey` or `PenguinMiniBoss` from `find` (4 runs): 4 tests fail in each run;
  - `mini` always `false`: 2 tests fail;
  - the `mini` read after the `end` read: 1 test fails;
  - no `ControllerOn` in the mini intro: 1 test fails;
  - no `ExitMidBossFight` in the mini outro: 1 test fails;
  - an empty `EndBossSpawn.begin`: 1 test fails.
- Step 4 (`289b1e148`): `Bospider.kt` commented out with a `TODO`, in the form of `SphinxMiniBoss.kt`; its constant and pool line removed from `BossesFactory`. No live reference remains. The full check passes: engine 636, core 858, smoke 436, 0 failures (smoke is 1 lower, because `Bospider` is gone).
- Step 5: `timeout 12` launches of `MOON_MAN room11`, `INFERNO_MAN room8`, `REACTOR_MAN room12`, `REACTOR_MAN room5` and `WILY_STAGE_1 room9` ran with no exception. The only error is the known `DuoBallCanon` spawner error in `WILY_STAGE_1` (`2026-10-22-misplaced-tmx-objects.md`).
- Step 6: the owner played the 4 checks in `CONTRACT.md` (`MoonHead` in `MOON_MAN room11`, `MechaDragon` in `INFERNO_MAN room8`, `ReactorMonkey` in `REACTOR_MAN room12`, control `DesertMan` in `WILY_STAGE_1 room9`). The owner said: "tests passed".
